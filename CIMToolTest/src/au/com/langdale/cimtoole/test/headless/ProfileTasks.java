package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;

public class ProfileTasks extends ProjectTest {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setupSchema();
	}

	public final void testCreateProfile() throws Exception {
		IWorkspaceRunnable action = Task.createProfile(
				profile, 
				PROFILE_NS, 
				PROFILE_ENVELOPE);
		workspace.run(action, monitor);
		assertTrue("profile exists", profile.exists());
	}
	
	public final void testImportProfile() throws CoreException {
		IWorkspaceRunnable action = Task.importProfile(
				profile, 
				getSamplesFolder() + SAMPLE_PROFILE, 
				PROFILE_NS);
		workspace.run(action, monitor);
		assertTrue("profile exists", profile.exists());
	}
	
}
