package au.com.langdale.cimtoole.project;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.kena.OntModel;
import au.com.langdale.util.NSMapper;
import au.com.langdale.util.Jobs;

public class NSChecker implements IWorkspaceRunnable {
	private IProject lastProject, project;
	private Set namespaces = Collections.EMPTY_SET;
	
	void setProject(IProject project) {
		this.project = project;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(Info.getSchemaFolder(project));
		namespaces = NSMapper.extractNamespaces(schema);
		lastProject = project;
	}

	public String validate(IProject project, String namespace, IRunnableContext container, Shell shell) {
		this.project = project;
		if( ! project.equals(lastProject)) 
			Jobs.runInteractive(this, null, container, shell);
		return validate(namespace);
	}

	protected String validate(String namespace) {
		if( namespaces.contains(namespace))
			return "The given namespace conflicts with a schema namespace.";
		else
			return null;
	}
}
