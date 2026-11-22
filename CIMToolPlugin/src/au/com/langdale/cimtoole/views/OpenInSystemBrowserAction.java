package au.com.langdale.cimtoole.views;

import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.ILiveHelpAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import au.com.langdale.cimtoole.CIMToolPlugin;

/**
 * A live help action that opens a web address in the system's web browser.
 * <p>
 * CIMTool help pages call it for links to web sites outside the help, such as
 * GitHub issues and cimtool.ucaiug.io, through the Eclipse help system's
 * <code>liveAction</code> JavaScript function:
 *
 * <pre>
 * &lt;a href="https://github.com/cimug-org/CIMTool/issues/272"
 *    onclick="liveAction('au.com.langdale.cimtoole',
 *        'au.com.langdale.cimtoole.views.OpenInSystemBrowserAction', this.href); return false;"&gt;
 * </pre>
 *
 * Without it, such a link opens inside the help, where the Internet Explorer
 * engine used by the embedded browsers on some Windows systems cannot display
 * many modern web sites. The link keeps its real address, so it still works
 * wherever JavaScript or live help is unavailable.
 * <p>
 * Only <code>http</code> and <code>https</code> addresses are opened; anything
 * else is ignored.
 */
public class OpenInSystemBrowserAction implements ILiveHelpAction {

	private String address;

	@Override
	public void setInitializationString(String data) {
		address = data;
	}

	@Override
	public void run() {
		final String location = address;
		if (!isWebAddress(location))
			return;
		// The help system runs live help actions on a background thread.
		Display display = PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench().getDisplay() : Display.getDefault();
		display.asyncExec(() -> open(location));
	}

	static boolean isWebAddress(String location) {
		if (location == null)
			return false;
		try {
			String scheme = new URI(location.trim()).getScheme();
			return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
		} catch (Exception e) {
			return false;
		}
	}

	static void open(String location) {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(location.trim()));
		} catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.WARNING, CIMToolPlugin.PLUGIN_ID,
					"Could not open " + location + " in the system web browser", e), StatusManager.LOG);
		}
	}
}
