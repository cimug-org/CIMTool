/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import org.eclipse.core.runtime.QualifiedName;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.preferences.GlobalPreferences;

public interface QualifiedNames extends GlobalPreferences {

	QualifiedName PROFILE_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PROFILE_PATH);
	QualifiedName BASE_MODEL_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_BASE_MODEL_PATH);
	QualifiedName MERGED_SCHEMA_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_MERGED_SCHEMA_PATH);

	/**
	 * The preferences that follow
	 */
	QualifiedName SCHEMA_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_SCHEMA_NAMESPACE);
	QualifiedName MERGE_SHADOW_EXTENSIONS = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_MERGE_SHADOW_EXTENSIONS);
	QualifiedName SELF_HEAL_ON_IMPORT = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_SELF_HEAL_ON_IMPORT);
	QualifiedName INSTANCE_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_INSTANCE_NAMESPACE);
	QualifiedName PRESERVE_NAMESPACES = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PRESERVE_NAMESPACES);
	QualifiedName USE_PACKAGE_NAMES = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_USE_PACKAGE_NAMES);
	QualifiedName PROBLEM_PER_SUBJECT = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PROBLEM_PER_SUBJECT);
	QualifiedName PROFILE_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PROFILE_NAMESPACE);
	QualifiedName PROFILE_ENVELOPE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PROFILE_ENVELOPE);
	QualifiedName MAPPING_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_MAPPING_NAMESPACE);
	QualifiedName MAPPING_LABEL = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_MAPPING_LABEL);

	/**
	 * PlantUML preferences below. When adding a new QualifiedName to the list here
	 * be sure to also add the new entry to the Info.getPlantUMLParameters() method
	 * to be passed along to the ProfileSerializer class.
	 */
	QualifiedName PLANTUML_THEME = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PLANTUML_THEME);
	QualifiedName DOCROOT_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_DOCROOT_CLASSES_COLOR);
	QualifiedName CONCRETE_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_CONCRETE_CLASSES_COLOR);
	QualifiedName ABSTRACT_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_ABSTRACT_CLASSES_COLOR);
	QualifiedName ENUMERATIONS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_ENUMERATIONS_COLOR);
	QualifiedName CIMDATATYPES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_CIMDATATYPES_COLOR);
	QualifiedName COMPOUNDS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_COMPOUNDS_COLOR);
	QualifiedName PRIMITIVES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_PRIMITIVES_COLOR);
	QualifiedName CHOICES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_CHOICES_COLOR);
	QualifiedName REFS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_REFS_COLOR);
	QualifiedName SHADOW_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_SHADOW_CLASSES_COLOR);
	QualifiedName ERRORS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_ERRORS_COLOR);
	QualifiedName ANONYMOUS_CLASSES_COLOR_WHITE = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			PREF_ANONYMOUS_CLASSES_COLOR_WHITE);
	QualifiedName ENABLE_DARK_MODE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_ENABLE_DARK_MODE);
	QualifiedName ENABLE_SHADOWING = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_ENABLE_SHADOWING);
	QualifiedName HIDE_ENUMERATIONS = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_HIDE_ENUMERATIONS);
	QualifiedName HIDE_CIMDATATYPES = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_HIDE_CIMDATATYPES);
	QualifiedName HIDE_COMPOUNDS = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_HIDE_COMPOUNDS);
	QualifiedName HIDE_PRIMITIVES = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_HIDE_PRIMITIVES);
	QualifiedName HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			PREF_HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES);
	QualifiedName HORIZONTAL_SPACING = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_HORIZONTAL_SPACING);
	QualifiedName VERTICAL_SPACING = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_VERTICAL_SPACING);
	QualifiedName CURRENT_PROFILE_PREVIEW_STYLE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, PREF_CURRENT_PROFILE_PREVIEW_STYLE);

	/** All global preference symbols */
	QualifiedName[] PREFERENCES_SYMBOLS = new QualifiedName[] { //
			PROFILE_PATH, //
			BASE_MODEL_PATH, //
			MERGED_SCHEMA_PATH, //
			//
			SCHEMA_NAMESPACE, //
			MERGE_SHADOW_EXTENSIONS, //
			SELF_HEAL_ON_IMPORT, //
			INSTANCE_NAMESPACE, //
			PRESERVE_NAMESPACES, //
			USE_PACKAGE_NAMES, //
			PROBLEM_PER_SUBJECT, PROFILE_NAMESPACE, //
			PROFILE_ENVELOPE, //
			MAPPING_NAMESPACE, //
			MAPPING_LABEL, //
			//
			// PlantUML specific preferences
			PLANTUML_THEME, //
			DOCROOT_CLASSES_COLOR, //
			CONCRETE_CLASSES_COLOR, //
			ABSTRACT_CLASSES_COLOR, //
			ENUMERATIONS_COLOR, //
			CIMDATATYPES_COLOR, //
			COMPOUNDS_COLOR, //
			PRIMITIVES_COLOR, //
			CHOICES_COLOR, //
			REFS_COLOR, //
			SHADOW_CLASSES_COLOR, //
			ERRORS_COLOR, //
			ANONYMOUS_CLASSES_COLOR_WHITE, //
			ENABLE_DARK_MODE, //
			ENABLE_SHADOWING, //
			HIDE_ENUMERATIONS, //
			HIDE_CIMDATATYPES, //
			HIDE_COMPOUNDS, //
			HIDE_PRIMITIVES, //
			HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES, //
			HORIZONTAL_SPACING, //
			VERTICAL_SPACING, //
			CURRENT_PROFILE_PREVIEW_STYLE //
	};

}
