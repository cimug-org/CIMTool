/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cim.CIM;
import au.com.langdale.cimtoole.CIMNature;
import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.ResourceOutputStream;
import au.com.langdale.cimtoole.builder.CIMBuilder;
import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.validation.ValidatorUtil;
import au.com.langdale.xmi.CIMInterpreter;
import au.com.langdale.xmi.UML;

/**
 * Utility tasks for CIMTool plugin.  Tasks are instances of <code>IWorkspaceRunnable</code>
 * and in some cases, plain methods.
 */
public class Task extends Info {

	public static class EmptyStream extends InputStream {
		@Override
		public int read() throws IOException {
			return -1;
		}
	}

	public static IWorkspaceRunnable chain(final IWorkspaceRunnable first, final IWorkspaceRunnable second) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				first.run(monitor);
				second.run(monitor);
			}
		};
		
	}

	public static IWorkspaceRunnable createProfile(final IFile file, final String namespace, final String envname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				OntModel model = ModelFactory.createMem();
				model.createClass(MESSAGE.Message.getURI());
				monitor.worked(1);
				write(model, namespace, false, file, "RDF/XML-ABBREV", monitor);
				file.setPersistentProperty(PROFILE_NAMESPACE, namespace);
				file.setPersistentProperty(PROFILE_ENVELOPE, envname);
				monitor.worked(1);
			}
		};
	}

	public static IWorkspaceRunnable createProject(final IProject project, final URI location) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
				description.setLocationURI(location);
				description.setNatureIds(new String[] {CIMNature.NATURE_ID});
				ICommand command = description.newCommand();
				command.setBuilderName(CIMBuilder.BUILDER_ID);
				description.setBuildSpec(new ICommand[] {command});
				project.create(description, monitor);
				project.open(monitor);
				monitor.worked(1);
				getSchemaFolder(project).create(false, true, monitor);
				getProfileFolder(project).create(false, true, monitor);
				getInstanceFolder(project).create(false, true, monitor);
				getIncrementalFolder(project).create(false, true, monitor);
				monitor.worked(1);
			}
		};
	}

	public static IWorkspaceRunnable createRules(final IFile file, final String template) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				InputStream source;
				if(template != null)
					source = getClass().getResourceAsStream("/au/com/langdale/profiles/" + template + ".xsl");
				else
					source = new EmptyStream();
				monitor.worked(1);

				writeFile(file, source, monitor);
			}
			
		};
	}

	public static IWorkspaceRunnable createValidationRules(final IFile file, final String template) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				InputStream source;
				if(template != null)
					source = ValidatorUtil.openStandardRules(template);
				else
					source = new EmptyStream();
				monitor.worked(1);

				writeFile(file, source, monitor);
			}
			
		};
	}
	
	public static IWorkspaceRunnable importSchema(final IFile file, final String pathname, final String namespace ) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, pathname, monitor);
				file.setPersistentProperty(SCHEMA_NAMESPACE, namespace);
			}
		};
	}
	
	public static IWorkspaceRunnable importProfile(final IFile file, final String pathname, final String namespace, final String envname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, pathname, monitor);
				file.setPersistentProperty(PROFILE_NAMESPACE, namespace);
				file.setPersistentProperty(PROFILE_ENVELOPE, envname);
			}
		};
	}
	
	public static IWorkspaceRunnable importRules(final IFile file, final String pathname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, pathname, monitor);
			}
		};
	}
	
	public static IWorkspaceRunnable importModel(final IFile file, final String pathname, final String namespace, final IFile profile) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, pathname, monitor);
				file.setPersistentProperty(PROFILE_PATH, profile.getName());
				file.setPersistentProperty(INSTANCE_NAMESPACE, namespace);
			}
		};
	}
	
	public static OntModel parse(IFile file) throws CoreException {
		return parse(file, new BufferedInputStream( file.getContents()));
	}

	private static OntModel parse(IFile file, InputStream contents)	throws CoreException {
		String ext = file.getFileExtension().toLowerCase();
		if( ext.equals("xmi")) {
			return parseXMI(file, contents);
		}
		else {
			return parseOWL(file, contents);
		}
	}

	public static TreeModelBase createTreeModel(IResource resource) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		if(resource instanceof IProject)
			return createUMLTreeModel(cache.getMergedOntologyWait(getSchemaFolder((IProject)resource)));
		else if( isSchemaFolder(resource))
			return createUMLTreeModel(cache.getMergedOntologyWait((IFolder)resource));
		else if( isSchema(resource))
			return createUMLTreeModel(cache.getOntologyWait((IFile)resource));
		else if(isProfile(resource)) {
			OntModel model = cache.getOntologyWait((IFile)resource);
			OntModel backgroundModel = cache.getMergedOntologyWait(getSchemaFolder(resource.getProject()));
			return createProfileTreeModel(model, backgroundModel);
		} else 
			return new UMLTreeModel();
	}
	
	private static TreeModelBase createUMLTreeModel(OntModel model) {
		UMLTreeModel tree = new UMLTreeModel();
		tree.setOntModel(model);
		tree.setRootResource(UML.global_package);
		return tree;
	}

	private static TreeModelBase createProfileTreeModel(OntModel model, OntModel backgroundModel) {
		ProfileModel tree = new ProfileModel();
		tree.setOntModel(model);
		tree.setBackgroundModel(backgroundModel);
		tree.setRootResource(MESSAGE.Message);
		return tree;
	}

	private static OntModel parseXMI(IFile file, InputStream contents) throws CoreException {
		String base = getProperty(SCHEMA_NAMESPACE, file);
		if( base == null ) {
			if( file.getName().toLowerCase().startsWith("cim"))
				base = CIM.NS;
			else
				base = file.getLocationURI().toString() + "#";
		}
		try {
			IFile auxfile = getRelated(file, "annotation");
			OntModel annote;
			if(auxfile.exists()) {
				annote = ModelFactory.createMem();
				IO.read(annote, auxfile.getContents(), base, "TURTLE");
			}
			else
				annote = null;
			return CIMInterpreter.parse(contents, base, annote, getPreferenceOption(USE_PACKAGE_NAMES));
		} catch (Exception e) {
			throw error("Can't parse model file " + file.getName(), e);
		}
	}

	private static OntModel parseOWL(IFile file, InputStream contents) throws CoreException {
		OntModel model;
		String extn = file.getFileExtension().toLowerCase();
		String base = getProperty(PROFILE_NAMESPACE, file);
		if( base == null ) 
			base = file.getLocationURI().toString() + "#";
		try {
			model = parse(contents, extn, base);
		}
		catch( Exception ex) {
			throw error("Can't parse model file " + file.getName(), ex);
		}
		return model;
	}

	public static OntModel parse(InputStream contents, String ext, String base) {
		OntModel model = ModelFactory.createMem();
		String syntax;
		if(ext.equals("n3"))
			syntax = "N3";
		else if(ext.equals("diagnostic"))
			syntax = "TURTLE";
		else
			syntax = "RDF/XML";
		IO.read(model, contents, base, syntax);
		return model;
	}

	public static void write(OntModel model, String namespace, boolean xmlbase, IFile file, String format, IProgressMonitor monitor) throws CoreException {
		OutputStream stream = new ResourceOutputStream(file, monitor, false, false);
		write(model, namespace, xmlbase, format, stream);
	}

	public static void write(OntModel model, String namespace, boolean xmlbase, String format, OutputStream stream)
			throws CoreException {
		try {
			HashMap style = new HashMap();
			if(format != null && format.equals("RDF/XML-ABBREV"))
				style.put("prettyTypes", PrettyTypes.PRETTY_TYPES);
			if( namespace != null) {
				if(namespace.endsWith("#"))
					namespace = namespace.substring(0, namespace.length()-1);
				if(xmlbase)
					style.put("xmlbase", namespace);
				style.put("relativeURIs", "same-document");
			}
			style.put("showXmlDeclaration", "true");
			IO.write(model, stream, namespace, format, style);
			stream.close();
		} catch (IOException e) {
			throw error("can't write model to stream", e);
		}
	}

	public static IWorkspaceRunnable exportSchema(final IProject project,	final String pathname, final String namespace) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFolder folder = Info.getSchemaFolder(project);
				OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(folder);
				OutputStream output; 
				try {
					output = new BufferedOutputStream( new FileOutputStream(pathname));
				}
				catch( IOException ex) {
					throw error("can't write to " + pathname);
				}
				write(schema, namespace, true, "RDF/XML", output);
			}
		};
	}
	
	public static IWorkspaceRunnable saveProfile(final IFile file, final OntModel model, final String namespace) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				write(model, namespace, false, file, "RDF/XML-ABBREV", monitor);
			}
		};
	}
	
	public static IWorkspaceRunnable delete(final IResource resource) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				resource.delete(false, monitor);
				monitor.worked(1);
			}
		};
	}
	
	public static IWorkspaceRunnable delete(final IResource[] resources) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				for (int ix = 0; ix < resources.length; ix++) {
					resources[ix].delete(false, monitor);
					monitor.worked(1);
				}
			}
		};
	}

	private static void importFile(final IFile file, final String pathname, IProgressMonitor monitor) throws CoreException {
		InputStream source;
		try {
			source = new BufferedInputStream( new FileInputStream(pathname));
		} catch (FileNotFoundException e) {
			throw error("can't open " + pathname, e);
		}
		monitor.worked(1);
		writeFile(file, source, monitor);
		monitor.worked(1);
	}

	private static void writeFile(final IFile file, InputStream source,	IProgressMonitor monitor) throws CoreException {
		if( file.exists())
			file.setContents(source, false, true, monitor);
		else
			file.create(source, false, monitor);
	}

}
