package au.com.langdale.cimtoole;

import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Hides the editor area of the Browsing perspective again when it has no editors open
 * (issue #408).
 *
 * <p>
 * The Browsing perspective is laid out without an editor area (see
 * {@link CIMToolBrowsingPerspective}). Opening an editor from it, for example by
 * double-clicking a profile, makes the editor area visible in the middle of the window,
 * and Eclipse keeps it visible, empty, after the last editor closes. This class hides it
 * again, so that the Project Explorer and Project Model columns take back the space.
 * </p>
 *
 * <p>
 * Editor area visibility is kept separately for each perspective, and this class acts
 * only while the Browsing perspective is active: other perspectives keep Eclipse's
 * standard behavior.
 * </p>
 */
public class BrowsingPerspectiveEditorArea implements IStartup {

	private static final String BROWSING_PERSPECTIVE_ID = "au.com.langdale.cimtoole.CIMToolBrowsingPerspective";

	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partClosed(IWorkbenchPartReference ref) {
			if (ref instanceof IEditorReference)
				hideIfEmpty(ref.getPage());
		}
	};

	private final IPerspectiveListener perspectiveListener = new IPerspectiveListener() {
		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			hideIfEmpty(page);
		}

		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		}
	};

	private final IWindowListener windowListener = new IWindowListener() {
		@Override
		public void windowOpened(IWorkbenchWindow window) {
			listenTo(window);
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			window.getPartService().removePartListener(partListener);
			window.removePerspectiveListener(perspectiveListener);
		}

		@Override
		public void windowActivated(IWorkbenchWindow window) {
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
		}
	};

	@Override
	public void earlyStartup() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(() -> {
			workbench.addWindowListener(windowListener);
			for (IWorkbenchWindow window : workbench.getWorkbenchWindows())
				listenTo(window);
		});
	}

	private void listenTo(IWorkbenchWindow window) {
		window.getPartService().addPartListener(partListener);
		window.addPerspectiveListener(perspectiveListener);
	}

	private static void hideIfEmpty(IWorkbenchPage page) {
		if (page == null)
			return;
		// Defer until Eclipse has finished closing the editor or switching perspective.
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			IPerspectiveDescriptor perspective = page.getPerspective();
			if (perspective != null && BROWSING_PERSPECTIVE_ID.equals(perspective.getId())
					&& page.getEditorReferences().length == 0 && page.isEditorAreaVisible())
				page.setEditorAreaVisible(false);
		});
	}
}
