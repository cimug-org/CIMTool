package au.com.langdale.cimtoole.registries;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.typesafe.config.parser.ConfigDocument;
import com.typesafe.config.parser.ConfigDocumentFactory;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.JSONSchemaBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;

/**
 * Utility class to handle specific functionality centered around the
 * configuration of profile buildlets of various types.
 */
public final class ProfileBuildletConfigUtils {

	private static final String KEY_BUILDERS = "builders";
	private static final String KEY_DATETIME = "datetime";
	private static final String KEY_TYPE = "type";
	private static final String KEY_EXT = "ext";

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
							Files.copy(is, dataAreaBuilderConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
							Config buildersConfig = ConfigFactory.parseFile(dataAreaBuilderConfigFile);

							// Retrieve all existing custom builder configuration entries under the
							// "builders" key...
							List<? extends ConfigObject> builders = buildersConfig.getObjectList(KEY_BUILDERS);

							// Loop through each builder contained within the .builders (i.e.
							// builders.conf) file...
							for (ConfigObject builder : builders) {

								String builderKey = builder.keySet().iterator().next();
								String xslFileName = builderKey + XSL;

								URL xslUrl = cimtooleBundle.getEntry(CONFIG_DIR + "/" + xslFileName);
								URL xslFileUrl = FileLocator.toFileURL(xslUrl);
								//
								File dataAreaXslFile = new File(dataAreaDir, xslFileName);
								dataAreaXslFile.createNewFile();
								//
								is = null;
								try {
									is = xslFileUrl.openStream();
									Files.copy(is, dataAreaXslFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
			// We log the exception, fail gracefully and proceed...
			e.printStackTrace();
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
					e.printStackTrace();
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
			// Do nothing. Return is = null;
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

					Config buildersConfig = ConfigFactory.parseFile(dataAreaBuilderConfigFile);

					// Retrieve all existing custom builder configuration entries under the
					// "builders" key...
					List<? extends ConfigObject> builders = buildersConfig.getObjectList(KEY_BUILDERS);

					// Loop through each builder contained within the builders.conf file
					for (ConfigObject builder : builders) {

						String builderKey = builder.keySet().iterator().next();
						ConfigObject configObject = (ConfigObject) builder.get(builderKey);

						// Obtain the datetime of TransformBuildlet to be created...
						String datetimeValue = (String) configObject.get(KEY_DATETIME).unwrapped();
						ZonedDateTime datetime = ZonedDateTime.parse(datetimeValue);
						
						// Obtain the type of TransformBuildlet to be created...
						String builderType = (String) configObject.get(KEY_TYPE).unwrapped();
						TransformType type = TransformType.valueOf(builderType);

						// Obtain the extension of the file that will be generated by the builder...
						String ext = (String) configObject.get(KEY_EXT).unwrapped();

						TransformBuildlet buildlet = null;
						try {
							switch (type) {
							case JSON:
								buildlet = new JSONSchemaBuildlet(builderKey, ext, datetime);
								break;
							case TEXT:
								buildlet = new TextBuildlet(builderKey, ext, datetime);
								break;
							case XSD:
								buildlet = new XSDBuildlet(builderKey, ext, datetime);
								break;
							case TRANSFORM:
								buildlet = new TransformBuildlet(builderKey, ext, datetime);
								break;
							}
							customBuildlets.put(builderKey, buildlet);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			// We log the exception, fail gracefully and proceed...
			e.printStackTrace();
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

				// We ensure that the configuration location is initialized. It most cases it
				// will have already been created.
				if (!dataAreaBuilderConfigFile.exists())
					initCustomBuildersConfiguration();

				if (dataAreaBuilderConfigFile.exists()) {
					ConfigDocument buildersConfigDocument = ConfigDocumentFactory.parseFile(dataAreaBuilderConfigFile);

					Config buildersConfig = ConfigFactory.parseFile(dataAreaBuilderConfigFile);

					// Retrieve all existing custom builder configuration entries under the
					// "builders" key...
					List<? extends ConfigObject> builders = buildersConfig.getObjectList(KEY_BUILDERS);

					List<ConfigObject> list = new ArrayList<ConfigObject>();

					// Add the existing builder config entries to the list...
					for (ConfigObject builder : builders) {
						// This logic excludes the builder from the new list to be resaved...
						if (!builderKey.equals(builder.keySet().iterator().next())) {
							list.add(builder);
						}
					}

					ConfigDocument newConfigDocument = buildersConfigDocument.withValue(KEY_BUILDERS,
							ConfigValueFactory.fromIterable(list));

					InputStream is = null;
					try {
						is = new ByteArrayInputStream(newConfigDocument.render().getBytes());
						Files.copy(is, dataAreaBuilderConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

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
			e.printStackTrace();
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

					boolean isExistingBuildlet = false;

					ConfigDocument originalConfigDocument = ConfigDocumentFactory.parseFile(dataAreaBuilderConfigFile,
							ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF));

					String builderKey = buildlet.getStyle();

					TransformType type = TransformType.toTransformType(buildlet);

					ConfigDocument newConfigDocument = null;

					Config buildersConfig = ConfigFactory.parseFile(dataAreaBuilderConfigFile);

					// Retrieve all existing custom builder configuration entries under the
					// "builders" key...
					List<? extends ConfigObject> builders = buildersConfig.getObjectList(KEY_BUILDERS);

					// Create a map to config objects
					Map<String, ConfigValue> values = new HashMap<String, ConfigValue>();

					values.put(KEY_DATETIME, ConfigValueFactory.fromAnyRef(buildlet.getDateTimeCreated().toString()));
					values.put(KEY_TYPE, ConfigValueFactory.fromAnyRef(type.name()));
					values.put(KEY_EXT, ConfigValueFactory.fromAnyRef(buildlet.getFileExt()));

					ConfigValue configValues = ConfigValueFactory.fromAnyRef(ConfigValueFactory.fromAnyRef(values));

					Map<String, ConfigValue> builderConfigEntry = new HashMap<String, ConfigValue>();
					builderConfigEntry.put(builderKey, configValues);
					ConfigObject configObject = ConfigValueFactory.fromMap(builderConfigEntry);

					List<ConfigObject> list = new ArrayList<ConfigObject>();

					// Add the existing builder config entries to the list...unless we find a
					// builderKey matching the one to be added. In that case with "overwrite" it by
					// NOT add the existing entry to the new list...
					for (ConfigObject co : builders) {
						if (!builderKey.equals(co.keySet().iterator().next())) {
							list.add(co);
						} else {
							isExistingBuildlet = true;
						}
					}

					// Add the new builder config entries to the list...
					list.add(configObject);

					newConfigDocument = originalConfigDocument.withValue(KEY_BUILDERS,
							ConfigValueFactory.fromIterable(list));

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
								Files.copy(is, destinationXslFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
						is = new ByteArrayInputStream(newConfigDocument.render().getBytes());
						Files.copy(is, dataAreaBuilderConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
