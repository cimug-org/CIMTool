/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.BuilderPreferences;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

/**
 * The order in which a builder preference resolves in the workbench (issue
 * #411): a file's own value, then its project's value, then the workspace
 * value. A project resolves to its own value, then the workspace value.
 */
public class BuilderPreferenceResolutionTests extends WorkspaceTest {

	private static final String SAVE_JOB = "Saving CIMTool builder preferences";
	private static final long TIMEOUT_MILLIS = 10000;

	private static final String WORKSPACE_HORIZONTAL = "42";
	private static final String WORKSPACE_VERTICAL = "24";

	private IPreferenceStore store;
	private BuilderPreferences preferences;
	private IFile diagram;

	private final AtomicInteger saveJobsScheduled = new AtomicInteger();
	private final AtomicInteger saveJobsDone = new AtomicInteger();
	private final IJobChangeListener saveJobCounter = new JobChangeAdapter() {
		@Override
		public void scheduled(IJobChangeEvent event) {
			if (SAVE_JOB.equals(event.getJob().getName()))
				saveJobsScheduled.incrementAndGet();
		}

		@Override
		public void done(IJobChangeEvent event) {
			if (SAVE_JOB.equals(event.getJob().getName()))
				saveJobsDone.incrementAndGet();
		}
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Job.getJobManager().addJobChangeListener(saveJobCounter);
		workspace.run(Task.createProject(project, null), monitor);
		preferences = CIMToolPlugin.getBuilderPreferences();
		diagram = project.getFile("Profiles/Test.rdfs-t2b.puml");
		store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setValue(Info.HORIZONTAL_SPACING.getLocalName(), WORKSPACE_HORIZONTAL);
		store.setValue(Info.VERTICAL_SPACING.getLocalName(), WORKSPACE_VERTICAL);
	}

	@Override
	protected void tearDown() throws Exception {
		awaitSaves();
		Job.getJobManager().removeJobChangeListener(saveJobCounter);
		store.setToDefault(Info.HORIZONTAL_SPACING.getLocalName());
		store.setToDefault(Info.VERTICAL_SPACING.getLocalName());
		super.tearDown();
	}

	public final void testFileOwnValueWins() throws Exception {
		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "13");
		preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "7");

		assertEquals("7", Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testFileInheritsProjectValue() throws Exception {
		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "13");

		assertEquals("13", Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testFileInheritsWorkspaceValueWhenProjectHasNone() throws Exception {
		assertEquals(WORKSPACE_HORIZONTAL, Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testProjectResolvesOwnValueThenWorkspace() throws Exception {
		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "13");

		assertEquals("project's own value", "13", Info.getBuilderPreference(project, Info.HORIZONTAL_SPACING));
		assertEquals("workspace value", WORKSPACE_VERTICAL,
				Info.getBuilderPreference(project, Info.VERTICAL_SPACING));
	}

	public final void testInheritedValueIgnoresOwnValue() throws Exception {
		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "13");
		preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "7");

		assertEquals("file inherits the project's value", "13",
				Info.getInheritedBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
		assertEquals("project inherits the workspace value", WORKSPACE_HORIZONTAL,
				Info.getInheritedBuilderPreference(project, Info.HORIZONTAL_SPACING));
	}

	public final void testBuilderParametersUseProjectValue() throws Exception {
		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "13");

		String parameters = Info.getBuilderParameters(diagram);

		assertTrue("project value passed to the builders: " + parameters,
				parameters.contains("horizontalSpacing=13|"));
		assertTrue("workspace value passed to the builders: " + parameters,
				parameters.contains("verticalSpacing=" + WORKSPACE_VERTICAL + "|"));
	}

	/**
	 * Waits until every save job scheduled during the test has finished, so that
	 * none is still writing when the next test deletes and recreates the project.
	 */
	private void awaitSaves() throws InterruptedException {
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (saveJobsDone.get() < saveJobsScheduled.get()) {
			if (System.currentTimeMillis() > deadline)
				fail("timed out waiting for builder preferences to be saved");
			Thread.sleep(50);
		}
	}
}
