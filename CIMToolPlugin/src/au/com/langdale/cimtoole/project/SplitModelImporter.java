/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.ResourceOutputStream;
import au.com.langdale.cimtoole.builder.CIMBuilder;
import au.com.langdale.kena.RDFParser;
import au.com.langdale.kena.RDFParser.TerminateParseException;
import au.com.langdale.splitmodel.SplitWriter;
import au.com.langdale.util.Logger;
/**
 * A task to import an RDF/XML instance into the project as <code>SplitModel</code>.
 */
public class SplitModelImporter implements IWorkspaceRunnable {
	private IFolder destin;
	private String source;
	private String namespace;
	private IFile profile;
	private IFolder base;
	
	public SplitModelImporter(IFolder destin, String source, String namespace, IFile profile, IFolder base) {
		this.destin = destin;
		this.source = source;
		this.namespace = namespace;
		this.profile = profile;
		this.base = base;
	}

	public void run(IProgressMonitor monitor) throws CoreException {

		IFile errors = Info.getRelated(destin, "xml-log");

		if( destin.exists()) 
			destin.delete(false, monitor);
		if( errors.exists()) 
			errors.delete(false, monitor);
		
		Logger logger = new Logger(new ResourceOutputStream(errors, null, true, true));
		SplitWriter writer = new SplitWriter(destin.getLocation().toOSString(), namespace);
		RDFParser splitter = new RDFParser(null, source, writer.getBase(), writer, logger, base!=null);
		try {
			splitter.run();
			logger.close();
		}
		catch( TerminateParseException e) {
			throw Info.error(e.getMessage(), e);
		} catch (IOException e) {
			throw Info.error("could not write error report", e);
		}
		if( logger.getErrorCount() > 0) {
			CIMBuilder.addMarker(errors, "errors parsing model " + destin.getProjectRelativePath().lastSegment());
		}
		
		destin.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		Info.putProperty( destin, Info.INSTANCE_NAMESPACE, namespace);

		if( profile != null)
			Info.putProperty( destin, Info.PROFILE_PATH, profile.getName());
		if( base != null)
			Info.putProperty( destin, Info.BASE_MODEL_PATH, base.getName());
	}
}
