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
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.BuilderPreferences;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Settings;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

/**
 * The Profile Real-Time Preview Diagram Style lives in
 * {@code .builder-preferences} at every level (issue #411). A project-level
 * value stored in {@code .cimtool-settings} by an earlier release moves there
 * when the project's builder preferences are loaded, and a profile resolves its
 * style from its own value, then its project's, then the workspace's.
 */
public class DiagramStyleTests extends WorkspaceTest {

	private static final String BUILDER_SAVE_JOB = "Saving CIMTool builder preferences";
	private static final String SETTINGS_SAVE_JOB = "Saving CIMTool settings";
	private static final long TIMEOUT_MILLIS = 10000;

	private static final String WORKSPACE_STYLE = "puml-rdfs-t2b";
	private static final String PROJECT_STYLE = "puml-xsd-t2b";
	private static final String PROFILE_STYLE = "puml-json-l2r";

	private IPreferenceStore store;
	private BuilderPreferences preferences;
	private Settings settings;
	private IFile profile;

	private final AtomicInteger savesScheduled = new AtomicInteger();
	private final AtomicInteger savesDone = new AtomicInteger();
	private final IJobChangeListener saveJobCounter = new JobChangeAdapter() {
		@Override
		public void scheduled(IJobChangeEvent event) {
			if (isSave(event))
				savesScheduled.incrementAndGet();
		}

		@Override
		public void done(IJobChangeEvent event) {
			if (isSave(event))
				savesDone.incrementAndGet();
		}

		private boolean isSave(IJobChangeEvent event) {
			String name = event.getJob().getName();
			return BUILDER_SAVE_JOB.equals(name) || SETTINGS_SAVE_JOB.equals(name);
		}
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Job.getJobManager().addJobChangeListener(saveJobCounter);
		workspace.run(Task.createProject(project, null), monitor);
		preferences = CIMToolPlugin.getBuilderPreferences();
		settings = CIMToolPlugin.getSettings();
		profile = project.getFile("Profiles/Test.owl");
		store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setValue(Info.CURRENT_PROFILE_PREVIEW_STYLE.getLocalName(), WORKSPACE_STYLE);
	}

	@Override
	protected void tearDown() throws Exception {
		awaitSaves();
		Job.getJobManager().removeJobChangeListener(saveJobCounter);
		store.setToDefault(Info.CURRENT_PROFILE_PREVIEW_STYLE.getLocalName());
		super.tearDown();
	}

	public final void testProjectStyleMovesToBuilderPreferences() throws Exception {
		storeLegacyProjectStyle(PROJECT_STYLE);

		assertEquals("project style read from .builder-preferences", PROJECT_STYLE,
				Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(project));
		awaitSaves();

		assertNull("removed from .cimtool-settings",
				settings.getSetting(project, Info.CURRENT_PROFILE_PREVIEW_STYLE));
		assertTrue("saved in .builder-preferences", read(Info.getBuilderPreferences(project)).contains(PROJECT_STYLE));
		assertFalse("gone from the .cimtool-settings file",
				read(Info.getSettings(project)).contains(Info.CURRENT_PROFILE_PREVIEW_STYLE.getLocalName()));
	}

	public final void testMigrationRunsOnce() throws Exception {
		storeLegacyProjectStyle(PROJECT_STYLE);
		Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(project);
		awaitSaves();

		reloadBuilderPreferences();
		savesScheduled.set(0);
		savesDone.set(0);

		assertEquals(PROJECT_STYLE, Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(project));
		awaitSaves();
		assertEquals("nothing left to move, nothing saved", 0, savesScheduled.get());
	}

	public final void testEmptyLegacyStyleIsDropped() throws Exception {
		storeLegacyProjectStyle("");

		assertNull("no project style", Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(project));
		awaitSaves();

		assertNull("removed from .cimtool-settings",
				settings.getSetting(project, Info.CURRENT_PROFILE_PREVIEW_STYLE));
	}

	public final void testExistingBuilderPreferenceIsKept() throws Exception {
		preferences.putPreference(project, Info.CURRENT_PROFILE_PREVIEW_STYLE, PROFILE_STYLE);
		storeLegacyProjectStyle(PROJECT_STYLE);

		assertEquals("value already in .builder-preferences kept", PROFILE_STYLE,
				Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(project));
		awaitSaves();

		assertNull("legacy value removed", settings.getSetting(project, Info.CURRENT_PROFILE_PREVIEW_STYLE));
	}

	public final void testProfileOwnStyleWins() throws Exception {
		preferences.putPreference(project, Info.CURRENT_PROFILE_PREVIEW_STYLE, PROJECT_STYLE);
		preferences.putPreference(profile, Info.CURRENT_PROFILE_PREVIEW_STYLE, PROFILE_STYLE);

		assertEquals(PROFILE_STYLE, Info.getCurrentProfileRealTimePreviewDisplayStyle(profile));
	}

	public final void testProfileInheritsProjectStyle() throws Exception {
		preferences.putPreference(project, Info.CURRENT_PROFILE_PREVIEW_STYLE, PROJECT_STYLE);

		assertEquals(PROJECT_STYLE, Info.getCurrentProfileRealTimePreviewDisplayStyle(profile));
	}

	public final void testProfileInheritsWorkspaceStyle() throws Exception {
		assertEquals(WORKSPACE_STYLE, Info.getCurrentProfileRealTimePreviewDisplayStyle(profile));
	}

	public final void testLevelShowsOnlyItsOwnStyle() throws Exception {
		assertNull("project sets no style of its own",
				Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(project));
		assertNull("profile sets no style of its own",
				Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(profile));
	}

	/**
	 * Stores a project-level Diagram Style where releases before 2.4.0 put it, in
	 * {@code .cimtool-settings}, and discards the loaded builder preferences so
	 * that the next read loads them again, as when the workbench starts.
	 */
	private void storeLegacyProjectStyle(String style) throws Exception {
		settings.putSetting(project, Info.CURRENT_PROFILE_PREVIEW_STYLE, style);
		reloadBuilderPreferences();
	}

	private void reloadBuilderPreferences() throws Exception {
		awaitSaves();
		project.setSessionProperty(BuilderPreferences.BUILDER_PREFERENCES, null);
	}

	/**
	 * Waits until every save job scheduled so far has finished.
	 */
	private void awaitSaves() throws InterruptedException {
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (savesDone.get() < savesScheduled.get()) {
			if (System.currentTimeMillis() > deadline)
				fail("timed out waiting for preferences to be saved");
			Thread.sleep(50);
		}
	}

	private String read(IFile file) throws CoreException, IOException {
		file.refreshLocal(IResource.DEPTH_ZERO, monitor);
		try (InputStream in = file.getContents(true)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
