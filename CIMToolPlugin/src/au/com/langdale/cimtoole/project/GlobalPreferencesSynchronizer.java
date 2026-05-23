/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import au.com.langdale.cimtoole.CIMToolPlugin;

/**
 * Listens for CIMTool project open events and synchronizes global preferences.
 * 
 * <p>
 * When a CIMTool project is opened (or imported), this listener exports the
 * current global preferences from the Eclipse PreferenceStore to the project's
 * {@code .cimtool-global-preferences} file. This ensures the CLI can use the
 * same customized defaults as the Eclipse UI.
 * </p>
 * 
 * <p>
 * On registration (called from the plugin activator's {@code start()} method),
 * this class also performs a one-time startup scan of all currently-open
 * CIMTool projects and exports preferences to any project whose
 * {@code .cimtool-global-preferences} file is missing. This handles the case
 * where an older workspace (predating the introduction of this file) is opened
 * in a newer release of CIMTool — no OPEN event fires for projects that were
 * already open in a previously-used workspace, so without the startup scan
 * those projects would never receive the file.
 * </p>
 * 
 * <p>
 * Register this listener in the plugin activator's {@code start()} method:
 * </p>
 * 
 * <pre>
 * ResourcesPlugin.getWorkspace().addResourceChangeListener(new GlobalPreferencesSynchronizer(),
 * 		IResourceChangeEvent.POST_CHANGE);
 * </pre>
 * 
 * @see GlobalPreferencesExporter
 */
public class GlobalPreferencesSynchronizer implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
			return;
		}

		IResourceDelta delta = event.getDelta();
		if (delta == null) {
			return;
		}

		try {
			delta.accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					// Only interested in projects
					if (delta.getResource() instanceof IProject) {
						IProject project = (IProject) delta.getResource();

						// Check if project was opened (OPEN flag is set when open state changes)
						if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
							// Only process if project is now open and is a CIMTool project
							if (project.isOpen() && Info.isCIMToolProject(project)) {
								scheduleExportGlobalPreferences(project);
							}
						}

						// Don't visit children of projects
						return false;
					}

					// Visit children (to find projects inside workspace root)
					return true;
				}
			});
		} catch (CoreException e) {
			CIMToolPlugin.getDefault().getLog().error(e.getMessage(), e);
		}
	}

	/**
	 * Schedule export of global preferences to the project.
	 * 
	 * <p>
	 * This must be done asynchronously because the workspace is locked during
	 * resource change notifications.
	 * </p>
	 */
	private void scheduleExportGlobalPreferences(final IProject project) {
		WorkspaceJob job = new WorkspaceJob("Export global preferences to " + project.getName()) {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					if (project.isOpen() && Info.isCIMToolProject(project)) {
						GlobalPreferencesExporter.exportGlobalPreferencesToProject(project);
					}
				} catch (CoreException e) {
					CIMToolPlugin.getDefault().getLog().error(e.getMessage(), e);
					return new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, "Failed to export global preferences", e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(project);
		job.schedule();
	}

	/**
	 * Register this listener with the workspace and perform a one-time startup scan.
	 * 
	 * <p>
	 * The startup scan exports global preferences to any currently-open CIMTool
	 * project whose {@code .cimtool-global-preferences} file is absent. This covers
	 * the case where an older workspace (predating this file) is opened in a newer
	 * release of CIMTool — projects already open in a previously-used workspace do
	 * not generate an OPEN event when that workspace is switched to, so the
	 * listener's normal event-driven path would never fire for them.
	 * </p>
	 * 
	 * <p>
	 * Call this from the plugin activator's {@code start()} method.
	 * </p>
	 */
	public static GlobalPreferencesSynchronizer register() {
		GlobalPreferencesSynchronizer listener = new GlobalPreferencesSynchronizer();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		listener.exportMissingGlobalPreferences();
		return listener;
	}

	/**
	 * Perform a one-time scan of all currently-open CIMTool projects and export
	 * global preferences to any project whose {@code .cimtool-global-preferences}
	 * file is missing.
	 * 
	 * <p>
	 * This is scheduled as an asynchronous {@link WorkspaceJob} so it does not
	 * block plugin startup and does not run while the workspace lock is held.
	 * </p>
	 */
	private void exportMissingGlobalPreferences() {
		WorkspaceJob job = new WorkspaceJob("Initializing global preferences for open CIMTool projects") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (IProject project : projects) {
					try {
						if (project.isOpen() && Info.isCIMToolProject(project)) {
							IFile prefsFile = Info.getGlobalPreferences(project);
							if (prefsFile != null && !prefsFile.exists()) {
								GlobalPreferencesExporter.exportGlobalPreferencesToProject(project);
							}
						}
					} catch (CoreException e) {
						CIMToolPlugin.getDefault().getLog().error(e.getMessage(), e);
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/**
	 * Unregister this listener from the workspace. Call this from the plugin
	 * activator's stop() method.
	 */
	public static void unregister(GlobalPreferencesSynchronizer listener) {
		if (listener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
		}
	}
}