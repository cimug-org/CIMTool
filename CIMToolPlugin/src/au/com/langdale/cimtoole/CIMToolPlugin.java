/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants. Langdale
 * Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import au.com.langdale.cimtoole.pandoc.PandocPathResolver;
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

	private static final String OS_ARCH = "os.arch";
	private static final String OS_NAME = "os.name";

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
		super.start(context);
		loadEmbeddedFirebirdLibraries();
		loadEmbeddedPandocLibraries();
		IconCache.setIcons(new CIMToolIconCache(context.getBundle(), "/icons/"));
		cache = new Cache();
		settings = new Settings();
		plugin = this;
	}
	
	private void loadEmbeddedFirebirdLibraries() {
		try {
			// Build the full path to the DLLs
			String platform = getPlatformFolder(); // Determine the platform-specific subfolder
			IPath stateLocation = getStateLocation(); 
			IPath stateLocationNativePlatformPath = stateLocation.append("native").append(platform).append("firebird");
			File stateLocationNativePlatformDir = stateLocationNativePlatformPath.toFile();
			
			File fbClientLib = new File(stateLocationNativePlatformDir, "fbembed.dll");

			if (!fbClientLib.exists()) {
				// Locate the ZIP file within the bundle...
				Bundle cimtooleBundle = Platform.getBundle(CIMToolPlugin.PLUGIN_ID);
				URL fbClientLibURL = cimtooleBundle.getEntry("native/Firebird_" + platform + ".zip");
				URL fbClientLibFileURL = FileLocator.toFileURL(fbClientLibURL);
				
				// Create input stream to the ZIP file in the bundle to unzip to the file system.
				InputStream zipFileInputStream = fbClientLibFileURL.openStream();
				unzip(zipFileInputStream, stateLocationNativePlatformDir.getAbsolutePath());
			}
			
			// Finally, append to the JNA library path...
			appendJnaLibraryPath(stateLocationNativePlatformDir.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}
	
	private void loadEmbeddedPandocLibraries() {
		try {
			// Build the full path to the DLLs
			String platform = getPlatformFolder(); // Determine the platform-specific subfolder
			IPath stateLocation = getStateLocation(); 
			IPath stateLocationNativePlatformPath = stateLocation.append("native").append(platform).append("pandoc");
			File stateLocationNativePlatformDir = stateLocationNativePlatformPath.toFile();
			
			PandocPathResolver.setEmbeddedPandocRootDir(stateLocationNativePlatformDir);
			
			File pandocExe = PandocPathResolver.getPandocExecutablePath();

			if (!pandocExe.exists()) {
				// Locate the ZIP file within the bundle...
				Bundle cimtooleBundle = Platform.getBundle(CIMToolPlugin.PLUGIN_ID);
				URL pandocExeURL = cimtooleBundle.getEntry("native/Pandoc_" + platform + ".zip");
				URL pandocClientExeFileURL = FileLocator.toFileURL(pandocExeURL);
				
				// Create input stream to the ZIP file in the bundle to unzip to the file system.
				InputStream zipFileInputStream = pandocClientExeFileURL.openStream();
				unzip(zipFileInputStream, stateLocationNativePlatformDir.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}
	
    private void appendJnaLibraryPath(String newPath) {
        // Retrieve the current value of jna.library.path
        String existingPaths = System.getProperty("jna.library.path");
        
        // If there's an existing path, append the new one; otherwise, use the new path as is
        if (existingPaths != null && !existingPaths.isEmpty()) {
            System.setProperty("jna.library.path", existingPaths + System.getProperty("path.separator") + newPath);
        } else {
            System.setProperty("jna.library.path", newPath);
        }

        System.out.println("New 'jna.library.path':  " + System.getProperty("jna.library.path"));
    }
	
	private void unzip(InputStream zipFileInputStream, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdirs();
		}
		ZipInputStream zipIn = new ZipInputStream(zipFileInputStream);
		ZipEntry entry = zipIn.getNextEntry();
		while (entry != null) {
			String filePath = destDirectory + File.separatorChar + entry.getName();
			if (!entry.isDirectory()) {
				extractFile(zipIn, filePath);
			} else {
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}

	private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[4096];
		int read;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
	
	private String getPlatformFolder() {
		String os = System.getProperty(OS_NAME).toLowerCase();
		String arch = System.getProperty(OS_ARCH).toLowerCase();
		if (os.contains("win")) {
			return arch.contains("64") ? "win32-x86_64" : "win32-x86";
		} else if (os.contains("mac")) {
			return ("amd64".equals(arch) || "x86_64".equals(arch)) ? "mac-x86_64" : "mac-arm64";
		} else {
			throw new IllegalStateException("Unsupported platform: " + os);
		}
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
