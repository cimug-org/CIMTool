package au.com.langdale.cimtoole.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

/**
 * Starts the regeneration of profile artifacts after a CIMTool upgrade (issue
 * #310).
 *
 * <p>
 * Contributed through {@code org.eclipse.ui.startup}, separately from the
 * temporary SWT workaround in
 * {@link au.com.langdale.cimtoole.interceptors.ProjectExplorerDoubleClickInterceptor},
 * so that it does not depend on that workaround remaining in place. At startup
 * it registers a resource change listener, then queues every open CIMTool
 * project with {@link UpgradeRegeneration#schedule}, which keeps only those
 * whose stamp is missing or older than the running version.
 * </p>
 *
 * <p>
 * The listener queues projects added to the workspace or opened later, for
 * example by a generic Eclipse import or by opening a closed project. Projects
 * imported through the Import CIMTool Projects wizard are also queued by the
 * wizard itself, so they are covered when startup contributions are disabled
 * under Window &gt; Preferences &gt; General &gt; Startup and Shutdown.
 * </p>
 */
public class UpgradeRegenerationStartup implements IStartup, IResourceChangeListener {

	@Override
	public void earlyStartup() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		UpgradeRegeneration.schedule(Arrays.asList(workspace.getRoot().getProjects()));
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;
		List<IProject> projects = new ArrayList<>();
		for (IResourceDelta child : delta.getAffectedChildren(IResourceDelta.ADDED | IResourceDelta.CHANGED)) {
			boolean added = child.getKind() == IResourceDelta.ADDED;
			boolean opened = (child.getFlags() & IResourceDelta.OPEN) != 0;
			if ((added || opened) && child.getResource() instanceof IProject)
				projects.add((IProject) child.getResource());
		}
		if (!projects.isEmpty())
			UpgradeRegeneration.schedule(projects);
	}
}
