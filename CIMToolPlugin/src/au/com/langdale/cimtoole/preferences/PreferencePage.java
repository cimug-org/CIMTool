/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.preferences;

import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.FurnishedPropertyPage;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;

public class PreferencePage extends FurnishedPropertyPage {

	public PreferencePage() {
		setPreferenceStore(CIMToolPlugin.getDefault().getPreferenceStore());
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(
						Group(
								Label("Default Schema Namespace:"), 
								new Preference(Info.SCHEMA_NAMESPACE, Validators.NAMESPACE)),
						Group(
								Label("Default Instance Namespace:"),
								new Preference(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(
								Label("Default Profile Namespace:"),
								new Preference(Info.PROFILE_NAMESPACE, Validators.NAMESPACE)),
						Group(
								Label("Default Envelope Element Name:"),
								new Preference(Info.PROFILE_ENVELOPE, Validators.NCNAME)),
						Group( 
								new PreferenceOption(Info.PRESERVE_NAMESPACES, 
								"Preserve schema namespaces in profiles")),
						Group( 
								new PreferenceOption(Info.USE_PACKAGE_NAMES, 
								"Add package names to default schema namespace")),
						Group( 
								new PreferenceOption(Info.PROBLEM_PER_SUBJECT, 
								"Limit validation output by subject and message type")),
						Group(  
								HRule()),
						Group(  
								Label("PlantUML Diagram Preferences:")),
						Group( 
								Label("Document Root Class Color:"),
								new ColorPreference("", Info.DOCROOT_CLASSES_COLOR),
								Label(Info.DOCROOT_CLASSES_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.DOCROOT_CLASSES_COLOR.getLocalName()))),
						Group( 
								Label("Concrete Classes Color:"),
								new ColorPreference("", Info.CONCRETE_CLASSES_COLOR),
								Label(Info.CONCRETE_CLASSES_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.CONCRETE_CLASSES_COLOR.getLocalName()))),
						Group( 
								Label("Abstract Classes Color:"),
								new ColorPreference("", Info.ABSTRACT_CLASSES_COLOR),
								Label(Info.ABSTRACT_CLASSES_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.ABSTRACT_CLASSES_COLOR.getLocalName()))),
						Group( 
								Label("Enumerations Color:"),
								new ColorPreference("", Info.ENUMERATIONS_COLOR),
								Label(Info.ENUMERATIONS_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.ENUMERATIONS_COLOR.getLocalName()))),
						Group( 
								Label("CIMDatatypes Color:"),
								new ColorPreference("", Info.CIMDATATYPES_COLOR),
								Label(Info.CIMDATATYPES_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.CIMDATATYPES_COLOR.getLocalName()))),
						Group( 
								Label("Compounds Color:"),
								new ColorPreference("", Info.COMPOUNDS_COLOR),
								Label(Info.COMPOUNDS_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.COMPOUNDS_COLOR.getLocalName()))),
						Group( 
								Label("Primitives Color:"),
								new ColorPreference("", Info.PRIMITIVES_COLOR),
								Label(Info.PRIMITIVES_COLOR.getLocalName() + "-label", getPreferenceStore().getString(Info.PRIMITIVES_COLOR.getLocalName()))),
						Group( 
								new PreferenceOption(Info.ENABLE_DARK_MODE, 
								"Enable 'Dark Mode' (overrides colors above)")),
						Group( 
								new PreferenceOption(Info.ENABLE_SHADOWING, 
								"Enable shadowing on classes")),
						Group( 
								new PreferenceOption(Info.HIDE_ENUMERATIONS, 
								"Hide enumerations in diagrams")),
						Group( 
								new PreferenceOption(Info.HIDE_CIMDATATYPES, 
								"Hide CIMDatatype classes in diagrams")),
						Group( 
								new PreferenceOption(Info.HIDE_COMPOUNDS, 
								"Hide Compound classes in diagrams")),
						Group( 
								new PreferenceOption(Info.HIDE_PRIMITIVES, 
								"Hide Primitive classes in diagrams")),	
						Group( 
								new PreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES, 
								"Hide cardinality for required attributes"))
						);
			}
		};
	}
    
}