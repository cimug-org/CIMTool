package au.com.langdale.cimtoole.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import au.com.langdale.cimtoole.CIMNature;
import au.com.langdale.cimtoole.CIMToolPlugin;

/**
 * Regenerates a project's profile artifacts the first time it is opened by a
 * newer CIMTool release (issue #310).
 *
 * <p>
 * Each project carries a stamp recording the CIMTool version that last
 * regenerated all of its artifacts. A project whose stamp is missing or older
 * than the running version has its profile artifacts regenerated once, in a
 * background job, after which it is stamped with the running version.
 * </p>
 *
 * <p>
 * The stamp is an Eclipse persistent property, held in the workspace metadata.
 * It is deliberately not a {@link au.com.langdale.cimtoole.project.QualifiedNames}
 * constant and is never written to {@code .cimtool-settings}: it describes one
 * workspace's build state, while {@code .cimtool-settings} travels with the
 * project to other workspaces and to the CLI.
 * </p>
 */
public final class UpgradeRegeneration {

	/**
	 * The persistent property holding the CIMTool version, as major.minor.micro,
	 * that last regenerated all of the project's artifacts.
	 */
	public static final QualifiedName LAST_FULL_BUILD_VERSION = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"lastFullBuildVersion");

	/**
	 * The bundle whose version identifies the builders that produced a project's
	 * artifacts. CIMUtil holds the builders, ProfileSerializer and the
	 * post-processing stylesheets.
	 */
	static final String CIMUTIL_BUNDLE = "au.com.langdale.cimutil";

	/**
	 * The job family of the regeneration job, for waiting on it with
	 * {@code Job.getJobManager().join(JOB_FAMILY, monitor)}.
	 */
	public static final Object JOB_FAMILY = UpgradeRegeneration.class;

	private static final Set<IProject> pending = new LinkedHashSet<>();

	private static Job job;

	private UpgradeRegeneration() {
	}

	/**
	 * @return the running CIMUtil version without its qualifier, or null if the
	 *         bundle cannot be found
	 */
	public static Version getRunningVersion() {
		Bundle bundle = Platform.getBundle(CIMUTIL_BUNDLE);
		return bundle == null ? null : withoutQualifier(bundle.getVersion());
	}

	static Version withoutQualifier(Version version) {
		return new Version(version.getMajor(), version.getMinor(), version.getMicro());
	}

	/**
	 * @return the project's stamp, or null if it has none or it cannot be parsed
	 */
	public static Version getStamp(IProject project) throws CoreException {
		String value = project.getPersistentProperty(LAST_FULL_BUILD_VERSION);
		if (value == null)
			return null;
		try {
			return withoutQualifier(Version.parseVersion(value));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static void setStamp(IProject project, Version version) throws CoreException {
		project.setPersistentProperty(LAST_FULL_BUILD_VERSION, withoutQualifier(version).toString());
	}

	/**
	 * Stamps the project with the running version. Called by {@link CIMBuilder}
	 * after a build that traversed the whole project, and after regeneration.
	 */
	public static void stampRunningVersion(IProject project) throws CoreException {
		Version running = getRunningVersion();
		if (running != null)
			setStamp(project, running);
	}

	/**
	 * A project needs regeneration when it is open, has the CIMTool nature, and
	 * its stamp is missing or older than the running version. A stamp equal to or
	 * newer than the running version never triggers regeneration.
	 */
	public static boolean needsUpgradeRebuild(IProject project) {
		try {
			if (!project.isOpen() || !project.hasNature(CIMNature.NATURE_ID))
				return false;
			return isOlder(getStamp(project), getRunningVersion());
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * The comparison behind {@link #needsUpgradeRebuild(IProject)}: true when there
	 * is a running version and the stamp is missing or older than it.
	 *
	 * <p>
	 * Versions compare numerically, component by component, as
	 * {@link Version#compareTo(Version)} defines. A version written with fewer
	 * than three components is completed with zeros when it is parsed, so a
	 * release numbered 2.4 is 2.4.0: newer than 2.3.0 and equal to 2.4.0. A
	 * missing component is never null.
	 * </p>
	 *
	 * @param stamp   the project's stamp, or null if it has none
	 * @param running the running version, or null if it is unknown
	 */
	public static boolean isOlder(Version stamp, Version running) {
		if (running == null)
			return false;
		return stamp == null || withoutQualifier(stamp).compareTo(withoutQualifier(running)) < 0;
	}

	/**
	 * Regenerates the project's profile artifacts and stamps it with the running
	 * version.
	 */
	public static void regenerate(IProject project, IProgressMonitor monitor) throws CoreException {
		CIMBuilder.regenerateProfileArtifacts(project, monitor);
		stampRunningVersion(project);
	}

	/**
	 * Queues the projects that need regeneration and schedules the regeneration
	 * job. Projects queued while the job is waiting are handled by the same run.
	 */
	public static void schedule(Collection<IProject> projects) {
		synchronized (pending) {
			for (IProject project : projects)
				if (needsUpgradeRebuild(project))
					pending.add(project);
			if (pending.isEmpty())
				return;
			if (job == null)
				job = new RegenerationJob();
		}
		job.schedule();
	}

	private static List<IProject> takePending() {
		synchronized (pending) {
			List<IProject> projects = new ArrayList<>(pending);
			pending.clear();
			return projects;
		}
	}

	private static final class RegenerationJob extends Job {

		RegenerationJob() {
			super("Regenerating CIMTool artifacts after upgrade to " + getRunningVersion());
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == JOB_FAMILY;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				// A newly imported project receives its first build from the auto-build
				// job, which also stamps it. Waiting for that build first avoids
				// regenerating the same artifacts twice.
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, monitor);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Status.CANCEL_STATUS;
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			}

			List<IProject> projects = takePending();
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			try {
				workspace.run(inner -> {
					SubMonitor progress = SubMonitor.convert(inner, projects.size());
					for (IProject project : projects) {
						if (needsUpgradeRebuild(project)) {
							regenerate(project, progress.split(1));
							CIMToolPlugin.getDefault().getLog().info("Regenerated CIMTool artifacts in project "
									+ project.getName() + " after upgrade to " + getRunningVersion());
						} else {
							progress.worked(1);
						}
					}
				}, workspace.getRuleFactory().buildRule(), IWorkspace.AVOID_UPDATE, monitor);
			} catch (CoreException e) {
				return e.getStatus();
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		}
	}
}
