/*
 * Temporary workaround for Eclipse SWT issue #1848 ("Edge: Focus jumps back to
 * browser control when trying to leave") as it manifests in CIMTool: when the
 * Edge/WebView2-backed PlantUML real-time preview view holds native focus, the
 * Project Explorer's normal open gestures stop working. A double-click selects
 * the file but opens nothing (the first click is consumed acquiring focus, so SWT
 * never synthesises the double-click), and pressing Enter on a selected file does
 * not open it either.
 *
 * This installs two raw SWT listeners on the Project Explorer tree:
 *   - SWT.MouseDown, which reconstructs the double-click from the primitive mouse
 *     events (which still arrive, since selection continues to work), and
 *   - SWT.KeyDown, which handles Enter / keypad-Enter on the current selection.
 * Both reroute the open through IDE.openEditor(...) - the same path the "Open
 * With" context menu uses, which is unaffected by the focus state. They operate
 * beneath SWT's gesture synthesis, which is why they succeed where the broken
 * gestures do not. (The Enter path is best-effort: SWT.KeyDown only arrives if
 * the tree still holds keyboard focus at the keystroke; see the class comment on
 * the key listener.)
 *
 * TEMPORARY - REMOVE AT PLATFORM UPGRADE. This is a deliberate stopgap scoped to
 * the current Eclipse 2023-06 (4.28) platform. It reaches into the platform-owned
 * Project Explorer viewer by reflection (getCommonViewer().getTree()), which is
 * acceptable ONLY because the workaround is bound to this exact platform. It MUST
 * BE REMOVED as part of the Eclipse/SWT platform upgrade that resolves SWT #1848
 * (the same upgrade other deferred work depends on). The corresponding CIMTool
 * defect remains the long-term fix; this class is the interim mitigation only.
 *
 * To remove: delete this class and the matching
 *   <extension point="org.eclipse.ui.startup"> ... </extension>
 * block in CIMToolPlugin/plugin.xml. No other files are touched.
 */
package au.com.langdale.cimtoole.interceptors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import au.com.langdale.cimtoole.CIMToolPlugin;

public class ProjectExplorerDoubleClickInterceptor implements IStartup {

	/**
	 * Verbose, per-event tracing for diagnosing this workaround. MUST be false for
	 * release builds. When false, the interceptor logs only a single info entry on
	 * attach and a warning if an open genuinely fails; routine clicks and key
	 * presses produce no log output.
	 */
	private static final boolean DEBUG = false;

	private static final String PROJECT_EXPLORER_ID = "org.eclipse.ui.navigator.ProjectExplorer";
	private static final String ATTACHED_KEY = "cimtool.dblclick.workaround.attached";
	private static final String LOG_PREFIX = "[ProjectExplorerDoubleClickInterceptor] ";

	@Override
	public void earlyStartup() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		if (display == null || display.isDisposed()) {
			return;
		}
		// earlyStartup() runs on a NON-UI thread, so SWT/Display work must be
		// marshalled to the UI thread. asyncExec() is safe from any thread;
		// timerExec() is UI-thread only and so is called from inside asyncExec.
		display.asyncExec(() -> display.timerExec(1500, () -> {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window == null) {
				debug("no active workbench window; interceptor not installed");
				return;
			}
			final IWorkbenchPage page = window.getActivePage();
			if (page == null) {
				debug("no active page; interceptor not installed");
				return;
			}
			// Attach now if the Project Explorer is already open ...
			attachIfPresent(page);
			// ... and whenever it is opened or shown later.
			page.addPartListener(new IPartListener2() {
				@Override
				public void partOpened(IWorkbenchPartReference ref) {
					attachIfPresent(page);
				}

				@Override
				public void partVisible(IWorkbenchPartReference ref) {
					attachIfPresent(page);
				}

				@Override
				public void partActivated(IWorkbenchPartReference ref) {
				}

				@Override
				public void partBroughtToTop(IWorkbenchPartReference ref) {
				}

				@Override
				public void partClosed(IWorkbenchPartReference ref) {
				}

				@Override
				public void partDeactivated(IWorkbenchPartReference ref) {
				}

				@Override
				public void partHidden(IWorkbenchPartReference ref) {
				}

				@Override
				public void partInputChanged(IWorkbenchPartReference ref) {
				}
			});
		}));
	}

	private void attachIfPresent(IWorkbenchPage page) {
		IViewPart view = page.findView(PROJECT_EXPLORER_ID);
		if (view == null) {
			return;
		}
		Tree tree = resolveTree(view);
		if (tree == null || tree.isDisposed()) {
			return;
		}
		if (tree.getData(ATTACHED_KEY) != null) {
			return; // already attached to this tree
		}
		tree.setData(ATTACHED_KEY, Boolean.TRUE);
		installListeners(tree);
		info("open interceptor (double-click + Enter) attached to the Project Explorer tree");
	}

	/**
	 * Resolves the underlying SWT Tree of the Project Explorer via reflection, so
	 * this workaround introduces no compile-time dependency on
	 * org.eclipse.ui.navigator. CommonNavigator#getCommonViewer() returns a
	 * CommonViewer, which extends the JFace TreeViewer.
	 */
	private Tree resolveTree(IViewPart view) {
		try {
			Object viewer = view.getClass().getMethod("getCommonViewer").invoke(view);
			if (viewer instanceof TreeViewer) {
				return ((TreeViewer) viewer).getTree();
			}
			warn("Project Explorer viewer is not a TreeViewer: "
					+ (viewer == null ? "null" : viewer.getClass().getName()));
		} catch (Exception e) {
			warn("could not resolve Project Explorer tree: " + e);
		}
		return null;
	}

	private void installListeners(final Tree tree) {
		final int[] lastTime = { 0 };
		final TreeItem[] lastItem = { null };

		// Mouse path: reconstruct the double-click from primitive mouse-down events.
		tree.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.button != 1) {
					return; // primary (left) button only
				}
				TreeItem item = tree.getItem(new Point(event.x, event.y));
				if (item == null) {
					lastItem[0] = null;
					lastTime[0] = 0;
					return;
				}
				int doubleClickTime = tree.getDisplay().getDoubleClickTime();
				int delta = event.time - lastTime[0];
				boolean isSecondClick = item == lastItem[0] && delta >= 0 && delta <= doubleClickTime;

				debug("MouseDown on '" + safeText(item) + "' delta=" + delta + "ms (dct=" + doubleClickTime
						+ ") -> " + (isSecondClick ? "DOUBLE-CLICK reconstructed" : "single"));

				if (isSecondClick) {
					lastItem[0] = null; // reset so a third click does not chain
					lastTime[0] = 0;
					openItem(item, "double-click");
				} else {
					lastItem[0] = item;
					lastTime[0] = event.time;
				}
			}
		});

		// Keyboard path: Enter / keypad-Enter on a single selection. Best-effort:
		// SWT.KeyDown only fires here if the tree still holds keyboard focus at the
		// keystroke. If the WebView2 focus re-grab has already moved focus away, the
		// event never reaches the tree and Enter cannot be rerouted - a documented
		// limitation that is resolved by the platform upgrade, not by this class.
		tree.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode != SWT.CR && event.keyCode != SWT.KEYPAD_CR) {
					return;
				}
				TreeItem[] selection = tree.getSelection();
				if (selection.length != 1) {
					return; // single selection only
				}
				debug("Enter on '" + safeText(selection[0]) + "' -> rerouting open");
				openItem(selection[0], "Enter");
			}
		});
	}

	private void openItem(TreeItem item, String via) {
		IFile file = toFile(item.getData());
		if (file == null) {
			debug(via + ": no IFile resolved from " + item.getData());
			return;
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window == null ? null : window.getActivePage();
		if (page == null) {
			debug(via + " for " + file.getName() + ": no active page to open in");
			return;
		}
		debug(via + ": rerouting open for " + file.getName() + " via IDE.openEditor(...)");
		try {
			IEditorPart editor = IDE.openEditor(page, file);
			debug(via + ": IDE.openEditor RETURNED " + (editor == null ? "null" : editor.getClass().getName())
					+ " for " + file.getName() + " (reroute succeeded)");
		} catch (Exception e) {
			warn(via + ": IDE.openEditor FAILED for " + file.getName() + ": " + e);
		}
	}

	private IFile toFile(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof IFile) {
			return (IFile) data;
		}
		if (data instanceof IAdaptable) {
			IFile adapted = ((IAdaptable) data).getAdapter(IFile.class);
			if (adapted != null) {
				return adapted;
			}
		}
		return Platform.getAdapterManager().getAdapter(data, IFile.class);
	}

	private String safeText(TreeItem item) {
		try {
			return item.getText();
		} catch (Exception e) {
			return "?";
		}
	}

	// Single info breadcrumb on attach; always logged.
	private void info(String message) {
		CIMToolPlugin.getDefault().getLog().info(LOG_PREFIX + message);
	}

	// Genuine failures; always logged.
	private void warn(String message) {
		CIMToolPlugin.getDefault().getLog().warn(LOG_PREFIX + message);
	}

	// Verbose per-event tracing; only when DEBUG is enabled.
	private void debug(String message) {
		if (DEBUG) {
			CIMToolPlugin.getDefault().getLog().info(LOG_PREFIX + message);
		}
	}
}
