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

/**
 * CLI-friendly reader for project-level global preferences.
 * 
 * <p>
 * This class reads the {@code .cimtool-global-preferences} file which contains
 * global preference values exported from the Eclipse PreferenceStore. This
 * allows the CLI to use the same customized defaults that the user has
 * configured in the Eclipse UI via the PlantUML Builders preference page.
 * </p>
 * 
 * <p>
 * The preferences file uses Turtle (TTL) format with the following structure:
 * </p>
 * 
 * <pre>
 * &lt;http://cimtoole.langdale.com.au/2009/project/global&gt;
 *     &lt;http://cimtoole.langdale.com.au/2009/builder-prefs#plantuml_theme&gt;
 *     "cerulean" ;
 *     &lt;http://cimtoole.langdale.com.au/2009/builder-prefs#plantuml_docroot_classes_color&gt;
 *     "#FF0000" .
 * </pre>
 * 
 * <p>
 * This file is written by CIMToolPlugin when the user saves changes in the
 * PlantUML Builders preference page.
 * </p>
 * 
 * @see CLIBuilderPreferences
 * @see CLIGlobalPreferenceDefaults
 */
public class CLIGlobalPreferences implements GlobalPreferences {

	/** Global preferences file name */
	private static final String GLOBAL_PREFERENCES_FILENAME = ".cimtool-global-preferences";

	private final OntModel preferencesModel;
	private final boolean loaded;

	/**
	 * Create a CLIGlobalPreferences instance by loading the global preferences file
	 * from a project directory.
	 * 
	 * <p>
	 * If the file does not exist, this instance will return null for all preference
	 * lookups, allowing fallback to hardcoded defaults.
	 * </p>
	 * 
	 * @param projectDir the CIMTool project directory
	 * @throws IOException if the file exists but cannot be read
	 */
	public CLIGlobalPreferences(File projectDir) throws IOException {
		File prefsFile = new File(projectDir, GLOBAL_PREFERENCES_FILENAME);

		if (!prefsFile.exists()) {
			// No global preferences file - this is normal for projects that
			// haven't had preferences exported yet
			this.preferencesModel = null;
			this.loaded = false;
		} else {
			this.preferencesModel = loadPreferencesModel(prefsFile);
			this.loaded = true;
		}
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
	 * Check if global preferences were loaded.
	 * 
	 * @return true if the global preferences file was found and loaded
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Get a global preference value.
	 * 
	 * @param preferenceName the preference name (e.g., "plantuml_theme")
	 * @return the preference value, or null if not set or file not loaded
	 */
	public String getPreference(String preferenceName) {
		if (preferencesModel == null) {
			return null;
		}

		OntResource subject = preferencesModel.createResource(GLOBAL_SUBJECT_URI);
		Property property = ResourceFactory.createProperty(BUILDER_PREFS_NS + preferenceName);
		return subject.getString(property);
	}

	/**
	 * Check if a global preference exists.
	 * 
	 * @param preferenceName the preference name
	 * @return true if the preference has a value
	 */
	public boolean hasPreference(String preferenceName) {
		return getPreference(preferenceName) != null;
	}

}