/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants. Langdale
 * Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.cimtoole.project.Settings;
import au.com.langdale.ui.util.GeneralIconCache;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.ui.util.NodeTraits;

/**
 * The activator class for the CIMTool plugin. It manages the lifetime of the
 * model cache.
 */
public class CIMToolPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "au.com.langdale.cimtoole";

	// The RDF namespace for settings subjects
	public static final String PROJECT_NS = "http://cimtoole.langdale.com.au/2009/project/";

	// The RDF namespace for settings properties
	public static final String SETTING_NS = "http://cimtoole.langdale.com.au/2009/setting#";

	// The shared instance
	private static CIMToolPlugin plugin;

	// the model cache
	private static Cache cache;

	// the settings database
	private static Settings settings;

	/**
	 * The constructor
	 */
	public CIMToolPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		setJNALibraryPath();
		//
		super.start(context);
		IconCache.setIcons(new CIMToolIconCache(context.getBundle(), "/icons/"));
		cache = new Cache();
		settings = new Settings();
		plugin = this;
	}

	private String getPlatformFolder() {
		String os = System.getProperty("os.name").toLowerCase();
		String arch = System.getProperty("os.arch").toLowerCase();

		if (os.contains("win")) {
			return arch.contains("64") ? "win32-x86-64" : "win32-x86";
		} else if (os.contains("linux")) {
			return arch.contains("64") ? "linux-x86-64" : "linux-x86";
		} else if (os.contains("mac")) {
			return "macosx-x86-64";
		} else {
			throw new IllegalStateException("Unsupported platform: " + os);
		}
	}

	private void setJNALibraryPath() throws IOException {
		if (Platform.isRunning()) {
			Location configLocation = Platform.getConfigurationLocation();
			URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

			File dataAreaDir = new File(dataArea.getPath());
			dataAreaDir = new File(dataAreaDir.getPath());


			// Specify the path to the directory containing the native libraries
			// String libraryPath = "C:\\Program Files\\Sparx Systems\\EA\\Firebird";
			String libraryPath = "C:\\CIMug-GIT-REPOS\\CIMTool\\CIMToolPlugin\\native\\" + getPlatformFolder();
			// Set the jna.library.path system property
			System.setProperty("jna.library.path", libraryPath);

		} else {
			// Determine the platform-specific subfolder
			String platform = getPlatformFolder();
	
			// Build the full path to the DLLs
			IPath stateLocation = getStateLocation();
			IPath nativePath = stateLocation.append("native").append(platform);
			File nativeDir = nativePath.toFile();
	
			// Add the native directory to the system property for loading libraries
			System.setProperty("jna.library.path", nativeDir.getAbsolutePath());
		}

		System.err.println(System.getProperty("jna.library.path"));
	}

	private static class CIMToolIconCache extends GeneralIconCache {
		public CIMToolIconCache(Bundle bundleToUse, String prefixPath) {
			super(bundleToUse, prefixPath);
		}

		@Override
		public Image get(Object value, int size) {
			if (value instanceof NodeTraits) {
				NodeTraits node = (NodeTraits) value;
				return get(node.getIconClass(), node.getErrorIndicator(), size);
			} else
				return super.get(value, size);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CIMToolPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative
	 * path
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static Cache getCache() {
		return cache;
	}

	public static Settings getSettings() {
		return settings;
	}

}
