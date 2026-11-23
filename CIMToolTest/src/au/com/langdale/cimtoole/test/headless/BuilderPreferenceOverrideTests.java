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
 * A project or file stores a builder preference only as a genuine override.
 * A value equal to the one the resource inherits is not stored,
 * and choosing it removes any override already stored, so the resource keeps
 * inheriting and later changes at a higher level reach it. Restore Defaults on a
 * property page sets every control to the inherited value, so the following
 * Apply removes the page's overrides through the same path.
 */
public class BuilderPreferenceOverrideTests extends WorkspaceTest {

	private static final String SAVE_JOB = "Saving CIMTool builder preferences";
	private static final long TIMEOUT_MILLIS = 10000;

	private static final String WORKSPACE_HORIZONTAL = "42";

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
		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "13");
	}

	@Override
	protected void tearDown() throws Exception {
		awaitSaves();
		Job.getJobManager().removeJobChangeListener(saveJobCounter);
		store.setToDefault(Info.HORIZONTAL_SPACING.getLocalName());
		super.tearDown();
	}

	public final void testDifferentValueIsStored() throws Exception {
		Info.setBuilderOverride(diagram, Info.HORIZONTAL_SPACING, "7");

		assertEquals("7", preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testInheritedValueIsNotStored() throws Exception {
		Info.setBuilderOverride(diagram, Info.HORIZONTAL_SPACING, "13");

		assertNull("value equal to the project's is not stored",
				preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testInheritedValueRemovesExistingOverride() throws Exception {
		preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "7");

		Info.setBuilderOverride(diagram, Info.HORIZONTAL_SPACING, "13");

		assertNull("override removed", preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
		assertEquals("file inherits the project's value", "13",
				Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testEmptyValueRemovesOverride() throws Exception {
		preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "7");

		Info.setBuilderOverride(diagram, Info.HORIZONTAL_SPACING, "");

		assertNull("override removed", preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
		assertEquals("file inherits the project's value", "13",
				Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testColorComparisonIgnoresCase() throws Exception {
		preferences.putPreference(project, Info.REFS_COLOR, "#ABCDEF");

		Info.setBuilderOverride(diagram, Info.REFS_COLOR, "#abcdef");

		assertNull("same color in other case is not an override",
				preferences.getPreference(diagram, Info.REFS_COLOR));
	}

	public final void testProjectValueEqualToWorkspaceIsNotStored() throws Exception {
		Info.setBuilderOverride(project, Info.HORIZONTAL_SPACING, WORKSPACE_HORIZONTAL);

		assertNull("project override removed", preferences.getPreference(project, Info.HORIZONTAL_SPACING));
		assertEquals("project inherits the workspace value", WORKSPACE_HORIZONTAL,
				Info.getBuilderPreference(project, Info.HORIZONTAL_SPACING));
	}

	/**
	 * The defect this fixes: applying a file's page used to store every value
	 * shown, so the file stopped following its project. Applying the inherited
	 * values must leave the file following later project changes.
	 */
	public final void testApplyingInheritedValuesKeepsFileInheriting() throws Exception {
		preferences.runBatch(project, () -> Info.setBuilderOverride(diagram, Info.HORIZONTAL_SPACING,
				Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING)));

		preferences.putPreference(project, Info.HORIZONTAL_SPACING, "19");

		assertEquals("later project change reaches the file", "19",
				Info.getBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	/**
	 * Restore Defaults followed by Apply: each control is set to the inherited
	 * value, and applying it removes the file's overrides in one save.
	 */
	public final void testRestoreDefaultsThenApplyRemovesOverrides() throws Exception {
		preferences.runBatch(project, () -> {
			preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "7");
			preferences.putPreference(diagram, Info.HIDE_ENUMERATIONS, "false");
		});
		awaitSaves();
		saveJobsScheduled.set(0);
		saveJobsDone.set(0);

		preferences.runBatch(project, () -> {
			Info.setBuilderOverride(diagram, Info.HORIZONTAL_SPACING,
					Info.getInheritedBuilderPreference(diagram, Info.HORIZONTAL_SPACING));
			Info.setBuilderOverride(diagram, Info.HIDE_ENUMERATIONS,
					Info.getInheritedBuilderPreference(diagram, Info.HIDE_ENUMERATIONS));
		});
		awaitSaves();

		assertNull("spacing override removed", preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
		assertNull("hide enumerations override removed",
				preferences.getPreference(diagram, Info.HIDE_ENUMERATIONS));
		assertEquals("one save for the removals", 1, saveJobsScheduled.get());
	}

	public final void testRemovingAbsentValueDoesNotSave() throws Exception {
		awaitSaves();
		saveJobsScheduled.set(0);
		saveJobsDone.set(0);

		preferences.removePreference(diagram, Info.HORIZONTAL_SPACING);
		awaitSaves();

		assertEquals("nothing to remove, nothing saved", 0, saveJobsScheduled.get());
	}

	/**
	 * Waits until every save job scheduled so far has finished.
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
