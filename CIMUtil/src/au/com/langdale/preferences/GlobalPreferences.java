/*
 * This software is Copyright 2005,2006,2007,2026 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.preferences;

import au.com.langdale.cim.CIM;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface GlobalPreferences {

	/** Namespace for project resource URIs */
	public static final String PROJECT_NS = "http://cimtoole.langdale.com.au/2009/project/";
	
	/** Namespace for builder preference properties */
	public static final String BUILDER_PREFS_NS = "http://cimtoole.langdale.com.au/2009/builder-prefs#";
	
	/** The RDF namespace for global preferences */
	public static final String GLOBAL_PREFS_NS = "http://cimtoole.langdale.com.au/2009/global-prefs#";
	
	/** Subject URI for global preferences */
	public static final String GLOBAL_SUBJECT_URI = PROJECT_NS + "global";
	
	String PREF_PROFILE_PATH = "profile_path";
	String PREF_BASE_MODEL_PATH = "base_model_path";
	String PREF_MERGED_SCHEMA_PATH = "merged_schema_path";
	//
	String PREF_SCHEMA_NAMESPACE = "schema_namespace";
	String PREF_MERGE_SHADOW_EXTENSIONS = "merge_shadow_extensions";
	String PREF_SELF_HEAL_ON_IMPORT = "self_heal_on_import";
	String PREF_INSTANCE_NAMESPACE = "instance_namespace";
	String PREF_PRESERVE_NAMESPACES = "preserve_namespaces";
	String PREF_USE_PACKAGE_NAMES = "user_package_names";
	String PREF_PROBLEM_PER_SUBJECT = "problem_per_subject";
	String PREF_PROFILE_NAMESPACE = "profile_namespace";
	String PREF_PROFILE_ENVELOPE = "profile_envelope";
	String PREF_MAPPING_NAMESPACE = "mapping_namespace";
	String PREF_MAPPING_LABEL = "mapping_label";

	/**
	 * PlantUML preferences below. When adding a new QualifiedName to the list here
	 * be sure to also add the new entry to the Info.getPlantUMLParameters() method
	 * to be passed along to the ProfileSerializer class.
	 */
	String PREF_PLANTUML_THEME = "plantuml_theme";
	String PREF_DOCROOT_CLASSES_COLOR = "plantuml_docroot_classes_color";
	String PREF_CONCRETE_CLASSES_COLOR = "plantuml_concrete_classes_color";
	String PREF_ABSTRACT_CLASSES_COLOR = "plantuml_abstract_classes_color";
	String PREF_ENUMERATIONS_COLOR = "plantuml_enumerations_color";
	String PREF_CIMDATATYPES_COLOR = "plantuml_cimdatatypes_color";
	String PREF_COMPOUNDS_COLOR = "plantuml_compounds_color";
	String PREF_PRIMITIVES_COLOR = "plantuml_primitives_color";
	String PREF_CHOICES_COLOR = "plantuml_choices_color";
	String PREF_REFS_COLOR = "plantuml_refs_color";
	String PREF_ERRORS_COLOR = "plantuml_errors_color";
	String PREF_ANONYMOUS_CLASSES_COLOR_WHITE = "plantuml_set_anonymous_classes_color_to_white";
	String PREF_ENABLE_DARK_MODE = "plantuml_enable_dark_mode";
	String PREF_ENABLE_SHADOWING = "plantuml_enable_shadowing";
	String PREF_HIDE_ENUMERATIONS = "plantuml_hide_enumerations";
	String PREF_HIDE_CIMDATATYPES = "plantuml_hide_cimdatatypes";
	String PREF_HIDE_COMPOUNDS = "plantuml_hide_compounds";
	String PREF_HIDE_PRIMITIVES = "plantuml_hide_primitives";
	String PREF_HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES = "plantuml_hide_cardinality_for_required_attributes";
	String PREF_HORIZONTAL_SPACING = "plantuml_nodesep_horizontal_spacing";
	String PREF_VERTICAL_SPACING = "plantuml_ranksep_vertical_spacing";

	/**
	 * The XSLT style used to generate the hidden .current-profile.puml live
	 * preview file. Choices are "puml-rdfs-t2b" (top-to-bottom) and
	 * "puml-rdfs-l2r" (left-to-right).
	 */
	String PREF_CURRENT_PROFILE_PREVIEW_STYLE = "plantuml_current_profile_preview_style";

	/**
	 * Returns the mapping of default preference values to the corresponding
	 * preference symbols
	 */
	default Map<String, Object> getPreferenceDefaults() {
		Map<String, Object> defaults = new HashMap<String, Object>();
		//
		defaults.put(PREF_PROFILE_PATH, "");
		defaults.put(PREF_BASE_MODEL_PATH, "");
		defaults.put(PREF_MERGED_SCHEMA_PATH, "");
		defaults.put(PREF_INSTANCE_NAMESPACE, "http://www.ucaiug.org/network#");
		defaults.put(PREF_PROFILE_NAMESPACE, "http://www.ucaiug.org/profile#");
		defaults.put(PREF_MAPPING_NAMESPACE, "http://langdale.com.au/2010/schema-mapping#");
		defaults.put(PREF_MAPPING_LABEL, "Mappings");
		defaults.put(PREF_SCHEMA_NAMESPACE, CIM.NS);
		defaults.put(PREF_MERGE_SHADOW_EXTENSIONS, true);
		defaults.put(PREF_SELF_HEAL_ON_IMPORT, false);
		defaults.put(PREF_PROFILE_ENVELOPE, "Profile");
		defaults.put(PREF_PRESERVE_NAMESPACES, true);
		defaults.put(PREF_PROBLEM_PER_SUBJECT, true);
		defaults.put(PREF_USE_PACKAGE_NAMES, false);
		defaults.put(PREF_PLANTUML_THEME, "_none_");
		defaults.put(PREF_DOCROOT_CLASSES_COLOR, "#D3FBFE");
		defaults.put(PREF_CONCRETE_CLASSES_COLOR, "#FFFFE0");
		defaults.put(PREF_ABSTRACT_CLASSES_COLOR, "#E8E8E8");
		defaults.put(PREF_ENUMERATIONS_COLOR, "#A9F1A9");
		defaults.put(PREF_CIMDATATYPES_COLOR, "#FFEBCD");
		defaults.put(PREF_COMPOUNDS_COLOR, "#FFEBCD");
		defaults.put(PREF_PRIMITIVES_COLOR, "#E6E6FF");
		defaults.put(PREF_CHOICES_COLOR, "#E6E6FF");
		defaults.put(PREF_REFS_COLOR, "#FFDBDB");
		defaults.put(PREF_ERRORS_COLOR, "#FFC0CB");
		defaults.put(PREF_ANONYMOUS_CLASSES_COLOR_WHITE, false);
		defaults.put(PREF_ENABLE_DARK_MODE, false);
		defaults.put(PREF_ENABLE_SHADOWING, true);
		defaults.put(PREF_HIDE_ENUMERATIONS, false);
		defaults.put(PREF_HIDE_CIMDATATYPES, true);
		defaults.put(PREF_HIDE_COMPOUNDS, true);
		defaults.put(PREF_HIDE_PRIMITIVES, true);
		defaults.put(PREF_HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES, false);
		defaults.put(PREF_HORIZONTAL_SPACING, "20");
		defaults.put(PREF_VERTICAL_SPACING, "30");
		defaults.put(PREF_CURRENT_PROFILE_PREVIEW_STYLE, "puml-rdfs-t2b");
		//
		return Collections.unmodifiableMap(defaults);
	}

}
