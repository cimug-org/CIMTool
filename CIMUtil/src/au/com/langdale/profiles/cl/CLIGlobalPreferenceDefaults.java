/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import au.com.langdale.preferences.GlobalPreferences;

import java.util.Map;

/**
 * Default values for builder preferences.
 * 
 * <p>
 * This class provides the same default values as the Eclipse-based
 * {@code PreferenceInitializer} class in CIMToolPlugin, but without any Eclipse
 * dependencies. This allows the CLI to use consistent defaults when preferences
 * are not explicitly set in the {@code .builder-preferences} file.
 * </p>
 * 
 * <p>
 * The defaults here should be kept in sync with:
 * {@code au.com.langdale.cimtoole.preferences.PreferenceInitializer}
 * </p>
 * 
 * @see CLIBuilderPreferences
 */
public final class CLIGlobalPreferenceDefaults implements GlobalPreferences {

	private static Map<String, Object> DEFAULTS;

	static {
		// Create a temporary instance just to call the default method
		DEFAULTS = new CLIGlobalPreferenceDefaults().getPreferenceDefaults();
	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private CLIGlobalPreferenceDefaults() {
	}

	/**
	 * Get the default value for a preference.
	 * 
	 * @param preferenceName the preference name (e.g., "plantuml_theme")
	 * @return the default value, or null if no default is defined
	 */
	public static String getDefault(String preferenceName) {
		Object value = DEFAULTS.get(preferenceName);
		return value != null ? value.toString() : null;
	}

	/**
	 * Get the default value for a preference, with a fallback.
	 * 
	 * @param preferenceName the preference name
	 * @param fallback       the value to return if no default is defined
	 * @return the default value, or fallback if no default is defined
	 */
	public static String getDefault(String preferenceName, String fallback) {
		Object value = DEFAULTS.get(preferenceName);
		return value != null ? value.toString() : fallback;
	}

	/**
	 * Check if a default value exists for a preference.
	 * 
	 * @param preferenceName the preference name
	 * @return true if a default value is defined
	 */
	public static boolean hasDefault(String preferenceName) {
		return DEFAULTS.containsKey(preferenceName);
	}

}