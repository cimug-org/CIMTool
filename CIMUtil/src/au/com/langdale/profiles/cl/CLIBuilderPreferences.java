/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import au.com.langdale.colors.util.ColorUtils;
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

/**
 * CLI-friendly builder preferences reader for CIMTool projects.
 * 
 * <p>
 * This class provides read-only access to builder preferences with a
 * three-level fallback chain that mirrors the Eclipse behavior:
 * </p>
 * 
 * <ol>
 * <li><b>Per-resource preferences</b> from {@code .builder-preferences} -
 * overrides for specific profile files set via resource-specific dialogs in
 * Eclipse</li>
 * <li><b>Global preferences</b> from {@code .cimtool-global-preferences} -
 * user's customized defaults exported from the Eclipse PreferenceStore (set via
 * PlantUML Builders preference page)</li>
 * <li><b>Hardcoded defaults</b> from {@link CLIGlobalPreferenceDefaults} -
 * fallback values matching those in {@code PreferenceInitializer}</li>
 * </ol>
 * 
 * <p>
 * Both preference files use Turtle (TTL) format. The per-resource file
 * structure:
 * </p>
 * 
 * <pre>
 * &lt;http://cimtoole.langdale.com.au/2009/project/Profiles/MyProfile.puml&gt;
 *     &lt;http://cimtoole.langdale.com.au/2009/builder-prefs#plantuml_theme&gt;
 *     "cerulean" .
 * </pre>
 * 
 * @see CLIGlobalPreferences
 * @see CLIGlobalPreferenceDefaults
 * @see CLISettings
 */
public class CLIBuilderPreferences implements GlobalPreferences {

	/** Builder preferences file name */
	private static final String BUILDER_PREFERENCES_FILENAME = ".builder-preferences";

	private final OntModel preferencesModel;
	private final CLIGlobalPreferences globalPreferences;
	private final File projectDir;

	/**
	 * Create a CLIBuilderPreferences instance by loading the preferences file from
	 * a project directory.
	 * 
	 * <p>
	 * This constructor loads both the per-resource preferences from
	 * {@code .builder-preferences} and the global preferences from
	 * {@code .cimtool-global-preferences} (if it exists).
	 * </p>
	 * 
	 * @param projectDir the CIMTool project directory containing the
	 *                   .builder-preferences file
	 * @throws IOException if the preferences file cannot be read
	 */
	public CLIBuilderPreferences(File projectDir) throws IOException {
		this.projectDir = projectDir;
		File prefsFile = new File(projectDir, BUILDER_PREFERENCES_FILENAME);

		if (!prefsFile.exists()) {
			// Create empty model if preferences file doesn't exist
			this.preferencesModel = ModelFactory.createMem();
		} else {
			this.preferencesModel = loadPreferencesModel(prefsFile);
		}

		// Load global preferences (exported from Eclipse PreferenceStore)
		this.globalPreferences = new CLIGlobalPreferences(projectDir);
	}

	/**
	 * Load the preferences model from a file.
	 */
	private OntModel loadPreferencesModel(File prefsFile) throws IOException {
		OntModel model = ModelFactory.createMem();
		try (InputStream is = new BufferedInputStream(new FileInputStream(prefsFile))) {
			IO.read(model, is, PROJECT_NS, Format.TURTLE.toFormat());
		}
		return model;
	}

	/**
	 * Get a preference value for a resource, with fallback chain.
	 * 
	 * <p>
	 * The preference resolution order is:
	 * </p>
	 * <ol>
	 * <li>Per-resource preferences from {@code .builder-preferences}</li>
	 * <li>Global preferences from {@code .cimtool-global-preferences} (exported
	 * from Eclipse)</li>
	 * <li>Hardcoded defaults from {@link CLIGlobalPreferenceDefaults}</li>
	 * </ol>
	 * 
	 * @param resourcePath   the project-relative path of the resource (e.g.,
	 *                       "Profiles/MyProfile.puml")
	 * @param preferenceName the preference name (e.g., "plantuml_theme")
	 * @return the preference value, or the default value if not found
	 */
	public String getPreference(String resourcePath, String preferenceName) {
		// 1. Try per-resource preference from .builder-preferences
		OntResource subject = createSubject(resourcePath);
		Property property = createProperty(preferenceName);
		String value = subject.getString(property);

		// 2. Fall back to global preferences from .cimtool-global-preferences
		if (value == null && globalPreferences != null) {
			value = globalPreferences.getPreference(preferenceName);
		}

		// 3. Fall back to hardcoded defaults
		if (value == null) {
			value = CLIGlobalPreferenceDefaults.getDefault(preferenceName);
		}

		return value;
	}

	/**
	 * Get a preference value for a file, with fallback chain.
	 * 
	 * <p>
	 * See {@link #getPreference(String, String)} for the preference resolution
	 * order.
	 * </p>
	 * 
	 * @param file           the file (path will be made relative to the project
	 *                       directory)
	 * @param preferenceName the preference name
	 * @return the preference value, or the default value if not found
	 */
	public String getPreference(File file, String preferenceName) {
		String relativePath = getRelativePath(file);
		return getPreference(relativePath, preferenceName);
	}

	/**
	 * Get the PlantUML theme preference.
	 * 
	 * @param file the file
	 * @return the theme name, or null if not set
	 */
	public String getPlantUmlTheme(File file) {
		return getPreference(file, PREF_PLANTUML_THEME);
	}

	/**
	 * Get the document root classes color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getDocrootClassesColor(File file) {
		return getPreference(file, PREF_DOCROOT_CLASSES_COLOR);
	}

	/**
	 * Get the concrete classes color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getConcreteClassesColor(File file) {
		return getPreference(file, PREF_CONCRETE_CLASSES_COLOR);
	}

	/**
	 * Get the abstract classes color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getAbstractClassesColor(File file) {
		return getPreference(file, PREF_ABSTRACT_CLASSES_COLOR);
	}

	/**
	 * Get the enumerations color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getEnumerationsColor(File file) {
		return getPreference(file, PREF_ENUMERATIONS_COLOR);
	}

	/**
	 * Get the CIM datatypes color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getCimDatatypesColor(File file) {
		return getPreference(file, PREF_CIMDATATYPES_COLOR);
	}

	/**
	 * Get the compounds color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getCompoundsColor(File file) {
		return getPreference(file, PREF_COMPOUNDS_COLOR);
	}

	/**
	 * Get the primitives color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getPrimitivesColor(File file) {
		return getPreference(file, PREF_PRIMITIVES_COLOR);
	}

	/**
	 * Get the choices color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getChoicesColor(File file) {
		return getPreference(file, PREF_CHOICES_COLOR);
	}

	/**
	 * Get the references color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getRefsColor(File file) {
		return getPreference(file, PREF_REFS_COLOR);
	}

	/**
	 * Get the shadow classes color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getShadowClassesColor(File file) {
		return getPreference(file, PREF_SHADOW_CLASSES_COLOR);
	}

	/**
	 * Get the errors color preference.
	 * 
	 * @param file the file
	 * @return the color value, or null if not set
	 */
	public String getErrorsColor(File file) {
		return getPreference(file, PREF_ERRORS_COLOR);
	}

	/**
	 * Check if anonymous classes should use white color.
	 * 
	 * @param file the file
	 * @return true if anonymous classes use white color, false otherwise
	 */
	public boolean isAnonymousClassesColorWhite(File file) {
		String value = getPreference(file, PREF_ANONYMOUS_CLASSES_COLOR_WHITE);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if dark mode is enabled.
	 * 
	 * @param file the file
	 * @return true if dark mode is enabled, false otherwise
	 */
	public boolean isDarkModeEnabled(File file) {
		String value = getPreference(file, PREF_ENABLE_DARK_MODE);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if shadowing is enabled.
	 * 
	 * @param file the file
	 * @return true if shadowing is enabled, false otherwise
	 */
	public boolean isShadowingEnabled(File file) {
		String value = getPreference(file, PREF_ENABLE_SHADOWING);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if enumerations should be hidden.
	 * 
	 * @param file the file
	 * @return true if enumerations should be hidden, false otherwise
	 */
	public boolean isHideEnumerations(File file) {
		String value = getPreference(file, PREF_HIDE_ENUMERATIONS);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if CIM datatypes should be hidden.
	 * 
	 * @param file the file
	 * @return true if CIM datatypes should be hidden, false otherwise
	 */
	public boolean isHideCimDatatypes(File file) {
		String value = getPreference(file, PREF_HIDE_CIMDATATYPES);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if compounds should be hidden.
	 * 
	 * @param file the file
	 * @return true if compounds should be hidden, false otherwise
	 */
	public boolean isHideCompounds(File file) {
		String value = getPreference(file, PREF_HIDE_COMPOUNDS);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if primitives should be hidden.
	 * 
	 * @param file the file
	 * @return true if primitives should be hidden, false otherwise
	 */
	public boolean isHidePrimitives(File file) {
		String value = getPreference(file, PREF_HIDE_PRIMITIVES);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Check if cardinality should be hidden for required attributes.
	 * 
	 * @param file the file
	 * @return true if cardinality should be hidden for required attributes, false
	 *         otherwise
	 */
	public boolean isHideCardinalityForRequiredAttributes(File file) {
		String value = getPreference(file, PREF_HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES);
		return "true".equalsIgnoreCase(value);
	}

	/**
	 * Get the horizontal spacing preference.
	 * 
	 * @param file the file
	 * @return the horizontal spacing value as a string, or null if not set
	 */
	public String getHorizontalSpacing(File file) {
		return getPreference(file, PREF_HORIZONTAL_SPACING);
	}

	/**
	 * Get the horizontal spacing preference for a file as an integer.
	 * 
	 * @param file         the file
	 * @param defaultValue the default value to return if not set or invalid
	 * @return the horizontal spacing value, or defaultValue if not set or invalid
	 */
	public int getHorizontalSpacing(File file, int defaultValue) {
		String value = getHorizontalSpacing(file);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get the vertical spacing preference.
	 * 
	 * @param file the file
	 * @return the vertical spacing value as a string, or null if not set
	 */
	public String getVerticalSpacing(File file) {
		return getPreference(file, PREF_VERTICAL_SPACING);
	}

	/**
	 * Get the vertical spacing preference for a file as an integer.
	 * 
	 * @param file         the file
	 * @param defaultValue the default value to return if not set or invalid
	 * @return the vertical spacing value, or defaultValue if not set or invalid
	 */
	public int getVerticalSpacing(File file, int defaultValue) {
		String value = getVerticalSpacing(file);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public String getBuilderParameters(File file) {
		StringBuffer builderParameters = new StringBuffer();
		// Currently we only have PlantUML builder parameters...
		if (isFile(file, "Profiles", "puml") || isFile(file, "Profiles", "adoc")) {
			//
			// Set diagram preferences used by any PlantUML diagram builders...
			/**
			 * IMPORTANT:
			 * 
			 * To properly support the CIMTool CLI, it requires that we maintain two
			 * instances of this method. This one and another located in the Info class
			 * defined in the CIMToolPlugin project. Whenever any type of update is made
			 * there this parallel instance must be updated as well to ensure it matches.
			 */
			builderParameters.append("plantUMLTheme=" + getPreference(file, PREF_PLANTUML_THEME) + "|");
			builderParameters.append("docRootClassesColor=" + getPreference(file, PREF_DOCROOT_CLASSES_COLOR) + "|");
			builderParameters.append("anonymousCompoundsColor="
					+ ColorUtils.lighten(getPreference(file, PREF_COMPOUNDS_COLOR), 60f) + "|");
			builderParameters.append("anonymousEnumerationsColor="
					+ ColorUtils.lighten(getPreference(file, PREF_ENUMERATIONS_COLOR), 60f) + "|");
			builderParameters.append("anonymousComplexTypesColor="
					+ ColorUtils.lighten(getPreference(file, PREF_CONCRETE_CLASSES_COLOR), 60f) + "|");
			builderParameters.append("docRootClassesFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_DOCROOT_CLASSES_COLOR)) + "|");
			builderParameters.append("concreteClassesColor=" + getPreference(file, PREF_CONCRETE_CLASSES_COLOR) + "|");
			builderParameters.append("concreteClassesFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_CONCRETE_CLASSES_COLOR)) + "|");
			builderParameters.append("abstractClassesColor=" + getPreference(file, PREF_ABSTRACT_CLASSES_COLOR) + "|");
			builderParameters.append("abstractClassesFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_ABSTRACT_CLASSES_COLOR)) + "|");
			builderParameters.append("enumerationsColor=" + getPreference(file, PREF_ENUMERATIONS_COLOR) + "|");
			builderParameters.append("enumerationsFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_ENUMERATIONS_COLOR)) + "|");
			builderParameters.append("cimDatatypesColor=" + getPreference(file, PREF_CIMDATATYPES_COLOR) + "|");
			builderParameters.append("cimDatatypesFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_CIMDATATYPES_COLOR)) + "|");
			builderParameters.append("compoundsColor=" + getPreference(file, PREF_COMPOUNDS_COLOR) + "|");
			builderParameters.append("compoundsFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_COMPOUNDS_COLOR)) + "|");
			builderParameters.append("primitivesColor=" + getPreference(file, PREF_PRIMITIVES_COLOR) + "|");
			builderParameters.append("primitivesFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_PRIMITIVES_COLOR)) + "|");
			builderParameters.append("choicesColor=" + getPreference(file, PREF_CHOICES_COLOR) + "|");
			builderParameters.append(
					"choicesFontColor=" + ColorUtils.getHexFontColor(getPreference(file, PREF_CHOICES_COLOR)) + "|");
			builderParameters.append("refsColor=" + getPreference(file, PREF_REFS_COLOR) + "|");
			builderParameters
					.append("refsFontColor=" + ColorUtils.getHexFontColor(getPreference(file, PREF_REFS_COLOR)) + "|");
			builderParameters.append("shadowClassesColor=" + getPreference(file, PREF_SHADOW_CLASSES_COLOR) + "|");
			builderParameters.append("shadowClassesFontColor="
					+ ColorUtils.getHexFontColor(getPreference(file, PREF_SHADOW_CLASSES_COLOR)) + "|");
			builderParameters.append("errorsColor=" + getPreference(file, PREF_ERRORS_COLOR) + "|");
			builderParameters.append(
					"errorsFontColor=" + ColorUtils.getHexFontColor(getPreference(file, PREF_ERRORS_COLOR)) + "|");
			builderParameters.append(
					"setAnonymousClassesColorWhite=" + getPreference(file, PREF_ANONYMOUS_CLASSES_COLOR_WHITE) + "|");
			builderParameters.append("enableDarkMode=" + getPreference(file, PREF_ENABLE_DARK_MODE) + "|");
			builderParameters.append("enableShadowing=" + getPreference(file, PREF_ENABLE_SHADOWING) + "|");
			builderParameters.append("hideEnumerations=" + getPreference(file, PREF_HIDE_ENUMERATIONS) + "|");
			builderParameters.append("hideCIMDatatypes=" + getPreference(file, PREF_HIDE_CIMDATATYPES) + "|");
			builderParameters.append("hideCompounds=" + getPreference(file, PREF_HIDE_COMPOUNDS) + "|");
			builderParameters.append("hidePrimitives=" + getPreference(file, PREF_HIDE_PRIMITIVES) + "|");
			builderParameters.append("hideCardinalityForRequiredAttributes="
					+ getPreference(file, PREF_HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES) + "|");
			builderParameters.append("horizontalSpacing=" + getPreference(file, PREF_HORIZONTAL_SPACING) + "|");
			builderParameters.append("verticalSpacing=" + getPreference(file, PREF_VERTICAL_SPACING) + "|");
		}

		return builderParameters.toString();
	}

	/**
	 * Create a subject resource for a given path.
	 */
	private OntResource createSubject(String path) {
		try {
			String encodedPath = new URI(null, path, null).toASCIIString();
			return preferencesModel.createResource(PROJECT_NS + encodedPath);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Invalid resource path: " + path, e);
		}
	}

	/**
	 * Create a property for a preference name.
	 */
	private Property createProperty(String preferenceName) {
		return ResourceFactory.createProperty(BUILDER_PREFS_NS + preferenceName);
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
	 * Get the file extension of the file passed in.
	 */
	private String getFileExtension(File file) {
		if (file == null)
			return "";
		String name = file.getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot <= 0 || lastDot == name.length() - 1) {
			return "";
		}
		return name.substring(lastDot + 1);
	}

	/**
	 * Determine if the file passed in has an extension mapping the specified type.
	 */
	private boolean hasExt(File file, String type) {
		String ext = getFileExtension(file);
		return ext != null && ext.equals(type);
	}

	/**
	 * Determine if the resource passed in is a file in the specified location.
	 */
	private boolean isFile(File resource, String location) {
		if (resource == null || location == null) {
			return false;
		}
		String relativePath = getRelativePath(resource);
		return relativePath.startsWith(location + "/");
	}

	/**
	 * Determine if the resource passed in is a file in the specified location with
	 * one of the given extensions.
	 */
	private boolean isFile(File resource, String location, String... types) {
		boolean hasExt = false;
		for (String t : types)
			hasExt = hasExt | hasExt(resource, t);
		return isFile(resource, location) && hasExt;
	}

}