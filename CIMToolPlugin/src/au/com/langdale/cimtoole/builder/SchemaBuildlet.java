/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.Syntax;
/**
 * A Buildlet to serialize the merged schema for a project as an RDF/XML document. 
 */
public class SchemaBuildlet extends Buildlet {

	@Override
	protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
		IProject project = result.getProject();
		IFolder folder = Info.getSchemaFolder(project);
		OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(folder);
		Task.writeOntology(result, schema, Syntax.RDF_XML.toFormat(), monitor);
	}
	
	@Override
	protected Collection getOutputs(IResource file) throws CoreException {
		if( Info.isSchema(file)) {
			IProject project = file.getProject();
			String name = Info.getProperty(project, Info.MERGED_SCHEMA_PATH);
			if( name != null && name.length() != 0)
				return Collections.singletonList(project.getFile(name)); 
		}
		return Collections.EMPTY_LIST;
	}

}
