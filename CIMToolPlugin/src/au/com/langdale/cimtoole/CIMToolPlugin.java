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
import java.io.PrintStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
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
import org.slf4j.bridge.SLF4JBridgeHandler;

import au.com.langdale.cimtoole.pandoc.PandocPathResolver;
import au.com.langdale.cimtoole.project.BuilderPreferences;
import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.cimtoole.project.GlobalPreferencesSynchronizer;
import au.com.langdale.cimtoole.project.Settings;
import au.com.langdale.colors.util.NodeTraits;
import au.com.langdale.ui.util.GeneralIconCache;
import au.com.langdale.ui.util.IconCache;

/**
 * The activator class for the CIMTool plugin. It manages the lifetime of the
 * model cache.
 */
public class CIMToolPlugin extends AbstractUIPlugin {

	private static final String OS_ARCH = "os.arch";
	private static final String OS_NAME = "os.name";

	// The CIMTool Product plug-in ID
	public static final String PLUGIN_CIMTOOL_PRODUCT = "au.com.langdale.cimtool.product";
	
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

	// the builder preferences database
	private static BuilderPreferences builderPreferences;

	// the global preferences synchronizer
	private static GlobalPreferencesSynchronizer globalPreferencesSynchronizer;

	/**
	 * The constructor
	 */
	public CIMToolPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		configureLogging();
		extractLoggingProperties();
		loadEmbeddedPandocLibraries();
		IconCache.setIcons(new CIMToolIconCache(context.getBundle(), "/icons/"));
		cache = new Cache();
		settings = new Settings();
		builderPreferences = new BuilderPreferences();
		globalPreferencesSynchronizer = GlobalPreferencesSynchronizer.register();
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		GlobalPreferencesSynchronizer.unregister(globalPreferencesSynchronizer);
		globalPreferencesSynchronizer = null;
		plugin = null;
		super.stop(context);
	}

	/**
	 * Configures the unified logging pipeline for the CIMTool application.
	 *
	 * JUL → SLF4J bridge:
	 *   SLF4JBridgeHandler is installed at startup to redirect all java.util.logging
	 *   (JUL) events into the SLF4J pipeline. Combined with the
	 *   io.ucaiug.slf4j.logback.binding fragment bundle which wires SLF4J to
	 *   Logback, this means all JUL events — including those from JDK internals,
	 *   JNA, Google Guice, and CIMTool's own code — flow through Logback and are
	 *   captured in logs/cimtool.log. The FileHandler in logging.properties is no
	 *   longer used; logging.properties is retained with handlers= empty and
	 *   .level=ALL to ensure JUL does not pre-filter events before they reach the
	 *   bridge.
	 *
	 * System.err filtering:
	 *   System.err is always filtered (in both development and production mode) to
	 *   suppress the spurious multi-line UCanAccess reserved-word warning. This is
	 *   harmless driver noise that originates from a direct System.err.println call
	 *   inside the JDBC driver and cannot be suppressed via logging configuration.
	 *   An AtomicBoolean guards the suppress flag because System.err may be written
	 *   concurrently from multiple threads.
	 *
	 * Production stream capture:
	 *   In production mode (!Platform.inDevelopmentMode()), System.out and
	 *   System.err are redirected through separate JUL loggers:
	 *   "CIMTool.console.out" (INFO) for standard output and
	 *   "CIMTool.console.err" (WARNING) for standard error. Both loggers are
	 *   children of "CIMTool.console" in the logger hierarchy and are bridged to
	 *   SLF4J by SLF4JBridgeHandler, so all console output is captured in
	 *   logs/cimtool.log via Logback with the correct severity level. In
	 *   development mode the streams are left untouched so output remains visible
	 *   in the Eclipse Console view.
	 */
	private void configureLogging() {
		// Install the JUL-to-SLF4J bridge so all java.util.logging events are
		// redirected into the SLF4J → Logback pipeline. Must be called before
		// any JUL loggers are used. removeHandlersForRootLogger() ensures no
		// duplicate output occurs if any JUL handlers were registered previously.
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		// Suppress the multi-line UCanAccess reserved-word warning on System.err.
		// AtomicBoolean is used for thread safety since System.err may be written
		// concurrently from multiple threads.
		final String UCANACCESS_WARNING = "You shouldn't use";
		final AtomicBoolean suppress = new AtomicBoolean(false);
		final PrintStream filteredErr = new PrintStream(System.err) {
			@Override
			public void println(String line) {
				if (line != null && line.contains(UCANACCESS_WARNING)) {
					suppress.set(true);
					return;
				}
				suppress.set(false);
				super.println(line);
			}

			@Override
			public void print(String s) {
				if (suppress.get())
					return;
				if (s != null && s.contains(UCANACCESS_WARNING)) {
					suppress.set(true);
					return;
				}
				super.print(s);
			}
		};
		System.setErr(filteredErr);

		// In production, route both streams through JUL loggers so all console
		// output is captured via the bridge into logs/cimtool.log via Logback.
		if (!Platform.inDevelopmentMode()) {
			final Logger stdoutLogger = Logger.getLogger("CIMTool.console.out");
			final Logger stderrLogger = Logger.getLogger("CIMTool.console.err");

			System.setOut(new PrintStream(System.out) {
				private final StringBuilder buffer = new StringBuilder();

				@Override
				public void println(String line) {
					if (line != null)
						stdoutLogger.info(line);
				}

				@Override
				public void print(String s) {
					if (s == null)
						return;
					buffer.append(s);
					int newline;
					while ((newline = buffer.indexOf("\n")) >= 0) {
						String line = buffer.substring(0, newline).replace("\r", "");
						if (!line.isEmpty())
							stdoutLogger.info(line);
						buffer.delete(0, newline + 1);
					}
				}
			});

			System.setErr(new PrintStream(filteredErr) {
				@Override
				public void println(String line) {
					if (line != null && !line.contains(UCANACCESS_WARNING))
						stderrLogger.warning(line);
				}
			});
		}
	}

	/**
	 * Extracts logging.properties and logback.xml to the installation root on first
	 * run. logging.properties is loaded by the JVM via the
	 * -Djava.util.logging.config.file=./logging.properties JVM argument in
	 * CIMTool.ini. logback.xml is loaded by Logback via the
	 * -Dlogback.configurationFile=./logback.xml JVM argument in CIMTool.ini.
	 * Both files are bundled inside the CIMToolProduct plugin and extracted once
	 * so end users can edit them without rebuilding.
	 */
	private void extractLoggingProperties() {
		try {
			URL installLocationURL = Platform.getInstallLocation().getURL();
			File installDir = new File(installLocationURL.toURI());
			File loggingPropertiesFile = new File(installDir, "logging.properties");

			// Ensure the logs/ directory exists - FileHandler will fail silently
			// if it cannot find the directory specified in the pattern.
			File logsDir = new File(installDir, "logs");
			if (!logsDir.exists()) {
				logsDir.mkdirs();
			}

			Bundle productBundle = Platform.getBundle(PLUGIN_CIMTOOL_PRODUCT);

			if (!loggingPropertiesFile.exists()) {
				// logging.properties is bundled in the CIMToolProduct plugin...
				if (productBundle != null) {
					URL bundledURL = productBundle.getEntry("logging.properties");
					if (bundledURL != null) {
						try (InputStream in = bundledURL.openStream();
							 FileOutputStream out = new FileOutputStream(loggingPropertiesFile)) {
							byte[] buffer = new byte[4096];
							int read;
							while ((read = in.read(buffer)) != -1) {
								out.write(buffer, 0, read);
							}
						}
					}
				}
			}

			// Extract logback.xml alongside logging.properties on first run.
			File logbackXmlFile = new File(installDir, "logback.xml");
			if (!logbackXmlFile.exists()) {
				if (productBundle != null) {
					URL bundledLogbackURL = productBundle.getEntry("logback.xml");
					if (bundledLogbackURL != null) {
						try (InputStream in = bundledLogbackURL.openStream();
							 FileOutputStream out = new FileOutputStream(logbackXmlFile)) {
							byte[] buffer = new byte[4096];
							int read;
							while ((read = in.read(buffer)) != -1) {
								out.write(buffer, 0, read);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Non-fatal - programmatic suppression remains active as fallback.
			e.printStackTrace(System.err);
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

				// Create input stream to the ZIP file in the bundle to unzip to the file
				// system.
				InputStream zipFileInputStream = pandocClientExeFileURL.openStream();
				unzip(zipFileInputStream, stateLocationNativePlatformDir.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convenience method to append a new JNI library (DLL) to the JNA path.
	 */
	private void appendJnaLibraryPath(String newPath) {
		// Retrieve the current value of jna.library.path
		String existingPaths = System.getProperty("jna.library.path");

		// If there's an existing path, append the new one; otherwise, use the new path
		// as is
		if (existingPaths != null && !existingPaths.isBlank()) {
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

	public static BuilderPreferences getBuilderPreferences() {
		return builderPreferences;
	}

}
