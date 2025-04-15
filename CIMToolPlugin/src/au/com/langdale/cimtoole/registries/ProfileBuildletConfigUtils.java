package au.com.langdale.cimtoole.registries;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Utility class to handle specific functionality centered around the
 * configuration of profile buildlets of various types.
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
	private static final String INCLUDES_DIR = "includes";
	private static final Pattern INCLUDE_PATTERN = Pattern.compile("(<xsl:include\\s*href=\")([^\"]+)(\"\\s*/>)");

	private ProfileBuildletConfigUtils() {
	}

	private static void initBuildersConfiguration() {
		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				if (!dataAreaDir.exists())
					dataAreaDir.mkdirs();

				File dataAreaIncludesDir = new File(dataAreaDir.getPath(), INCLUDES_DIR);
				if (!dataAreaIncludesDir.exists())
					dataAreaIncludesDir.mkdirs();

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				if (!dataAreaBuilderConfigFile.exists()) {
					dataAreaBuilderConfigFile.createNewFile();
					
					Bundle cimtooleBundle = Platform.getBundle(CIMToolPlugin.PLUGIN_ID);
					URL buildersConfURL = cimtooleBundle.getEntry(CONFIG_DIR + "/" + CONFIG_DEFAULTS_FILE);
					URL buildersConfFileURL = FileLocator.toFileURL(buildersConfURL);
					
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

						String json = new String(IOUtils.toByteArray(new FileInputStream(dataAreaBuilderConfigFile)));
						Map<String, TransformBuildlet> buildlets = gson.fromJson(json, typeOfHashMap);

						for (TransformBuildlet buildlet : buildlets.values()) {

							String xslFileName = buildlet.getStyle() + XSL;

							URL xslUrl = cimtooleBundle.getEntry(CONFIG_DIR + "/" + xslFileName);
							URL xslFileUrl = FileLocator.toFileURL(xslUrl);
							File dataAreaXslFile = new File(dataAreaDir, xslFileName);
							dataAreaXslFile.createNewFile();
							
							File dataAreaXslIncludesFile = null;
							URL xslIncludesFileURL = null;
							if (buildlet.getIncludesFile() != null && !"".equals(buildlet.getIncludesFile())) {
								URL xslIncludesUrl = cimtooleBundle.getEntry(CONFIG_DIR + "/" + INCLUDES_DIR + "/" + buildlet.getIncludesFile());
								xslIncludesFileURL = FileLocator.toFileURL(xslIncludesUrl);
								dataAreaXslIncludesFile = new File(dataAreaIncludesDir, buildlet.getIncludesFile());
								dataAreaXslIncludesFile.createNewFile();
								
								try {
									is = xslIncludesFileURL.openStream();
									IOUtils.copy(is, new FileOutputStream(dataAreaXslIncludesFile));
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
							//
							try {
								String mainXslFile = new String(Files.readAllBytes(Paths.get(xslFileUrl.toURI())), StandardCharsets.UTF_8);
								// Check if the builder has an includes file...
								if (dataAreaXslIncludesFile != null) {
									String xslIncludesFilePath = getIncludesFileAbsolutePath(buildlet);
									// Replace the placeholder...
									Matcher matcher = INCLUDE_PATTERN.matcher(mainXslFile);
									mainXslFile = matcher.replaceAll("$1" + xslIncludesFilePath + "$3");
								} 
								is = new ByteArrayInputStream(mainXslFile.getBytes(StandardCharsets.UTF_8));
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
	
	public static TransformBuildlet getTransformBuildletForExtension(String extension) {
		if (extension != null) {
			for (TransformBuildlet buildlet : getTransformBuildlets().values()) {
				if (buildlet.getFileExt().equals(extension))
					return buildlet;
			}
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
					initBuildersConfiguration();

				File xslFile = new File(dataAreaDir, builderKey + ".xsl");

				if (xslFile.exists() && xslFile.canRead()) {
					// Check if the builder has an includes file...
					TransformBuildlet buildlet = getTransformBuildlet(builderKey);
					if (buildlet.getIncludesFile() != null && !"".equals(buildlet.getIncludesFile())) {
						String xslIncludesFilePath = getIncludesFileAbsolutePath(buildlet);
						String mainXslFile = new String(Files.readAllBytes(Paths.get(xslFile.toURI())),
								StandardCharsets.UTF_8);
						//
						// Replace the placeholder...
				        Matcher matcher = INCLUDE_PATTERN.matcher(mainXslFile);
				        mainXslFile = matcher.replaceAll("$1" + xslIncludesFilePath + "$3");
						//
						is = new ByteArrayInputStream(mainXslFile.getBytes(StandardCharsets.UTF_8));
					} else {
						is = new FileInputStream(xslFile);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new RuntimeException(
					"Unable to create an input stream for the XSLT transform file: " + builderKey + ".xsl", e);
		}

		return is;
	}

	public static Map<String, TransformBuildlet> getTransformBuildlets() {
		Map<String, TransformBuildlet> customBuildlets = new HashMap<String, TransformBuildlet>();

		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				if (!dataAreaBuilderConfigFile.exists())
					initBuildersConfiguration();

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
	
	/**
	 * Method that will return an XSLT compliant absolute file path to the include
	 * file if one exists for the builder passed in. 
	 * 
	 * For example, the method takes the following standard absolute windows path:
	 * 
	 * "C:\\MyXSLT Files\\includes\\my-includes.xsl"
	 * 
	 * ...and converts it the required XSLT compliant format that is to be used within any <xsl:include href="..."/>:
	 * 
	 * "file:///C:/MyXSLT Files/workspace-release-2.3.0/includes/rdfs-cimstudio-namespaces.xsl"
	 * 
	 * @param buildlet
	 * @return
	 */
	protected static String getIncludesFileAbsolutePath(TransformBuildlet buildlet) {
		String absoluteIncludesFilePath = null;
		try {
			if (buildlet == null)
				throw new IllegalArgumentException("Invalid transform builder parameter passed. May not be null.");

			if (buildlet.getIncludesFile() == null)
				return null;

			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				File dataAreaBuilderIncludesDir = new File(dataAreaDir.getPath(), INCLUDES_DIR);

				// We ensure that the configuration location is initialized.
				// In most cases it will have already been created.
				if (!dataAreaBuilderConfigFile.exists())
					initBuildersConfiguration();

				// We ensure that the XSLT includes directory exists. In
				// most cases it will have already been created.
				if (!dataAreaBuilderIncludesDir.exists())
					initBuildersConfiguration();

				File xslIncludesFile = new File(dataAreaBuilderIncludesDir, buildlet.getIncludesFile());
				absoluteIncludesFilePath = xslIncludesFile.getAbsolutePath();
				// We must replace spaces in the file path with encoded spaces. This 
				// is required in order to ensure the include file is read correctly.
				absoluteIncludesFilePath = absoluteIncludesFilePath.replace(" ", "%20");
				absoluteIncludesFilePath = "file:///" + absoluteIncludesFilePath.replaceAll("\\\\", "/");
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Unable to determine the includes file absolute file path.", e);
		}
		return absoluteIncludesFilePath;
	}

	public static boolean deleteTransformBuilderConfigEntry(TransformBuildlet buildlet) {
		boolean successful = false;

		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				File dataAreaBuilderIncludesDir = new File(dataAreaDir.getPath(), INCLUDES_DIR);

				// We ensure that the configuration location is initialized.
				// In most cases it will have already been created.
				if (!dataAreaBuilderConfigFile.exists())
					initBuildersConfiguration();

				// We ensure that the XSLT includes directory exists. In
				// most cases it will have already been created.
				if (!dataAreaBuilderIncludesDir.exists())
					initBuildersConfiguration();

				if (dataAreaBuilderConfigFile.exists()) {

					String json = new String(IOUtils.toByteArray(new FileInputStream(dataAreaBuilderConfigFile)));
					Map<String, TransformBuildlet> builders = gson.fromJson(json, typeOfHashMap);

					builders.remove(buildlet.getStyle());

					InputStream is = null;
					try {
						String newJson = gson.toJson(builders, typeOfHashMap);
						is = new ByteArrayInputStream(newJson.getBytes());
						IOUtils.copy(is, new FileOutputStream(dataAreaBuilderConfigFile));

						successful = true;

						// Finally, we remove the XSLT file...
						File xslFile = new File(dataAreaDir, buildlet.getStyle() + ".xsl");
						xslFile.delete();

						// ...and any XSL includes file if relevant.
						if (buildlet.getIncludesFile() != null && buildlet.getIncludesFile().length() > 0) {
							File xslIncludesFile = new File(dataAreaBuilderIncludesDir, buildlet.getIncludesFile());
							if (xslIncludesFile.exists())
								xslIncludesFile.delete();
						}
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
			throw new RuntimeException("Unable to delete the custom XSLT builder: " + buildlet.getStyle() + ".xsl", e);
		}

		return successful;
	}

	public static void updateTransformBuilderConfigEntry(TransformBuildlet buildlet) {
		persistTransformBuilderConfigEntry(buildlet, null, null);
	}

	public static void addTransformBuilderConfigEntry(TransformBuildlet buildlet, File xslFile, File xslIncludesFile) {
		persistTransformBuilderConfigEntry(buildlet, xslFile, xslIncludesFile);
	}

	private static void persistTransformBuilderConfigEntry(TransformBuildlet buildlet, File xslFile,
			File xslIncludesFile) {
		try {
			if (Platform.isRunning()) {
				Location configLocation = Platform.getConfigurationLocation();
				URL dataArea = configLocation.getDataArea(CIMToolPlugin.PLUGIN_ID);

				File dataAreaDir = new File(dataArea.getPath());
				dataAreaDir = new File(dataAreaDir.getPath(), CONFIG_DIR);

				File dataAreaBuilderConfigFile = new File(dataAreaDir, CONFIG_FILE);

				// We ensure that the configuration location is initialized.
				// In most cases it will have already been created.
				if (!dataAreaBuilderConfigFile.exists())
					initBuildersConfiguration();

				File dataAreaBuilderIncludesDir = new File(dataAreaDir, INCLUDES_DIR);

				// We ensure that the XSLT includes directory exists. In
				// most cases it will have already been created.
				if (!dataAreaBuilderIncludesDir.exists())
					initBuildersConfiguration();

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
						if (xslIncludesFile != null && xslIncludesFile.exists()) {
							String xslIncludesFilePath = getIncludesFileAbsolutePath(buildlet);
							String mainXslFile = new String(Files.readAllBytes(Paths.get(xslFile.toURI())),
									StandardCharsets.UTF_8);
							//
							// Replace the placeholder...
					        Matcher matcher = INCLUDE_PATTERN.matcher(mainXslFile);
					        mainXslFile = matcher.replaceAll("$1" + xslIncludesFilePath + "$3");
							//
							File destinationXslFile = new File(dataAreaDir, xslFile.getName());
							Files.write(Paths.get(destinationXslFile.toURI()),
									mainXslFile.getBytes(StandardCharsets.UTF_8));
						} else if (xslFile != null){
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

					if (xslIncludesFile != null) {
						if (!xslIncludesFile.exists()) {
							throw new IllegalArgumentException(
									"The specified corresponding XSLT includes file does not exist on the file system ["
											+ xslIncludesFile.getAbsolutePath() + "]");
						} else {
							InputStream is = null;
							try {
								is = new FileInputStream(xslIncludesFile);
								File destinationXslIncludesFile = new File(dataAreaBuilderIncludesDir,
										xslIncludesFile.getName());
								IOUtils.copy(is, new FileOutputStream(destinationXslIncludesFile));
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
						String newJson = gson.toJson(builders, typeOfHashMap);
						is = new ByteArrayInputStream(newJson.getBytes());
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
