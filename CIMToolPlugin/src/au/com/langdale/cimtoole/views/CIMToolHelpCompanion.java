package au.com.langdale.cimtoole.views;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * Brings up the CIMTool Context Help panel whenever the user asks for context help
 * (F1) in the CIMTool perspective. The panel opens in the perspective's help column,
 * where Eclipse's Help view also opens, so the two are docked together as sibling tabs at
 * the Help view's usual position and width. The panel is made the active tab because
 * it shows the CIMTool help page itself; the Help view stays one click away for
 * search, the index and bookmarks.
 * <p>
 * F1 is recognised in two ways, because platforms deliver it differently: as the
 * Show Contextual Help command, and by the Help view opening. Clicking the Help tab
 * does neither, so it never pulls the panel back over the Help view.
 * </p>
 * <p>
 * Contributed through {@code org.eclipse.ui.startup}. If startup contributions are
 * disabled under Window &gt; Preferences &gt; General &gt; Startup and Shutdown, the
 * panel can still be opened from Window &gt; Show View.
 * </p>
 */
public class CIMToolHelpCompanion implements IStartup {

	private static final String HELP_VIEW_ID = "org.eclipse.help.ui.HelpView";
	private static final String CONTEXT_HELP_COMMAND_ID = "org.eclipse.ui.help.dynamicHelp";
	private static final String CIMTOOL_PERSPECTIVE_ID = "au.com.langdale.cimtoole.CIMToolPerspective";

	private final IExecutionListener commandListener = new IExecutionListener() {
		@Override
		public void postExecuteSuccess(String commandId, Object returnValue) {
			if (CONTEXT_HELP_COMMAND_ID.equals(commandId))
				showPanel(activePage());
		}

		@Override
		public void preExecute(String commandId, ExecutionEvent event) {
		}

		@Override
		public void postExecuteFailure(String commandId, ExecutionException exception) {
		}

		@Override
		public void notHandled(String commandId, NotHandledException exception) {
		}
	};

	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partOpened(IWorkbenchPartReference ref) {
			if (HELP_VIEW_ID.equals(ref.getId()))
				showPanel(ref.getPage());
		}
	};

	private final IWindowListener windowListener = new IWindowListener() {
		@Override
		public void windowOpened(IWorkbenchWindow window) {
			window.getPartService().addPartListener(partListener);
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			window.getPartService().removePartListener(partListener);
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
			ICommandService commands = workbench.getService(ICommandService.class);
			if (commands != null)
				commands.addExecutionListener(commandListener);
			workbench.addWindowListener(windowListener);
			for (IWorkbenchWindow window : workbench.getWorkbenchWindows())
				window.getPartService().addPartListener(partListener);
		});
	}

	private static IWorkbenchPage activePage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return window == null ? null : window.getActivePage();
	}

	private static void showPanel(IWorkbenchPage page) {
		if (page == null)
			return;
		IPerspectiveDescriptor perspective = page.getPerspective();
		if (perspective == null || !CIMTOOL_PERSPECTIVE_ID.equals(perspective.getId()))
			return;
		// Defer until the Help view has finished opening or activating, then dock the panel
		// beside it and make it the active (selected) tab. The panel keeps the page of the
		// part F1 was pressed in, since it ignores its own activation.
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			try {
				page.showView(CIMToolHelpView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				// The panel is optional: Eclipse's Help view is already open.
			}
		});
	}
}
