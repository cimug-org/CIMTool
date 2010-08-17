/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import au.com.langdale.cimtoole.builder.ConsistencyChecks.ProfileChecker;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.CopyBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.LegacyRDFSBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.SimpleOWLBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.registries.ProfileBuildletRegistry;
/**
 * The builder for CIMTool projects.  
 * 
 * This class divides the build into smaller units of work 
 * performed by <code>Buildlets</code>.
 */
public class CIMBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "au.com.langdale.cimtoole.CIMBuilder";

	private static final String MARKER_TYPE = "au.com.langdale.cimtoole.problem";
	
	/**
	 * @return an array of buildlets that together build a CIMTool project
	 */
	public Buildlet[] createBuildlets() {
		Buildlet[] defaultBuildlets = new Buildlet[] {
			new SchemaBuildlet(),
			new ProfileChecker(),
			new XSDBuildlet(),
			new TransformBuildlet(null, "xml"),
			new TransformBuildlet("html", "html"),
			new TextBuildlet("sql", "sql"),
                        new TextBuildlet("jpa", "java"),
                        new TextBuildlet("scala", "scala"),
			new SimpleOWLBuildlet("RDF/XML", "simple-flat-owl", false),
			new SimpleOWLBuildlet("RDF/XML-ABBREV", "simple-owl", false),
			new LegacyRDFSBuildlet("RDF/XML", "legacy-rdfs", false),
			new SimpleOWLBuildlet("RDF/XML", "simple-flat-owl-augmented", true),
			new SimpleOWLBuildlet("RDF/XML-ABBREV", "simple-owl-augmented", true),
			new LegacyRDFSBuildlet("RDF/XML", "legacy-rdfs-augmented", true),
			new CopyBuildlet("TURTLE", "ttl"),
			new ValidationBuildlet(),
			new SplitValidationBuildlet(),
			new IncrementalValidationBuildlet(),

		};
		
		ProfileBuildlet[] registered = ProfileBuildletRegistry.INSTANCE.getBuildlets();
		if (registered.length>0){
			Buildlet[] combined = new Buildlet[defaultBuildlets.length+registered.length];
			System.arraycopy(defaultBuildlets, 0, combined, 0, defaultBuildlets.length);
			System.arraycopy(registered, 0, combined, defaultBuildlets.length, registered.length);
			return combined;
		}else
			return defaultBuildlets;
	}

	/**
	 * Utility to add a problem marker to a file.
	 * @param file: the file to be marked
	 * @param message: the text describing the problem
	 * @param lineNumber: the line to be marked.
	 * @param severity: the severity code (see <code>IMarker</code>).
	 * @throws CoreException
	 */
	public static void addMarker(IFile file, String message, int lineNumber, int severity) throws CoreException {
		IMarker marker = addMarker(file, message, severity);
		if (lineNumber == -1)
			lineNumber = 1;
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
	}
	/**
	 * Utility to add a problem marker to a file.
	 * @param file: the file to be marked
	 * @param message: the text describing the problem
	 * @param severity: the severity code (see <code>IMarker</code>).
	 * @throws CoreException
	 */
	public static IMarker addMarker(IFile file, String message, int severity)
			throws CoreException {
		IMarker marker = file.createMarker(MARKER_TYPE);
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.SEVERITY, severity);
		return marker;
	}
	/**
	 * Create a SAX error handler that converts each error into a marker.
	 * @param result: the file to which the markers will be attached
	 * @return: the SAX error handler
	 * @throws CoreException
	 */
	public static ErrorHandler createErrorHandler(final IFile result) throws CoreException {
		removeMarkers(result);
		return new ErrorHandler() {
			private void log(final IFile result, SAXParseException ex, int severity) throws SAXException {
				try {
					addMarker(result, ex.getMessage(), ex.getLineNumber(), severity);
				} catch (CoreException e) {
					throw new SAXException(e);
				}
			}

			public void warning(SAXParseException ex) throws SAXException {
				log(result, ex, IMarker.SEVERITY_WARNING);
			}

			public void error(SAXParseException ex) throws SAXException {
				log(result, ex, IMarker.SEVERITY_ERROR);
			}

			public void fatalError(SAXParseException ex) throws SAXException {
				log(result, ex, IMarker.SEVERITY_ERROR);
			}
		};
	}
	/**
	 * Utility to add a problem marker with ERROR severity to a file.
	 * @param file: the file to be marked
	 * @param message: the text describing the problem
	 * @throws CoreException
	 */
	public static IMarker addMarker(IFile file, String message) throws CoreException{
		return addMarker(file, message, IMarker.SEVERITY_ERROR);
	}
	/**
	 * Utility to remove all CIMTool markers from a file.
	 * @param file: the file
	 * @throws CoreException
	 */
	public static void removeMarkers(IFile file) throws CoreException {
		if( file.exists())
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
	}
	
	private static class Worker implements IResourceDeltaVisitor, IResourceVisitor, IWorkspaceRunnable {
		private boolean cleanup;
		private boolean rebuild; 
		private Buildlet[] buildlets;
		private Map work = new LinkedHashMap();
		
		public Worker(Buildlet[] buildlets, boolean cleanup) {
			this.buildlets = buildlets;
			this.cleanup = cleanup;
		}
		
		public boolean getRebuild() {
			return rebuild;
		}
		
		public boolean visit(IResourceDelta delta) throws CoreException {
			switch (delta.getKind()) {
				case IResourceDelta.ADDED:
				case IResourceDelta.REMOVED:
					collect(delta.getResource());
					break;

				case IResourceDelta.CHANGED:
					if((delta.getFlags()&(IResourceDelta.CONTENT|IResourceDelta.REPLACED)) != 0)
						collect(delta.getResource());
					break;
			}
			return true;
		}

		public boolean visit(IResource resource) throws CoreException {
			collect(resource);
			return true;
		}

		private void collect(IResource resource) throws CoreException {
			rebuild = rebuild || Info.isSchema(resource);

			for( int ix = 0; ix < buildlets.length; ix++) {
				Buildlet buildlet = buildlets[ix];
				Iterator outputs = buildlet.getOutputs(resource).iterator();
				while( outputs.hasNext()) {
					IFile output = (IFile) outputs.next();
					if(work.remove(output) != null)
						System.out.println("CIMBuilder: push down in build order: " + output.getName()); // push output down in the build order
					else
						System.out.println("CIMBuilder: adding to build: " + output.getName());
					work.put(output, buildlet);
					collect(output); // not efficient since we might encounter an output many times
				}
			}
		}
		
		public void run(IProgressMonitor monitor) throws CoreException {
			Iterator outputs = work.keySet().iterator();
			while(outputs.hasNext()) {
				IFile output = (IFile) outputs.next();
				Buildlet buildlet = (Buildlet) work.get(output);
				System.out.println("CIMBuilder: building: " + output.getName());
				buildlet.run(output, cleanup, monitor);
			}
		}
	}

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		Buildlet[] buildlets = createBuildlets();
		Worker worker = new Worker(buildlets, false);
		IProject project = getProject();
		
		if (kind == FULL_BUILD) {
			project.accept(worker);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				project.accept(worker);
			} else {
				delta.accept(worker);
				if( worker.getRebuild())
					project.accept(worker);
			}
		}
		
		worker.run(monitor);
		return null;
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		Worker worker = new Worker(createBuildlets(), true);
		getProject().accept(worker);
		worker.run(monitor);		
	}
}
