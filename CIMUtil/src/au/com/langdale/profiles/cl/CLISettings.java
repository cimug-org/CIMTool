/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.preferences.GlobalPreferences;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * CLI-friendly settings reader for CIMTool project settings.
 * 
 * <p>
 * This class provides read-only access to settings stored in the
 * {@code .cimtool-settings} file. It is a simplified version of the
 * Eclipse-dependent {@code Settings} class.
 * </p>
 * 
 * <p>
 * The settings file uses Turtle (TTL) format with the following structure:
 * </p>
 * 
 * <pre>
 * &lt;http://cimtoole.langdale.com.au/2009/project/Profiles/MyProfile.owl&gt;
 *     &lt;http://cimtoole.langdale.com.au/2009/setting#profile_namespace&gt;
 *     "http://example.com/MyProfile#" .
 * </pre>
 * 
 * @see CLIBuilderPreferences
 */
public class CLISettings implements GlobalPreferences {

	/** Namespace for setting properties */
	private static final String SETTING_NS = "http://cimtoole.langdale.com.au/2009/setting#";

	/** Settings file name */
	public static final String SETTINGS_FILENAME = ".cimtool-settings";

	private final OntModel settingsModel;
	private final File projectDir;

	/**
	 * Create a CLISettings instance by loading the settings file from a project
	 * directory.
	 * 
	 * @param projectDir the CIMTool project directory containing the
	 *                   .cimtool-settings file
	 * @throws IOException if the settings file cannot be read
	 */
	public CLISettings(File projectDir) throws IOException {
		this.projectDir = projectDir;
		File settingsFile = new File(projectDir, SETTINGS_FILENAME);

		if (!settingsFile.exists()) {
			// Create empty model if settings file doesn't exist
			this.settingsModel = ModelFactory.createMem();
		} else {
			this.settingsModel = loadSettingsModel(settingsFile);
		}
	}

	/**
	 * Load the settings model from a file.
	 */
	private OntModel loadSettingsModel(File settingsFile) throws IOException {
		OntModel model = ModelFactory.createMem();
		try (InputStream is = new BufferedInputStream(new FileInputStream(settingsFile))) {
			IO.read(model, is, PROJECT_NS, Format.TURTLE.toFormat());
		}
		return model;
	}

	/**
	 * Get a setting value for a resource.
	 * 
	 * @param resourcePath the project-relative path of the resource (e.g.,
	 *                     "Profiles/MyProfile.owl")
	 * @param settingName  the setting name (e.g., "profile_namespace")
	 * @return the setting value, or null if not found
	 */
	public String getSetting(String resourcePath, String settingName) {
		OntResource subject = createSubject(resourcePath);
		Property property = createProperty(settingName);
		return subject.getString(property);
	}

	/**
	 * Get a setting value for a file.
	 * 
	 * @param file        the file (path will be made relative to the project
	 *                    directory)
	 * @param settingName the setting name
	 * @return the setting value, or null if not found
	 */
	public String getSetting(File file, String settingName) {
		String relativePath = getRelativePath(file);
		return getSetting(relativePath, settingName);
	}

	/**
	 * Get a setting value stored against the project root.
	 *
	 * <p>
	 * Project-level settings (such as {@code merge_shadow_extensions} and
	 * {@code self_heal_on_import}) are written by CIMToolPlugin against the project
	 * root resource, not against individual file resources. This method looks them
	 * up using {@link GlobalPreferences#GLOBAL_SUBJECT_URI} as the subject,
	 * consistent with how {@link CLIGlobalPreferences} reads its project-root
	 * preferences.
	 * </p>
	 *
	 * @param settingName the setting name
	 * @return the setting value, or null if not found
	 */
	public String getProjectSetting(String settingName) {
		Property property = createProperty(settingName);
		return settingsModel.createResource(PROJECT_NS).getString(property);
	}

	/**
	 * Get the schema namespace setting.
	 * 
	 * @param profileFile the file
	 * @return the schema namespace, or null if not set
	 */
	public String getSchemaNamespace(File profileFile) {
		return getSetting(profileFile, PREF_SCHEMA_NAMESPACE);
	}

	/**
	 * Check if shadow extensions should be merged.
	 *
	 * <p>
	 * This is a project-level setting, not a per-file setting. It is stored in
	 * {@code .cimtool-settings} against the project root resource by CIMToolPlugin.
	 * </p>
	 *
	 * @return true if shadow extensions should be merged, false otherwise
	 */
	public boolean isMergeShadowExtensions() {
		String value = getProjectSetting(PREF_MERGE_SHADOW_EXTENSIONS);
		if (value == null)
			value = CLIGlobalPreferenceDefaults.getDefault(PREF_MERGE_SHADOW_EXTENSIONS);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if self-heal on import is enabled.
	 *
	 * <p>
	 * This is a project-level setting, not a per-file setting. It is stored in
	 * {@code .cimtool-settings} against the project root resource by CIMToolPlugin.
	 * </p>
	 *
	 * @return true if self-heal on import is enabled, false otherwise
	 */
	public boolean isSelfHealOnImport() {
		String value = getProjectSetting(PREF_SELF_HEAL_ON_IMPORT);
		if (value == null)
			value = CLIGlobalPreferenceDefaults.getDefault(PREF_SELF_HEAL_ON_IMPORT);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Create a subject resource for a given path.
	 */
	private OntResource createSubject(String path) {
		try {
			String encodedPath = new URI(null, path, null).toASCIIString();
			return this.settingsModel.createResource(PROJECT_NS + encodedPath);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Invalid resource path: " + path, e);
		}
	}

	/**
	 * Create a property for a setting name.
	 */
	private Property createProperty(String settingName) {
		return ResourceFactory.createProperty(SETTING_NS + settingName);
	}

	/**
	 * Get the path of a file relative to the project directory.
	 */
	private String getRelativePath(File file) {
		String filePath = file.getAbsolutePath();
		String projectPath = projectDir.getAbsolutePath();

		if (filePath.startsWith(projectPath)) {
			String relativePath = filePath.substring(projectPath.length());
			// Remove leading separator and normalize to forward slashes
			if (relativePath.startsWith(File.separator)) {
				relativePath = relativePath.substring(1);
			}
			return relativePath.replace(File.separatorChar, '/');
		}

		// If file is not under project dir, just use the filename
		return file.getName();
	}

	/**
	 * Get all schema files referenced in the settings.
	 * 
	 * <p>
	 * Schema files are identified by subjects with URIs starting with
	 * {@code http://cimtoole.langdale.com.au/2009/project/Schema/}
	 * </p>
	 * 
	 * @return list of schema files (relative to project directory)
	 */
	public List<File> getSchemaFiles() {
		List<File> schemaFiles = new java.util.ArrayList<>();
		String schemaPrefix = PROJECT_NS + "Schema/";

		// Iterate through all subjects in the model
		au.com.langdale.kena.ResIterator subjects = this.settingsModel.listSubjects();
		while (subjects.hasNext()) {
			au.com.langdale.kena.OntResource subject = subjects.nextResource();
			if (subject.isURIResource()) {
				String uri = subject.getURI();
				if (uri.startsWith(schemaPrefix)) {
					// Extract filename from URI
					String filename = uri.substring(schemaPrefix.length());
					// Decode URI encoding if present
					try {
						filename = java.net.URLDecoder.decode(filename, "UTF-8");
					} catch (java.io.UnsupportedEncodingException e) {
						// UTF-8 is always supported, this shouldn't happen
					}
					File schemaFile = new File(projectDir, "Schema" + File.separator + filename);
					if (!schemaFiles.contains(schemaFile)) {
						schemaFiles.add(schemaFile);
					}
				}
			}
		}

		return schemaFiles;
	}

}