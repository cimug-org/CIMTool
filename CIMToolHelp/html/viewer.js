/*
 * CIMToolHelp image viewer.
 *
 * Clicking an image with class "zoomable" (or pressing Enter or Space when it
 * has focus) shows it over the whole page, scaled to fit the help window or
 * panel, on a dark backdrop. Clicking anywhere, or pressing Esc, closes it.
 *
 * Links to web sites outside the help (any http or https address on another
 * host) are marked with an "external" class, which adds a small arrow, and a
 * tooltip saying they open in the web browser and need internet access. When
 * clicked, they open in the system's web browser through cimtoolOpenExternal()
 * below, so that they never load inside the help. Pages load
 * PLUGINS_ROOT/org.eclipse.help/livehelp.js before this script for that.
 *
 * Written in plain ES5 without newer browser APIs, so that it also works in the
 * Internet Explorer engine some Windows systems use for the embedded browser.
 */
(function () {
	var overlay = null;

	function close() {
		if (overlay && overlay.parentNode) {
			overlay.parentNode.removeChild(overlay);
		}
		overlay = null;
	}

	function open(img) {
		close();
		overlay = document.createElement("div");
		overlay.className = "viewer-overlay";
		overlay.setAttribute("role", "dialog");
		overlay.setAttribute("aria-label", img.getAttribute("alt") || "Enlarged image");

		var big = document.createElement("img");
		big.className = "viewer-image";
		big.src = img.src;
		big.alt = img.getAttribute("alt") || "";
		overlay.appendChild(big);

		var hint = document.createElement("div");
		hint.className = "viewer-hint";
		hint.appendChild(document.createTextNode("Click or press Esc to close"));
		overlay.appendChild(hint);

		overlay.onclick = close;
		document.body.appendChild(overlay);
	}

	function isZoomable(el) {
		return el && el.tagName && el.tagName.toLowerCase() === "img" &&
			(" " + el.className + " ").indexOf(" zoomable ") >= 0;
	}

	var EXTERNAL_TITLE = "Opens in your web browser; requires internet access";

	function isExternal(a) {
		if (!a || !a.tagName || a.tagName.toLowerCase() !== "a" || !a.href)
			return false;
		var href = a.href.toLowerCase();
		if (href.indexOf("http://") !== 0 && href.indexOf("https://") !== 0)
			return false;
		return a.host !== window.location.host;
	}

	function enclosingLink(el) {
		while (el && el.tagName && el.tagName.toLowerCase() !== "a") {
			el = el.parentNode;
		}
		return (el && el.tagName) ? el : null;
	}

	function onClick(e) {
		e = e || window.event;
		var target = e.target || e.srcElement;
		var link = enclosingLink(target);
		if (isExternal(link)) {
			if (!cimtoolOpenExternal(link)) {
				if (e.preventDefault) { e.preventDefault(); } else { e.returnValue = false; }
			}
			return;
		}
		if (isZoomable(target)) {
			open(target);
		}
	}

	function onKey(e) {
		e = e || window.event;
		var key = e.keyCode || e.which;
		if (overlay && key === 27) {
			close();
			return;
		}
		var target = e.target || e.srcElement;
		if (!overlay && isZoomable(target) && (key === 13 || key === 32)) {
			if (e.preventDefault) { e.preventDefault(); } else { e.returnValue = false; }
			open(target);
		}
	}

	function init() {
		var links = document.getElementsByTagName("a");
		for (var j = 0; j < links.length; j++) {
			if (isExternal(links[j])) {
				if (!links[j].getAttribute("title")) {
					links[j].setAttribute("title", EXTERNAL_TITLE);
				}
				// Mark text links only; a link around an image, such as the sponsor
				// logo, gets the tooltip but no arrow.
				if (links[j].getElementsByTagName("img").length === 0) {
					links[j].className = (links[j].className ? links[j].className + " " : "") + "external";
				}
			}
		}
		var imgs = document.getElementsByTagName("img");
		for (var i = 0; i < imgs.length; i++) {
			if (isZoomable(imgs[i])) {
				imgs[i].setAttribute("tabindex", "0");
				imgs[i].setAttribute("title", "Click to enlarge");
			}
		}
	}

	if (document.addEventListener) {
		document.addEventListener("click", onClick, false);
		document.addEventListener("keydown", onKey, false);
		document.addEventListener("DOMContentLoaded", init, false);
	} else if (document.attachEvent) {
		document.attachEvent("onclick", onClick);
		document.attachEvent("onkeydown", onKey);
		window.attachEvent("onload", init);
	}
})();

/*
 * Opens a link to a web site outside the help in the system's web browser, using
 * CIMTool's OpenInSystemBrowserAction through the Eclipse help system's live help.
 * Use it as onclick="return cimtoolOpenExternal(this);" on such a link, with the
 * page also loading PLUGINS_ROOT/org.eclipse.help/livehelp.js.
 * It returns false when it has handed the link to live help, and true otherwise,
 * so that the browser follows the link normally: for example in the CIMTool
 * Context Help panel, which opens external links in the system browser itself,
 * or wherever live help is unavailable.
 */
function cimtoolOpenExternal(link) {
	if (typeof liveAction != "function" || typeof findHelpTop != "function")
		return true;
	var top = findHelpTop();
	var inFrameset = top && top.liveActionInternal;
	var inHelpView = window.location.href.indexOf("/ntopic/") > 0;
	if (!inFrameset && !inHelpView)
		return true;
	liveAction("au.com.langdale.cimtoole", "au.com.langdale.cimtoole.views.OpenInSystemBrowserAction", link.href);
	return false;
}
