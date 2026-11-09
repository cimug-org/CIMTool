package au.com.langdale.cimtoole.test.headless;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Version;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.CopyBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.CIMNature;
import au.com.langdale.cimtoole.builder.UpgradeRegeneration;
import au.com.langdale.cimtoole.builder.UpgradeRegenerationStartup;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.OntModel;

/**
 * Tests for {@link UpgradeRegeneration}: the per-project version stamp, the
 * rule deciding when a project needs regeneration, stamping by the builder,
 * and regeneration of profile artifacts (issue #310).
 *
 * <p>
 * Automatic building is turned off for these tests so that only the builds
 * each test requests can change a stamp.
 * </p>
 */
public class UpgradeRegenerationTests extends ProjectTest {

	private static final Version OLDER = new Version(2, 3, 0);
	private static final Version NEWER = new Version(99, 0, 0);

	private static final XSDBuildlet xsdBuildlet = new XSDBuildlet("xsd", "xsd");
	private static final CopyBuildlet turtleBuildlet = new CopyBuildlet(Format.TURTLE.toFormat(), "ttl");

	private boolean autoBuilding;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IWorkspaceDescription description = workspace.getDescription();
		autoBuilding = description.isAutoBuilding();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		setupSchema();
		setupProfile();
	}

	@Override
	protected void tearDown() throws Exception {
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(autoBuilding);
		workspace.setDescription(description);
		super.tearDown();
	}

	public final void testRunningVersionHasNoQualifier() throws Exception {
		Version running = UpgradeRegeneration.getRunningVersion();
		assertNotNull("CIMUtil bundle found", running);
		assertEquals("no qualifier", "", running.getQualifier());
	}

	public final void testStampRoundTripDropsQualifier() throws Exception {
		project.setPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION, "2.3.1.v20261107");
		assertEquals("qualifier dropped", new Version(2, 3, 1), UpgradeRegeneration.getStamp(project));
		UpgradeRegeneration.setStamp(project, OLDER);
		assertEquals("stamp written as major.minor.micro", "2.3.0",
				project.getPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION));
	}

	public final void testMissingStampNeedsRebuild() throws Exception {
		project.setPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION, null);
		assertTrue("missing stamp", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testTwoComponentStampReadsWithZeroMicro() throws Exception {
		project.setPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION, "2.4");
		assertEquals("2.4 read as 2.4.0", new Version(2, 4, 0), UpgradeRegeneration.getStamp(project));
	}

	public final void testEmptyStampNeedsRebuild() throws Exception {
		project.setPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION, "");
		assertTrue("empty stamp", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testUnreadableStampNeedsRebuild() throws Exception {
		project.setPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION, "not a version");
		assertTrue("unreadable stamp", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testOlderStampNeedsRebuild() throws Exception {
		UpgradeRegeneration.setStamp(project, OLDER);
		assertTrue("older stamp", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testEqualStampDoesNotNeedRebuild() throws Exception {
		UpgradeRegeneration.setStamp(project, UpgradeRegeneration.getRunningVersion());
		assertFalse("equal stamp", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testNewerStampDoesNotNeedRebuild() throws Exception {
		UpgradeRegeneration.setStamp(project, NEWER);
		assertFalse("newer stamp", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testStampsCompareNumerically() throws Exception {
		Version running = UpgradeRegeneration.getRunningVersion();
		Version tenthMinor = new Version(running.getMajor(), running.getMinor() + 10, 0);
		UpgradeRegeneration.setStamp(project, tenthMinor);
		assertFalse("x.(y+10).0 is newer than x.y.z", UpgradeRegeneration.needsUpgradeRebuild(project));
	}

	public final void testClosedProjectDoesNotNeedRebuild() throws Exception {
		UpgradeRegeneration.setStamp(project, OLDER);
		project.close(monitor);
		try {
			assertFalse("closed project", UpgradeRegeneration.needsUpgradeRebuild(project));
		} finally {
			// Reopened so that the next test's setUp deletes the project's files: deleting
			// a closed project leaves its content on disk.
			project.open(monitor);
			Job.getJobManager().join(UpgradeRegeneration.JOB_FAMILY, monitor);
		}
	}

	public final void testProjectWithoutNatureDoesNotNeedRebuild() throws Exception {
		IProject plain = workspace.getRoot().getProject("PlainProject");
		if (plain.exists())
			plain.delete(true, monitor);
		plain.create(monitor);
		plain.open(monitor);
		try {
			assertFalse("project without the CIMTool nature", UpgradeRegeneration.needsUpgradeRebuild(plain));
		} finally {
			plain.delete(true, monitor);
		}
	}

	public final void testFullBuildWritesStamp() throws Exception {
		project.setPersistentProperty(UpgradeRegeneration.LAST_FULL_BUILD_VERSION, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		assertEquals("stamped by a full build", UpgradeRegeneration.getRunningVersion(),
				UpgradeRegeneration.getStamp(project));
	}

	public final void testIncrementalBuildKeepsStamp() throws Exception {
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		UpgradeRegeneration.setStamp(project, OLDER);
		profile.touch(monitor);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		assertEquals("incremental build leaves the stamp", OLDER, UpgradeRegeneration.getStamp(project));
	}

	public final void testRegenerationRebuildsEnabledOutput() throws Exception {
		enableXSDBuilder();
		project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		IFile xsd = getRelated("xsd");
		assertTrue("XSD built", xsd.exists());
		xsd.delete(true, monitor);
		UpgradeRegeneration.setStamp(project, OLDER);

		workspace.run(m -> UpgradeRegeneration.regenerate(project, m), monitor);

		assertTrue("XSD regenerated", xsd.exists());
		assertEquals("stamped by regeneration", UpgradeRegeneration.getRunningVersion(),
				UpgradeRegeneration.getStamp(project));
	}

	public final void testRegenerationCleansDisabledOutput() throws Exception {
		OntModel model = Task.getProfileModel(profile);
		turtleBuildlet.setFlagged(model, false);
		workspace.run(Task.saveProfile(profile, model), monitor);
		IFile ttl = getRelated("ttl");
		ttl.create(new ByteArrayInputStream("# stale".getBytes(StandardCharsets.UTF_8)), true, monitor);

		workspace.run(m -> UpgradeRegeneration.regenerate(project, m), monitor);

		assertFalse("output of a builder not enabled on the profile removed", ttl.exists());
	}

	public final void testScheduleRegeneratesAndStamps() throws Exception {
		enableXSDBuilder();
		UpgradeRegeneration.setStamp(project, OLDER);
		IFile xsd = getRelated("xsd");
		assertFalse("no XSD before regeneration", xsd.exists());

		UpgradeRegeneration.schedule(Collections.singletonList(project));
		Job.getJobManager().join(UpgradeRegeneration.JOB_FAMILY, monitor);

		assertTrue("XSD generated by the job", xsd.exists());
		assertEquals("stamped by the job", UpgradeRegeneration.getRunningVersion(),
				UpgradeRegeneration.getStamp(project));
	}

	public final void testScheduleSkipsCurrentProject() throws Exception {
		enableXSDBuilder();
		UpgradeRegeneration.setStamp(project, UpgradeRegeneration.getRunningVersion());

		UpgradeRegeneration.schedule(Collections.singletonList(project));
		Job.getJobManager().join(UpgradeRegeneration.JOB_FAMILY, monitor);

		assertFalse("current project not regenerated", getRelated("xsd").exists());
	}

	public final void testNewProjectIsStamped() throws Exception {
		assertEquals("stamped when created", UpgradeRegeneration.getRunningVersion(),
				UpgradeRegeneration.getStamp(project));
	}

	public final void testListenerSchedulesReopenedProject() throws Exception {
		UpgradeRegenerationStartup listener = new UpgradeRegenerationStartup();
		workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		try {
			enableXSDBuilder();
			UpgradeRegeneration.setStamp(project, OLDER);
			closeAndWaitForNotification(project);
			project.open(monitor);
			waitForStamp(project);
		} finally {
			workspace.removeResourceChangeListener(listener);
		}
		assertEquals("stamped after the project was opened", UpgradeRegeneration.getRunningVersion(),
				UpgradeRegeneration.getStamp(project));
		assertTrue("XSD generated after the project was opened", getRelated("xsd").exists());
	}

	public final void testListenerSchedulesUnstampedAddedProject() throws Exception {
		IProject added = workspace.getRoot().getProject("AddedProject");
		if (added.exists())
			added.delete(true, monitor);
		UpgradeRegenerationStartup listener = new UpgradeRegenerationStartup();
		workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		try {
			// Created outside Task.createProject, as an import would, so it has no stamp.
			IProjectDescription description = workspace.newProjectDescription(added.getName());
			description.setNatureIds(new String[] { CIMNature.NATURE_ID });
			added.create(description, monitor);
			added.open(monitor);
			waitForStamp(added);
			assertEquals("stamped after the project was added", UpgradeRegeneration.getRunningVersion(),
					UpgradeRegeneration.getStamp(added));
		} finally {
			workspace.removeResourceChangeListener(listener);
			added.delete(true, monitor);
		}
	}

	/**
	 * Closes the project and waits until the workspace has reported the close.
	 * The workspace can hold notifications back while other workspace operations
	 * run and then report several changes as one delta. A close followed at once
	 * by an open would then be reported as no change in the project's open state,
	 * and the listener would have no open to react to. Waiting here keeps the
	 * reopen in a delta of its own.
	 */
	private void closeAndWaitForNotification(IProject target) throws Exception {
		CountDownLatch closed = new CountDownLatch(1);
		IResourceChangeListener watcher = event -> {
			IResourceDelta delta = event.getDelta() == null ? null : event.getDelta().findMember(target.getFullPath());
			if (delta != null && (delta.getFlags() & IResourceDelta.OPEN) != 0 && !target.isOpen())
				closed.countDown();
		};
		workspace.addResourceChangeListener(watcher, IResourceChangeEvent.POST_CHANGE);
		try {
			target.close(monitor);
			assertTrue("the workspace reported the close within 60 seconds", closed.await(60, TimeUnit.SECONDS));
		} finally {
			workspace.removeResourceChangeListener(watcher);
		}
	}

	/**
	 * Waits until the listener has queued the project and the regeneration job has
	 * stamped it, since the workspace can report a change after the operation that
	 * made it has returned. Returns after 60 seconds regardless, leaving the
	 * assertions to report the state.
	 */
	private void waitForStamp(IProject target) throws Exception {
		long deadline = System.currentTimeMillis() + 60_000;
		while (System.currentTimeMillis() < deadline) {
			Job.getJobManager().join(UpgradeRegeneration.JOB_FAMILY, monitor);
			if (UpgradeRegeneration.getRunningVersion().equals(UpgradeRegeneration.getStamp(target)))
				return;
			Thread.sleep(100);
		}
	}

	private void enableXSDBuilder() throws CoreException {
		OntModel model = Task.getProfileModel(profile);
		xsdBuildlet.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
	}
}
