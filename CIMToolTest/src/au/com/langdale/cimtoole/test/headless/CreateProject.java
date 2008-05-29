package au.com.langdale.cimtoole.test.headless;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

public class CreateProject extends WorkspaceTest {
	public final void testCreate() throws Exception {
		workspace.run(Task.createProject(project, null), monitor);
		assertTrue("project resource created", project.exists());
		assertTrue("project folders created", project.getFolder("Schema").exists());
		assertTrue("project folders created", project.getFolder("Profiles").exists());
		assertTrue("project folders created", project.getFolder("Instances").exists());
		assertTrue("project folders created", project.getFolder("Incremental").exists());
	}

}
