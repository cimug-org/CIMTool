/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.BuilderPreferences;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

/**
 * Tests for {@link BuilderPreferences#runBatch(org.eclipse.core.resources.IProject, Runnable)}.
 * Every change made to a project's builder preferences inside a
 * batch is applied to one working copy of the store, which is saved to the
 * project's {@code .builder-preferences} file once, and only if something
 * changed. Reads made during the batch see the working copy.
 */
public class BuilderPreferencesBatchTests extends WorkspaceTest {

	private static final String SAVE_JOB = "Saving CIMTool builder preferences";
	private static final long TIMEOUT_MILLIS = 10000;

	private BuilderPreferences preferences;
	private IResource diagram;
	private IFile preferencesFile;

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
		workspace.run(Task.createProject(project, null), monitor);
		preferences = CIMToolPlugin.getBuilderPreferences();
		diagram = project.getFile("Profiles/Test.rdfs-t2b.puml");
		preferencesFile = Info.getBuilderPreferences(project);

		// Load the store before counting starts. A new project's store is created
		// by a one-off migration that writes the file in a job of its own.
		preferences.getPreference(diagram, Info.HORIZONTAL_SPACING);
		Job.getJobManager().addJobChangeListener(saveJobCounter);
		awaitSaves();
		saveJobsScheduled.set(0);
		saveJobsDone.set(0);
	}

	@Override
	protected void tearDown() throws Exception {
		Job.getJobManager().removeJobChangeListener(saveJobCounter);
		super.tearDown();
	}

	public final void testBatchSavesOnce() throws Exception {
		preferences.runBatch(project, () -> {
			preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "11");
			preferences.putPreference(diagram, Info.VERTICAL_SPACING, "22");
			preferences.putPreference(diagram, Info.PLANTUML_THEME, "cerulean");
			preferences.putPreference(diagram, Info.REFS_COLOR, "#123456");
			preferences.putPreference(diagram, Info.HIDE_ENUMERATIONS, "true");
		});
		awaitSaves();

		assertEquals("one save for the whole batch", 1, saveJobsScheduled.get());
		String content = read();
		assertTrue("horizontal spacing saved", content.contains("\"11\""));
		assertTrue("vertical spacing saved", content.contains("\"22\""));
		assertTrue("theme saved", content.contains("\"cerulean\""));
		assertTrue("refs color saved", content.contains("\"#123456\""));
		assertTrue("hide enumerations saved", content.contains("\"true\""));
	}

	public final void testReadsSeeWorkingCopyDuringBatch() throws Exception {
		String[] seen = new String[1];
		preferences.runBatch(project, () -> {
			preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "33");
			seen[0] = preferences.getPreference(diagram, Info.HORIZONTAL_SPACING);
		});
		awaitSaves();

		assertEquals("value visible inside the batch", "33", seen[0]);
		assertEquals("value kept after the batch", "33", preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testUnchangedBatchDoesNotSave() throws Exception {
		preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "44");
		awaitSaves();
		saveJobsScheduled.set(0);
		saveJobsDone.set(0);

		preferences.runBatch(project, () -> preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "44"));
		awaitSaves();

		assertEquals("no save when nothing changed", 0, saveJobsScheduled.get());
	}

	public final void testFailedBatchSavesNothing() throws Exception {
		try {
			preferences.runBatch(project, () -> {
				preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "55");
				throw new IllegalStateException("update failed");
			});
			fail("the exception should propagate");
		} catch (IllegalStateException expected) {
			// expected
		}
		awaitSaves();

		assertEquals("no save after a failed batch", 0, saveJobsScheduled.get());
		assertNull("store unchanged after a failed batch",
				preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
	}

	public final void testNestedBatchJoinsOuterBatch() throws Exception {
		preferences.runBatch(project, () -> {
			preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "66");
			preferences.runBatch(project,
					() -> preferences.putPreference(diagram, Info.VERTICAL_SPACING, "77"));
		});
		awaitSaves();

		assertEquals("one save for the outer and nested batch", 1, saveJobsScheduled.get());
		assertEquals("66", preferences.getPreference(diagram, Info.HORIZONTAL_SPACING));
		assertEquals("77", preferences.getPreference(diagram, Info.VERTICAL_SPACING));
	}

	public final void testPutOutsideBatchSavesImmediately() throws Exception {
		preferences.putPreference(diagram, Info.HORIZONTAL_SPACING, "88");
		awaitSaves();

		assertEquals("a single put saves as before", 1, saveJobsScheduled.get());
		assertTrue("value saved", read().contains("\"88\""));
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

	private String read() throws CoreException, IOException {
		preferencesFile.refreshLocal(IResource.DEPTH_ZERO, monitor);
		try (InputStream in = preferencesFile.getContents(true)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
