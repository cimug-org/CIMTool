/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders;

import au.com.langdale.profiles.cl.builders.config.BuilderConfigDeserializer;
import au.com.langdale.profiles.cl.builders.config.BuilderConfigSerializer;
import au.com.langdale.profiles.cl.builders.config.InstantDeserializer;
import au.com.langdale.profiles.cl.builders.config.InstantSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for all CIMTool builders — both XSLT-based and Java-based.
 * 
 * <p>
 * This class provides access to builder configurations and their associated XSL
 * files. It loads builders from two sources on the classpath:
 * </p>
 * <ul>
 * <li>{@code builders.json} — XSLT-based builders (TEXT, XSD, TRANSFORM
 * types)</li>
 * <li>{@code java-builders.json} — Java-based builders (JAVA type) that use
 * CIMTool generator classes rather than XSLT stylesheets</li>
 * </ul>
 * <p>
 * Both files are loaded into a single unified {@link TreeMap}, producing one
 * alphabetically sorted registry. Alternatively, an external directory can be
 * provided for custom builders.
 * </p>
 * 
 * <p>
 * This class has no Eclipse dependencies and can be used in standalone
 * applications.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * // Load default bundled builders
 * BuilderRegistry registry = BuilderRegistry.getInstance();
 * 
 * // Get a specific builder configuration
 * BuilderConfig xsdBuilder = registry.getBuilder("xsd");
 * 
 * // Get the XSL input stream for transformation
 * InputStream xsl = registry.getBuilderXslStream("xsd");
 * </pre>
 */
public class BuilderRegistry {

	private static final Logger log = LoggerFactory.getLogger(BuilderRegistry.class);

	/**
	 * Default path to builders.json within the classpath. Since builders/ is in the
	 * Bundle-ClassPath, its contents are at the classpath root.
	 */
	private static final String DEFAULT_CONFIG_RESOURCE = "/builders.json";

	/**
	 * Default path to java-builders.json within the classpath.
	 */
	private static final String JAVA_CONFIG_RESOURCE = "/java-builders.json";

	/**
	 * Default path prefix for XSL files within the classpath. Since builders/ is in
	 * the Bundle-ClassPath, XSL files are at the classpath root.
	 */
	private static final String DEFAULT_XSL_RESOURCE_PREFIX = "/";

	/** Default path prefix for include files */
	private static final String INCLUDES_SUBDIR = "includes/";

	/** Singleton instance for bundled builders */
	private static BuilderRegistry instance;

	/** Type token for Gson deserialization of builder map */
	private static final Type BUILDER_MAP_TYPE = new TypeToken<Map<String, BuilderConfig>>() {
	}.getType();

	/** Configured Gson instance */
	private final Gson gson;

	/** Loaded builder configurations, keyed by style name */
	private final Map<String, BuilderConfig> builders;

	/** External directory for XSL files (null if using classpath resources) */
	private final File externalBuildersDir;

	/** Resource prefix for classpath loading */
	private final String resourcePrefix;

	/**
	 * Get the singleton instance that loads bundled builders from the classpath.
	 * 
	 * @return the shared BuilderRegistry instance
	 */
	public static synchronized BuilderRegistry getInstance() {
		if (instance == null) {
			instance = new BuilderRegistry();
		}
		return instance;
	}

	/**
	 * Create a registry that loads bundled builders from the classpath.
	 */
	public BuilderRegistry() {
		this(null, DEFAULT_XSL_RESOURCE_PREFIX);
	}

	/**
	 * Create a registry that loads builders from an external directory.
	 * 
	 * @param buildersDirectory the directory containing builders.json and XSL files
	 */
	public BuilderRegistry(File buildersDirectory) {
		this(buildersDirectory, null);
	}

	/**
	 * Private constructor with full configuration.
	 */
	private BuilderRegistry(File externalDir, String resourcePrefix) {
		this.externalBuildersDir = externalDir;
		this.resourcePrefix = resourcePrefix != null ? resourcePrefix : DEFAULT_XSL_RESOURCE_PREFIX;

		this.gson = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantDeserializer())
				.registerTypeAdapter(Instant.class, new InstantSerializer())
				.registerTypeAdapter(BuilderConfig.class, new BuilderConfigDeserializer())
				.registerTypeAdapter(BuilderConfig.class, new BuilderConfigSerializer()).setPrettyPrinting().create();

		this.builders = new TreeMap<>();
		loadBuilders();
	}

	/**
	 * Load builder configurations from the appropriate source. In classpath mode,
	 * loads both {@code builders.json} and {@code java-builders.json} into the
	 * unified registry. In external directory mode, loads only
	 * {@code builders.json} from the external directory.
	 */
	private void loadBuilders() {
		if (externalBuildersDir != null) {
			loadFromExternalDir(externalBuildersDir);
		} else {
			loadFromClasspath(DEFAULT_CONFIG_RESOURCE);
			loadFromClasspath(JAVA_CONFIG_RESOURCE);
		}
		log.info("BuilderRegistry: Loaded {} builders total", builders.size());
	}

	/**
	 * Load builder configurations from a classpath resource.
	 *
	 * @param resourcePath the classpath path to the JSON config file
	 */
	private void loadFromClasspath(String resourcePath) {
		try {
			InputStream is = getClass().getResourceAsStream(resourcePath);
			if (is == null) {
				log.warn("Builder config not found in classpath at: {}", resourcePath);
				return;
			}
			try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Map<String, BuilderConfig> loaded = gson.fromJson(reader, BUILDER_MAP_TYPE);
				if (loaded != null) {
					builders.putAll(loaded);
					log.debug("BuilderRegistry: Loaded {} builders from {}", loaded.size(), resourcePath);
				}
			}
		} catch (IOException e) {
			log.error("Error loading builder config from {}: {}", resourcePath, e.getMessage(), e);
		}
	}

	/**
	 * Load builder configurations from an external directory.
	 *
	 * @param dir the directory containing builders.json
	 */
	private void loadFromExternalDir(File dir) {
		try {
			File configFile = new File(dir, "builders.json");
			if (!configFile.exists() || !configFile.canRead()) {
				log.warn("builders.json not found at: {}", configFile.getAbsolutePath());
				return;
			}
			try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
				Map<String, BuilderConfig> loaded = gson.fromJson(reader, BUILDER_MAP_TYPE);
				if (loaded != null) {
					builders.putAll(loaded);
					log.debug("BuilderRegistry: Loaded {} builders from {}", loaded.size(),
							configFile.getAbsolutePath());
				}
			}
		} catch (IOException e) {
			log.error("Error loading builders configuration: {}", e.getMessage(), e);
		}
	}

	/**
	 * Get all available builder style names.
	 * 
	 * @return unmodifiable set of builder style names
	 */
	public Set<String> getBuilderNames() {
		return Collections.unmodifiableSet(builders.keySet());
	}

	/**
	 * Get all builder configurations.
	 * 
	 * @return unmodifiable map of style name to BuilderConfig
	 */
	public Map<String, BuilderConfig> getBuilders() {
		return Collections.unmodifiableMap(builders);
	}

	/**
	 * Get a specific builder configuration by style name.
	 * 
	 * @param style the builder style name (e.g., "xsd", "json-schema-draft-07")
	 * @return the BuilderConfig, or null if not found
	 */
	public BuilderConfig getBuilder(String style) {
		return builders.get(style);
	}

	/**
	 * Check if a builder exists with the given style name.
	 * 
	 * @param style the builder style name
	 * @return true if the builder exists
	 */
	public boolean hasBuilder(String style) {
		return builders.containsKey(style);
	}

	/**
	 * Find a builder by its output file extension.
	 * 
	 * @param extension the output file extension (e.g., "xsd",
	 *                  "draft-07.schema.json")
	 * @return the BuilderConfig, or null if not found
	 */
	public BuilderConfig getBuilderByExtension(String extension) {
		if (extension == null) {
			return null;
		}
		for (BuilderConfig config : builders.values()) {
			if (extension.equals(config.getExtension())) {
				return config;
			}
		}
		return null;
	}

	/**
	 * Get an InputStream for a builder's XSL file.
	 * 
	 * @param style the builder style name
	 * @return an InputStream for the XSL file, or null if not found
	 * @throws IOException if the file cannot be read
	 */
	public InputStream getBuilderXslStream(String style) throws IOException {
		if (style == null || !builders.containsKey(style)) {
			return null;
		}

		String xslFileName = style + ".xsl";

		if (externalBuildersDir != null) {
			// Load from external directory
			File xslFile = new File(externalBuildersDir, xslFileName);
			if (xslFile.exists() && xslFile.canRead()) {
				return new FileInputStream(xslFile);
			}
			return null;
		} else {
			// Load from classpath resources
			return getClass().getResourceAsStream(resourcePrefix + xslFileName);
		}
	}

	/**
	 * Get an InputStream for a builder's includes file.
	 * 
	 * @param style the builder style name
	 * @return an InputStream for the includes file, or null if not found or not
	 *         configured
	 * @throws IOException if the file cannot be read
	 */
	public InputStream getBuilderIncludesStream(String style) throws IOException {
		BuilderConfig config = builders.get(style);
		if (config == null || !config.hasIncludesFile()) {
			return null;
		}

		String includesFileName = config.getIncludesFile();

		if (externalBuildersDir != null) {
			// Load from external directory
			File includesFile = new File(new File(externalBuildersDir, INCLUDES_SUBDIR), includesFileName);
			if (includesFile.exists() && includesFile.canRead()) {
				return new FileInputStream(includesFile);
			}
			return null;
		} else {
			// Load from classpath resources
			return getClass().getResourceAsStream(resourcePrefix + INCLUDES_SUBDIR + includesFileName);
		}
	}

	/**
	 * Get the absolute file path for an includes file. This is used for XSLT
	 * xsl:include href resolution.
	 * 
	 * @param style the builder style name
	 * @return the absolute file path formatted for XSLT, or null if not applicable
	 */
	public String getIncludesFilePath(String style) {
		BuilderConfig config = builders.get(style);
		if (config == null || !config.hasIncludesFile()) {
			return null;
		}

		if (externalBuildersDir != null) {
			File includesFile = new File(new File(externalBuildersDir, INCLUDES_SUBDIR), config.getIncludesFile());
			if (includesFile.exists()) {
				// Format as file:// URL for XSLT
				String path = includesFile.getAbsolutePath();
				path = path.replace(" ", "%20");
				path = path.replace("\\", "/");
				return "file:///" + path;
			}
		}

		// For classpath resources, return the resource path
		// Note: This may need adjustment depending on how XSLT resolves includes
		return resourcePrefix + INCLUDES_SUBDIR + config.getIncludesFile();
	}

	/**
	 * Reload builders from the configuration source. This clears and reloads all
	 * builder configurations from both {@code builders.json} and
	 * {@code java-builders.json}.
	 */
	public void reload() {
		builders.clear();
		loadBuilders();
	}

	/**
	 * Get the number of registered builders.
	 * 
	 * @return the count of builders
	 */
	public int size() {
		return builders.size();
	}

	/**
	 * Reset the singleton instance (mainly for testing).
	 */
	public static synchronized void resetInstance() {
		instance = null;
	}
}