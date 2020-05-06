package au.com.langdale.cimtoole.registries;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.joda.time.DateTime;
import org.osgi.framework.Bundle;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.registries.config.JodaDateTimeDeserializer;
import au.com.langdale.cimtoole.registries.config.JodaDateTimeSerializer;
import au.com.langdale.cimtoole.registries.config.TransformBuildletDeserializer;
import au.com.langdale.cimtoole.registries.config.TransformBuildletSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Utility class to handle specific functionality centered around the configuration
 * of profile buildlets of various types.
 */
public final class ProfileBuildletConfigUtils {

	private static Type typeOfHashMap = new TypeToken<Map<String, TransformBuildlet>>() {
	}.getType();

	private static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new JodaDateTimeDeserializer())
			.registerTypeAdapter(DateTime.class, new JodaDateTimeSerializer())
			.registerTypeAdapter(TransformBuildlet.class, new TransformBuildletDeserializer<TransformBuildlet>())
			.registerTypeAdapter(TransformBuildlet.class, new TransformBuildletSerializer<TransformBuildlet>())
			.setPrettyPrinting().create();

	private static final String XSL = ".xsl";

	private static final String CONFIG_DEFAULTS_FILE = "builders.json";
	private static final String CONFIG_DIR = "builders";
	private static final String CONFIG_FILE = ".builders";

	private ProfileBuildletConfigUtils() {
	}

	private static void initCustomBuildersConfiguration() {
		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				if (!dataAreaBuilderConfigFile.exists()) {

					Bundle cimtooleBundle = Platform.getBundle(CIMToolPlugin.PLUGIN_ID);
					URL buildersConfURL = cimtooleBundle.getEntry(CONFIG_DIR + "/" + CONFIG_DEFAULTS_FILE);
					URL buildersConfFileURL = FileLocator.toFileURL(buildersConfURL);

					if (!dataAreaDir.exists())
						dataAreaDir.mkdirs();

					if (dataAreaDir.exists() && !dataAreaBuilderConfigFile.exists()) {
						dataAreaBuilderConfigFile.createNewFile();

						InputStream is = null;

						try {
							is = buildersConfFileURL.openStream();
							IOUtils.copy(is, new FileOutputStream(dataAreaBuilderConfigFile));
						} finally {
							if (is != null) {
								try {
									is.close();
								} catch (Exception e) {
									// Do nothing
								}
							}
						}

						if (dataAreaBuilderConfigFile.exists()) {

							String json = new String(
									IOUtils.toByteArray(new FileInputStream(dataAreaBuilderConfigFile)));
							Map<String, TransformBuildlet> builders = gson.fromJson(json, typeOfHashMap);

							for (TransformBuildlet builder : builders.values()) {

								String xslFileName = builder.getStyle() + XSL;

								URL xslUrl = cimtooleBundle.getEntry(CONFIG_DIR + "/" + xslFileName);
								URL xslFileUrl = FileLocator.toFileURL(xslUrl);
								File dataAreaXslFile = new File(dataAreaDir, xslFileName);
								dataAreaXslFile.createNewFile();

								is = null;
								try {
									is = xslFileUrl.openStream();
									IOUtils.copy(is, new FileOutputStream(dataAreaXslFile));
								} finally {
									if (is != null) {
										try {
											is.close();
										} catch (Exception e) {
											// Do nothing
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}

	public static Map<String, ProfileBuildlet> getExtensionBuildlets() {
		Map<String, ProfileBuildlet> extensionBuildlets = new TreeMap<String, ProfileBuildlet>();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = registry.getExtensionPoint(ProfileBuildletRegistry.BUILDLET_REGISTRY_ID);

		IExtension[] pExts = extPoint.getExtensions();
		for (IExtension p : pExts) {
			for (IConfigurationElement el : p.getConfigurationElements()) {
				try {
					Object obj = el.createExecutableExtension("class");
					if (obj instanceof ProfileBuildlet) {
						ProfileBuildlet buildlet = (ProfileBuildlet) obj;
						String id = el.getAttribute("id");
						if (buildlet != null && id != null) {
							extensionBuildlets.put(id, buildlet);
						}
					}
				} catch (CoreException e) {
					e.printStackTrace(System.err);
				}
			}
		}

		return extensionBuildlets;
	}

	public static boolean hasBuildlet(String builderKey) {
		return (builderKey != null ? getTransformBuildlets().containsKey(builderKey) : false);
	}

	public static TransformBuildlet getTransformBuildlet(String builderKey) {
		if (builderKey != null) {
			Map<String, TransformBuildlet> buildlets = getTransformBuildlets();
			if (buildlets.containsKey(builderKey))
				return buildlets.get(builderKey);
		}
		return null;
	}

	public static InputStream getTransformBuildletInputStream(String builderKey) {
		InputStream is = null;

		if (builderKey == null)
			return is;

		try {
			if (builderKey != null && Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				if (!dataAreaBuilderConfigFile.exists())
					initCustomBuildersConfiguration();

				File xslFile = new File(dataAreaDir, builderKey + ".xsl");

				if (xslFile.exists() && xslFile.canRead()) {
					is = new FileInputStream(xslFile);
				}
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Unable to create an input stream for the XSLT transform file: " + builderKey
					+ ".xsl", e);
		}

		return is;
	}

	public static Map<String, TransformBuildlet> getTransformBuildlets() {
		Map<String, TransformBuildlet> customBuildlets = new TreeMap<String, TransformBuildlet>();

		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				if (!dataAreaBuilderConfigFile.exists())
					initCustomBuildersConfiguration();

				if (dataAreaBuilderConfigFile.exists()) {
					String json = new String(IOUtils.toByteArray(new FileInputStream(dataAreaBuilderConfigFile)));
					customBuildlets = gson.fromJson(json, typeOfHashMap);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Unable to load the XSLT transform builders configuration.", e);
		}

		return customBuildlets;
	}

	public static boolean deleteTransformBuilderConfigEntry(String builderKey) {
		boolean successful = false;

		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				// We ensure that the configuration location is initialized. It
				// most cases it
				// will have already been created.
				if (!dataAreaBuilderConfigFile.exists())
					initCustomBuildersConfiguration();

				if (dataAreaBuilderConfigFile.exists()) {

					String json = new String(IOUtils.toByteArray(new FileInputStream(dataAreaBuilderConfigFile)));
					Map<String, TransformBuildlet> builders = gson.fromJson(json, typeOfHashMap);

					builders.remove(builderKey);

					InputStream is = null;
					try {
						is = new ByteArrayInputStream(gson.toJson(builders).getBytes());
						IOUtils.copy(is, new FileOutputStream(dataAreaBuilderConfigFile));

						successful = true;

						// Finally, we remove the XSLT file...
						File xslFile = new File(dataAreaDir, builderKey + ".xsl");
						xslFile.delete();
					} finally {
						if (is != null) {
							try {
								is.close();
							} catch (Exception e) {
								// Do nothing
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// We log the exception, fail gracefully and proceed...
			e.printStackTrace(System.err);
			throw new RuntimeException("Unable to delete the custom XSLT builder: " + builderKey + ".xsl", e);
		}

		return successful;
	}

	public static void updateTransformBuilderConfigEntry(TransformBuildlet buildlet) {
		persistTransformBuilderConfigEntry(buildlet, null);
	}

	public static void addTransformBuilderConfigEntry(TransformBuildlet buildlet, File xslFile) {
		persistTransformBuilderConfigEntry(buildlet, xslFile);
	}

	private static void persistTransformBuilderConfigEntry(TransformBuildlet buildlet, File xslFile) {
		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				if (!dataAreaBuilderConfigFile.exists())
					initCustomBuildersConfiguration();

				if (dataAreaBuilderConfigFile.exists()) {

					String json = new String(IOUtils.toByteArray(new FileInputStream(dataAreaBuilderConfigFile)));
					Map<String, TransformBuildlet> builders = gson.fromJson(json, typeOfHashMap);

					boolean isExistingBuildlet = builders.containsKey(buildlet.getStyle());

					builders.put(buildlet.getStyle(), buildlet);

					if ((!isExistingBuildlet && xslFile == null) || (xslFile != null && !xslFile.exists())) {
						throw new IllegalArgumentException((xslFile == null ? "No XSLT file was provided."
								: "The XSLT file specified does not exist on the file system ["
										+ xslFile.getAbsolutePath() + "]"));
					} else {
						if (xslFile != null) {
							InputStream is = null;
							try {
								is = new FileInputStream(xslFile);
								File destinationXslFile = new File(dataAreaDir, xslFile.getName());
								IOUtils.copy(is, new FileOutputStream(destinationXslFile));
							} finally {
								if (is != null) {
									try {
										is.close();
									} catch (Exception e) {
										// Do nothing
									}
								}
							}
						}
					}

					InputStream is = null;
					try {
						is = new ByteArrayInputStream(gson.toJson(builders).getBytes());
						IOUtils.copy(is, new FileOutputStream(dataAreaBuilderConfigFile));
					} finally {
						if (is != null) {
							try {
								is.close();
							} catch (Exception e) {
								// Do nothing
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// We log the exception, fail gracefully and proceed...
			e.printStackTrace();
			throw new RuntimeException("Unable to register the XSLT builder: " + xslFile.getName(), e);
		}

	}

}
