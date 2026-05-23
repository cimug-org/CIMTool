/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple support for custom stereotypes (not in the UML model) but needed at
 * the time of profiling. A prime example is the Operation and ShortCircuit
 * stereotypes used in the IEC 61970-452 Ed. 3.0 (CIM16) profiles.
 */
public final class StereotypeExtensions {

	private static final Logger log = LoggerFactory.getLogger(StereotypeExtensions.class);

	private static final Map<String, Map<String, String>> projectStereotypeExtensions = new ConcurrentHashMap<>();
	private static final Map<String, Set<String>> projectStereotypeURIs = new ConcurrentHashMap<>();

	private StereotypeExtensions() {
	}

	/**
	 * <pre>
	 * Based on UML 2.5 specifications, UML stereotypes are considered
	 * case-insensitive.
	 * 
	 * While the convention is for Stereotype names to start with
	 * an upper-case letter, UML allows for variations and matches between
	 * stereotype definitions and applications to be case-insensitive. 
	 * 
	 * This means that a tool or system should recognize «stereotype» and 
	 * «STEREOTYPE» as the same stereotype, even if their casing differs. 
	 * 
	 * However, some tools may display stereotype names with a different 
	 * initial letter than how they are defined, which is considered valid 
	 * but stylistically obsolete in later versions of UML (2.4+). The best 
	 * practice according to UML 2.4 and later is to start the stereotype 
	 * name with an uppercase letter, similar to how Class names are 
	 * capitalized.
	 * 
	 * As a result the below mapping handles stereotypes in a case insensitive
	 * manner and converts the keys to lowercase.
	 * </pre>
	 * 
	 * @param stereotypeExtensionsFile
	 */
	public static void initStereotypeExtensions(String projectName, File stereotypeExtensionsFile) {

		if (projectName == null || projectName.trim().isBlank() || !stereotypeExtensionsFile.exists())
			return;

		if (projectStereotypeExtensions.containsKey(projectName))
			projectStereotypeExtensions.remove(projectName);
		
		if (projectStereotypeURIs.containsKey(projectName))
			projectStereotypeURIs.remove(projectName);

		// Map for Stereotype -> Stereotype label
		Map<String, String> stereotypeToLabelMap = new HashMap<>();
		Set<String> stereotypeURIs = new HashSet<>();

		if (stereotypeExtensionsFile != null) {
			Properties props = new Properties();

			try (FileInputStream fis = new FileInputStream(stereotypeExtensionsFile)) {
				props.load(fis);
				for (String stereotypeKey : props.stringPropertyNames()) {
					stereotypeToLabelMap.put(stereotypeKey, props.getProperty(stereotypeKey));
					stereotypeURIs.add(UML.NS + stereotypeKey.toLowerCase().replaceAll("\\s", ""));
				}
				projectStereotypeExtensions.put(projectName, stereotypeToLabelMap);
				projectStereotypeURIs.put(projectName, stereotypeURIs);
			} catch (Exception e) {
				log.error("Failed to load stereotype extensions for project: {}", projectName, e);
			}
		}

	}

	public static Set<String> getStereotypes(String projectName) {
		if (projectStereotypeExtensions.containsKey(projectName))
			return projectStereotypeExtensions.get(projectName).keySet();

		return Collections.emptySet();
	}
	
	public static Set<String> getStereotypesURIs(String projectName) {
		if (projectName != null && projectStereotypeURIs.containsKey(projectName))
			return projectStereotypeURIs.get(projectName);

		return Collections.emptySet();
	}

	public static void removeStereotypes(String projectName) {
		projectStereotypeExtensions.remove(projectName);
	}

}