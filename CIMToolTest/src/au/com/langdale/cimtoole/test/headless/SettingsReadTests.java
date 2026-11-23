/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Settings;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

/**
 * Reading a setting with {@link Info#getProperty} never writes to the project's
 * {@code .cimtool-settings} file (issue #411). A resource with no value of its
 * own reads the workspace value, and keeps following it, instead of having the
 * value copied into the file on first read.
 */
public class SettingsReadTests extends WorkspaceTest {

	private static final String SAVE_JOB = "Saving CIMTool settings";
	private static final long TIMEOUT_MILLIS = 10000;

	private static final String FIRST = "http://example.com/first#";
	private static final String SECOND = "http://example.com/second#";

	private IPreferenceStore store;
	private Settings settings;

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
		settings = CIMToolPlugin.getSettings();
		store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setValue(Info.SCHEMA_NAMESPACE.getLocalName(), FIRST);

		// Load the settings store before counting starts. A new project's store is
		// created by a one-off migration that writes the file in a job of its own.
		settings.getSetting(project, Info.SCHEMA_NAMESPACE);
		Job.getJobManager().addJobChangeListener(saveJobCounter);
	}

	@Override
	protected void tearDown() throws Exception {
		awaitSaves();
		Job.getJobManager().removeJobChangeListener(saveJobCounter);
		store.setToDefault(Info.SCHEMA_NAMESPACE.getLocalName());
		super.tearDown();
	}

	public final void testReadingMissingSettingDoesNotStoreIt() throws Exception {
		assertEquals("workspace value read", FIRST, Info.getProperty(project, Info.SCHEMA_NAMESPACE));
		awaitSaves();

		assertNull("nothing stored for the project", settings.getSetting(project, Info.SCHEMA_NAMESPACE));
		assertEquals("no save of .cimtool-settings", 0, saveJobsScheduled.get());
	}

	public final void testMissingSettingFollowsWorkspace() throws Exception {
		assertEquals(FIRST, Info.getProperty(project, Info.SCHEMA_NAMESPACE));

		store.setValue(Info.SCHEMA_NAMESPACE.getLocalName(), SECOND);

		assertEquals("later workspace value read", SECOND, Info.getProperty(project, Info.SCHEMA_NAMESPACE));
	}

	public final void testStoredSettingWins() throws Exception {
		Info.putProperty(project, Info.SCHEMA_NAMESPACE, SECOND);
		awaitSaves();

		assertEquals("stored value read", SECOND, Info.getProperty(project, Info.SCHEMA_NAMESPACE));
	}

	public final void testMissingResourceReadsWorkspaceValue() throws Exception {
		assertEquals(FIRST, Info.getProperty(project.getFile("Schema/Missing.qea"), Info.SCHEMA_NAMESPACE));
	}

	/**
	 * Waits until every save job scheduled so far has finished.
	 */
	private void awaitSaves() throws InterruptedException {
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (saveJobsDone.get() < saveJobsScheduled.get()) {
			if (System.currentTimeMillis() > deadline)
				fail("timed out waiting for settings to be saved");
			Thread.sleep(50);
		}
	}
}
