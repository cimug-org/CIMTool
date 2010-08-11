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
import java.util.Iterator;
import java.util.Set;

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
import au.com.langdale.cimtoole.builder.CIMBuilder;
import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.validation.RepairMan;
import au.com.langdale.validation.ValidatorUtil;
import au.com.langdale.workspace.ResourceOutputStream;
import au.com.langdale.xmi.CIMInterpreter;
import au.com.langdale.xmi.EAPExtractor;
import au.com.langdale.xmi.UML;
import au.com.langdale.xmi.XMIParser;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;

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
				OntModel backgroundModel = getBackgroundModel(file);
				initProfile(model, backgroundModel, namespace, envname, null);
				monitor.worked(1);
				writeProfile(file, model, monitor);
				putProperty( file, PROFILE_NAMESPACE, namespace);
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
				saveSettings(project, ModelFactory.createMem()).run(monitor);
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
				putProperty( file, SCHEMA_NAMESPACE, namespace);
			}
		};
	}
	
	public static IWorkspaceRunnable importProfile(final IFile file, final String pathname) {
		return new IWorkspaceRunnable() { 
			public void run(IProgressMonitor monitor) throws CoreException {
				String defaultName = getPreference(PROFILE_ENVELOPE);
				String namespace = getPreference(PROFILE_NAMESPACE);
				OntModel model = parse(openExternalFile(pathname, monitor), "owl", namespace);
				model = fixupProfile(model, getBackgroundModel(file), defaultName, namespace);
				writeProfile(file, model, monitor);
				putProperty( file, PROFILE_NAMESPACE, namespace);
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
				putProperty( file, PROFILE_PATH, profile.getName());
				putProperty( file, INSTANCE_NAMESPACE, namespace);
			}
		};
	}
	
	public static OntModel parse(IFile file) throws CoreException {
		String ext = file.getFileExtension().toLowerCase();
		if( ext.equals("xmi")) {
			return parseXMI(file);
		}
		else if( ext.equals("eap"))
			return parseEAP(file);
		else {
			return parseOWL(file);
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
			IFile file = (IFile) resource;
			return getMessageModel(file);
		} else 
			return new UMLTreeModel();
	}
	
	private static TreeModelBase createUMLTreeModel(OntModel model) {
		UMLTreeModel tree = new UMLTreeModel();
		tree.setOntModel(model);
		tree.setRootResource(UML.global_package);
		return tree;
	}
	
	public static ProfileModel getMessageModel(IFile file) throws CoreException {
		ProfileModel model = new ProfileModel();
		model.setOntModel(getProfileModel(file));
		model.setBackgroundModel(getBackgroundModel(file));
		return model;
	}

	private static OntModel parseXMI(IFile file) throws CoreException {
		XMIParser parser = new XMIParser();
		try {
			parser.parse(new BufferedInputStream( file.getContents()));
		} catch (Exception e) {
			throw error("Can't parse model file " + file.getName(), e);
		}
		return interpretSchema(parser.getModel(), file);
	}
	
	private static OntModel parseEAP(IFile file) throws CoreException {
		EAPExtractor extractor;
		try {
			extractor = new EAPExtractor(file.getLocation().toFile());
			extractor.run();
		} catch (IOException e) {
			throw error("can't access EA project", e);
		}
		return interpretSchema(extractor.getModel(), file);
	}

	private static OntModel interpretSchema(OntModel raw, IFile file) throws CoreException {
		String base = getProperty(file, SCHEMA_NAMESPACE);
		if( base == null ) {
			if( file.getName().toLowerCase().startsWith("cim"))
				base = CIM.NS;
			else
				base = file.getLocationURI().toString() + "#";
		}
		IFile auxfile = getRelated(file, "annotation");
		OntModel annote;
		if(auxfile.exists()) {
			annote = ModelFactory.createMem();
			IO.read(annote, auxfile.getContents(), base, "TURTLE");
		}
		else
			annote = null;
		return CIMInterpreter.interpret(raw, base, annote, getPreferenceOption(USE_PACKAGE_NAMES));
	}

	private static OntModel parseOWL(IFile file) throws CoreException {
		OntModel model;
		String extn = file.getFileExtension();
		
		if( extn == null )
			extn = "";
		else
			extn = extn.toLowerCase();
		
		String base;
		if( isProfile(file))
			base = getProperty(file, PROFILE_NAMESPACE);
		else if( extn.equals(SETTINGS_EXTENSION))
			base = CIMToolPlugin.PROJECT_NS;
		else
			base = file.getLocationURI().toString() + "#";
		try {
			model = parse(new BufferedInputStream( file.getContents()), extn, base);
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
		else if(ext.equals("diagnostic") || ext.equals("cimtool-settings"))
			syntax = "TURTLE";
		else if(ext.equals("owl") || ext.equals("repair"))
			syntax = IO.RDF_XML_WITH_NODEIDS;
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

	public static IWorkspaceRunnable exportSchema(final IProject project,	final String pathname) {
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
				writeOntology(output, schema, "RDF/XML", monitor);
			}
		};
	}
	
	public static IWorkspaceRunnable repairProfile(final IFile file, final RepairMan repairs) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				if( repairs.size() > 0 ) {
					IFile related = Info.getRelated(file, "owl");
					OntModel model = Task.getProfileModel(related);
					model = repairs.apply(model);
					writeProfile(related, model, monitor);
				}
			}
		};
	}
	
	public static IWorkspaceRunnable saveProfile(final IFile file, final OntModel model) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				writeProfile(file, model, monitor);
			}
		};
	}
	
	public static IWorkspaceRunnable saveSettings(final IProject project, final OntModel model) {
		model.setNsPrefix("tool", CIMToolPlugin.SETTING_NS);
		model.setNsPrefix("project", CIMToolPlugin.PROJECT_NS);
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				write(model, CIMToolPlugin.PROJECT_NS, true, getSettings(project), "TURTLE", monitor);
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
		InputStream source = openExternalFile(pathname, monitor);
		writeFile(file, source, monitor);
		monitor.worked(1);
	}

	private static InputStream openExternalFile(final String pathname, IProgressMonitor monitor) throws CoreException {
		InputStream source;
		try {
			source = new BufferedInputStream( new FileInputStream(pathname));
		} catch (FileNotFoundException e) {
			throw error("can't open " + pathname, e);
		}
		monitor.worked(1);
		return source;
	}

	private static void writeFile(final IFile file, InputStream source,	IProgressMonitor monitor) throws CoreException {
		if( file.exists())
			file.setContents(source, false, true, monitor);
		else
			file.create(source, false, monitor);
	}

	public static void writeProfile(IFile file, OntModel model, IProgressMonitor monitor) throws CoreException {
		writeOntology(file, model, IO.RDF_XML_WITH_NODEIDS, monitor);
	}
	
	public static void writeOntology(IFile file, OntModel model, String format, IProgressMonitor monitor) throws CoreException {
		OutputStream stream = new ResourceOutputStream(file, monitor, false, false);
		writeOntology(stream, model, format, monitor);
	}
	
	public static void writeOntology(OutputStream stream, OntModel model, String format, IProgressMonitor monitor) throws CoreException {
		OntResource ont = model.getValidOntology();
		String namespace;
		if( ont != null ) {
			namespace = ont.getURI() + "#";
			model.setNsPrefix("", namespace);
		}
		else
			namespace = null;
		write(model, namespace, true, format, stream);
		
	}

	public static OntModel getBackgroundModel(IFile file) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		IFolder schema = getSchemaFolder(file.getProject());
		return cache.getMergedOntologyWait(schema);
	}

	public static OntModel getProfileModel(IFile file) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		return fixupProfile(file, cache.getOntologyWait(file), getBackgroundModel(file));
	}
	
	public static OntModel fixupProfile(IFile file, OntModel model, OntModel backgroundModel) {
		
		String defaultName, namespace;
		try {
			defaultName = getProperty(file, Info.PROFILE_ENVELOPE);
			namespace = getProperty(file, PROFILE_NAMESPACE);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		
		return fixupProfile(model, backgroundModel, defaultName, namespace);
	}
	
	private static OntModel fixupProfile(OntModel model, OntModel backgroundModel, String defaultName, String defaultNamespace) {
		OntResource header = model.getValidOntology();
		if( header != null ) 
			return model;
		
		model = Composition.copy(model);
		
		String namespace = defaultNamespace;
		String label = defaultName;
		String comment = null;
		
		// harvest and remove old style, mal-formed or repeated headers
                Iterator it = model.listSubjectsWithProperty(RDF.type, OWL2.Ontology).toSet().iterator();
                while(it.hasNext()) {
                    OntResource ont = (OntResource) it.next();
                    String candLabel = ont.getLabel();
                    if( candLabel != null && ! candLabel.equals(defaultName))
                        label = candLabel;
                    String candComment = ont.getComment();
                    if( candComment != null && candComment.length() > 0)
                        comment = candComment;
                    
                    if( ont.isURIResource()) { 
                        String uri = ont.getURI();
                        if(! uri.contains("#") || uri.endsWith("#"))
                          namespace = uri;
                    }
                    ont.remove();
                }
		
		// remove any untyped, old style header
		MESSAGE.profile.inModel(model).remove();
		
		initProfile(model, backgroundModel, namespace, label, comment);
		return model;
	}

	public static void initProfile(OntModel profileModel, OntModel backgroundModel, String namespace, String envname, String comment) {
		// add standard ontology header
		if( ! namespace.endsWith("#"))
			namespace += "#";
		String uri = namespace.substring(0, namespace.length()-1);
		OntResource header = profileModel.createResource(uri);
		header.addRDFType(OWL.Ontology);
		header.addLabel(envname, null);
		if( comment != null)
			header.addComment(comment, null);
		profileModel.setNsPrefix("", namespace);
		
		
		// add the import to the CIM
		if( backgroundModel != null ) {
			OntResource backOnt = backgroundModel.getValidOntology();
			if( backOnt != null ) {
				header.addProperty(OWL.imports, backOnt);
				profileModel.setNsPrefix("cim", backOnt.getURI() + "#");
			}
		}
	}
}
