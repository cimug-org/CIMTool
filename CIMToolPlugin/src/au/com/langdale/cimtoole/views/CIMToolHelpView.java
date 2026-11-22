package au.com.langdale.cimtoole.views;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.HelpContextIds;
import au.com.langdale.cimtoole.editors.ProfileEditor;

/**
 * The CIMTool Context Help panel: shows the CIMTool help page for the part the user is
 * working in, and follows them as they move between CIMTool views, editors and
 * Profile Editor tabs.
 * <p>
 * Unlike Eclipse's Help view, whose Related Topics page shows a summary with a
 * link, this panel always shows the help page itself. A part's page is the first
 * topic of its help context in contexts_CIMTool.xml, so F1 and this panel
 * always agree. Parts with no CIMTool help leave the current page in place.
 * The panel listens only while it is open.
 * <p>
 * Links to web sites outside the help, such as GitHub issues or cimtool.ucaiug.io,
 * open in the system's web browser rather than in the panel, which is too narrow
 * for them and, on some Windows systems, uses an engine that cannot display them.
 */
public class CIMToolHelpView extends ViewPart {

	public static final String ID = "au.com.langdale.cimtoole.views.CIMToolHelpView";

	private static final String HELP_HTML = "/au.com.langdale.cimtoole.help/html/";
	private static final String HOME = HELP_HTML + "cimtool_topics.html";
	private static final String EDITORS_SECTION = HELP_HTML + "workbench_basics.html#editors";

	/** Parts whose page is the first topic of their CIMTool help context. */
	private static final Map<String, String> PART_CONTEXTS = new HashMap<>();

	/** Parts with no CIMTool help context of their own, mapped straight to a page. */
	private static final Map<String, String> PART_PAGES = new HashMap<>();

	static {
		PART_CONTEXTS.put("au.com.langdale.cimtoole.views.ProjectModelView", HelpContextIds.PROJECT_MODEL_VIEW);
		PART_CONTEXTS.put("au.com.langdale.cimtoole.views.Documentation", HelpContextIds.DOCUMENTATION_VIEW);
		PART_CONTEXTS.put("au.com.langdale.cimtoole.views.CurrentProfilePlantUmlSvgView", HelpContextIds.PROFILE_PREVIEW_VIEW);
		PART_CONTEXTS.put("au.com.langdale.cimtoole.views.ValidationView", HelpContextIds.VALIDATION_PROJECTS_VIEW);

		PART_PAGES.put("org.eclipse.ui.navigator.ProjectExplorer", HELP_HTML + "project_explorer.html");
		PART_PAGES.put("net.sourceforge.plantuml.eclipse.views.PlantUmlSvgView", HELP_HTML + "workbench_basics.html#plantuml-svg");
		PART_PAGES.put("au.com.langdale.cimtoole.editors.InfoModelEditor", EDITORS_SECTION);
		PART_PAGES.put("au.com.langdale.cimtoole.editors.EAInfoModelEditor", EDITORS_SECTION);
		PART_PAGES.put("au.com.langdale.cimtoole.editors.DiagnosisEditor", EDITORS_SECTION);
		PART_PAGES.put("au.com.langdale.cimtoole.editors.RepairEditor", EDITORS_SECTION);
		PART_PAGES.put("au.com.langdale.cimtoole.editors.MappingEditor", EDITORS_SECTION);
	}

	private Browser browser;
	private String currentHref;
	private ProfileEditor trackedEditor;

	private final IPageChangedListener pageListener = event -> showForProfileEditor(trackedEditor);

	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference ref) {
			showForPart(ref.getPart(false));
		}

		@Override
		public void partClosed(IWorkbenchPartReference ref) {
			if (trackedEditor != null && ref.getPart(false) == trackedEditor)
				trackEditor(null);
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference ref) {
		}

		@Override
		public void partOpened(IWorkbenchPartReference ref) {
		}

		@Override
		public void partHidden(IWorkbenchPartReference ref) {
		}

		@Override
		public void partVisible(IWorkbenchPartReference ref) {
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference ref) {
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		try {
			browser = new Browser(parent, SWT.NONE);
		} catch (SWTError e) {
			Label label = new Label(parent, SWT.WRAP);
			label.setText("The CIMTool Context Help panel needs an embedded browser, which is not available here. Use Help > Help Contents instead.");
			return;
		}
		browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changing(LocationEvent event) {
				if (!event.top)
					return;
				String liveTarget = liveHelpTarget(event.location);
				if (liveTarget != null) {
					// A help page asked for OpenInSystemBrowserAction through liveAction().
					// Help pages shown in this panel are not in a help frameset, so the
					// request arrives as a navigation; run it here rather than through the
					// help server.
					event.doit = false;
					if (OpenInSystemBrowserAction.isWebAddress(liveTarget))
						OpenInSystemBrowserAction.open(liveTarget);
				} else if (isExternal(event.location)) {
					event.doit = false;
					openExternally(event.location);
				}
			}
		});
		createActions();
		IWorkbenchPage page = getSite().getPage();
		page.addPartListener(partListener);
		IWorkbenchPart active = page.getActivePart();
		if (active == null || active == this)
			active = page.getActiveEditor();
		showForPart(active);
		if (currentHref == null)
			showHref(HOME);
	}

	private void createActions() {
		IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();

		Action back = new Action("Back") {
			@Override
			public void run() {
				browser.back();
			}
		};
		back.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_BACK));

		Action forward = new Action("Forward") {
			@Override
			public void run() {
				browser.forward();
			}
		};
		forward.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));

		Action home = new Action("CIMTool Topics") {
			@Override
			public void run() {
				showHref(HOME);
			}
		};
		home.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_ETOOL_HOME_NAV));

		toolBar.add(back);
		toolBar.add(forward);
		toolBar.add(home);
	}

	private void showForPart(IWorkbenchPart part) {
		if (part == null || part == this)
			return;
		if (part instanceof ProfileEditor) {
			ProfileEditor editor = (ProfileEditor) part;
			trackEditor(editor);
			showForProfileEditor(editor);
			return;
		}
		String partId = part.getSite().getId();
		String contextId = PART_CONTEXTS.get(partId);
		if (contextId != null) {
			showContext(contextId);
			return;
		}
		String href = PART_PAGES.get(partId);
		if (href != null)
			showHref(href);
		// Any other part, such as the Outline or a third-party editor, keeps the current page.
	}

	private void trackEditor(ProfileEditor editor) {
		if (trackedEditor == editor)
			return;
		if (trackedEditor != null)
			trackedEditor.removePageChangedListener(pageListener);
		trackedEditor = editor;
		if (trackedEditor != null)
			trackedEditor.addPageChangedListener(pageListener);
	}

	private void showForProfileEditor(ProfileEditor editor) {
		if (editor != null)
			showContext(editor.getActivePageHelpContextId());
	}

	private void showContext(String contextId) {
		if (contextId == null)
			return;
		IContext context = HelpSystem.getContext(contextId);
		if (context == null)
			return;
		IHelpResource[] topics = context.getRelatedTopics();
		if (topics != null && topics.length > 0)
			showHref(topics[0].getHref());
	}

	private void showHref(String href) {
		if (browser == null || browser.isDisposed() || href == null || href.equals(currentHref))
			return;
		URL url = PlatformUI.getWorkbench().getHelpSystem().resolve(href, true);
		if (url == null)
			return;
		currentHref = href;
		browser.setUrl(url.toString());
	}

	/**
	 * Whether a location is a web page outside the help system: an http or https
	 * address on a different host or port from the local help server.
	 */
	private boolean isExternal(String location) {
		if (location == null)
			return false;
		try {
			URI target = new URI(location);
			String scheme = target.getScheme();
			if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
				return false;
			URL help = PlatformUI.getWorkbench().getHelpSystem().resolve(HOME, true);
			if (help == null)
				return true;
			return !(target.getHost() != null && target.getHost().equalsIgnoreCase(help.getHost())
					&& target.getPort() == help.getPort());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * The web address carried by a live help request for
	 * {@link OpenInSystemBrowserAction}, or null if the location is not one.
	 */
	private static String liveHelpTarget(String location) {
		if (location == null || !location.contains("/livehelp/?"))
			return null;
		String query = location.substring(location.indexOf("/livehelp/?") + "/livehelp/?".length());
		String className = null;
		String arg = null;
		for (String pair : query.split("&")) {
			int eq = pair.indexOf('=');
			if (eq < 0)
				continue;
			String name = pair.substring(0, eq);
			String value;
			try {
				value = java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
			} catch (Exception e) {
				return null;
			}
			if (name.equals("class"))
				className = value;
			else if (name.equals("arg"))
				arg = value;
		}
		return OpenInSystemBrowserAction.class.getName().equals(className) ? arg : null;
	}

	private void openExternally(String location) {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(location));
		} catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.WARNING, CIMToolPlugin.PLUGIN_ID,
					"Could not open " + location + " in the system web browser", e), StatusManager.LOG);
		}
	}

	@Override
	public void setFocus() {
		if (browser != null && !browser.isDisposed())
			browser.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removePartListener(partListener);
		trackEditor(null);
		super.dispose();
	}
}
