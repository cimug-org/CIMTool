package au.com.langdale.cimtoole.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class WorkspaceTest extends TestUtility {

	protected IProject project;
	protected IProgressMonitor monitor;
	protected IWorkspace workspace;

	@Override
	protected void setUp() throws Exception {
		workspace = ResourcesPlugin.getWorkspace();
		monitor = new NullProgressMonitor();
		project = workspace.getRoot().getProject("TestProject");
		if( project.exists())
			project.delete(true, monitor);
	}

	@Override
	protected void tearDown() throws Exception {
//		if( project.exists())
//			project.delete(true, monitor);
	}

}
