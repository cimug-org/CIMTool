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
	
	/*
	protected class PlantUMLLabel extends Label { 
		
		public PlantUMLLabel("", "") {
			super(parent, background);
		}
		
	    public byte[] generatePlantUMLImage(String plantUMLSource) throws IOException {
	    	// Example PlantUML code
	        String plantUMLSource = "@startuml\nAlice -> Bob: Hello\n@enduml";

	        try {
	            // Generate PNG image from PlantUML source
	            byte[] pngImage;
	            
	            // Create a SourceStringReader from the PlantUML source
		        SourceStringReader reader = new SourceStringReader(plantUMLSource);

		        // Use ByteArrayOutputStream to capture the generated PNG
		        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		        reader.generateImage(outputStream);

		        // Return the PNG image as byte array
		        pngImage =  outputStream.toByteArray();

	            // Now, you can display the image in an SWT UI component
		        Image image = new Image(display, new org.eclipse.io.BytesInputStream(pngImage));
		        setImage(image);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        
	        
	       
	    }
	}
	*/
	
	public PreferencePage() {
		setPreferenceStore(CIMToolPlugin.getDefault().getPreferenceStore());
	}	

	@Override
	protected Content createContent() {
		return new Content() {

			/**
			 * TODO: This is saved off temporarily until the PlantUML preview image is completed.
			@Override
			protected Template define() {
				return Grid(
						Group(Label("Default Schema Namespace:"),
								new Preference(Info.SCHEMA_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Default Instance Namespace:"),
								new Preference(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Default Profile Namespace:"),
								new Preference(Info.PROFILE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Default Envelope Element Name:"),
								new Preference(Info.PROFILE_ENVELOPE, Validators.NCNAME)),
						Group(new PreferenceOption(Info.PRESERVE_NAMESPACES, "Preserve schema namespaces in profiles")),
						Group(new PreferenceOption(Info.USE_PACKAGE_NAMES,
								"Add package names to default schema namespace")),
						Group(new PreferenceOption(Info.PROBLEM_PER_SUBJECT,
								"Limit validation output by subject and message type")),
						Group(HRule()),
						Group(Label("PlantUML Diagram Preferences:")),
						Group(new PreferenceOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")),
						Group(new PreferenceOption(Info.HIDE_CIMDATATYPES, "Hide CIMDatatype classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_COMPOUNDS, "Hide Compound classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_PRIMITIVES, "Hide Primitive classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
								"Hide cardinality for all required attributes")),
						Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Custom horizontal spacing:"), 
								new Preference(Info.HORIZONTAL_SPACING, Validators.NON_NEGATIVE_INTEGER)), 
						Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Custom vertical spacing:"), 
								new Preference(Info.VERTICAL_SPACING, Validators.NON_NEGATIVE_INTEGER)),
						Group(HRule()),
						Group(Label("Predefined PlantUML Themes:"), 
								new PreferenceReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE, themes)),
						Group(HRule()),
						Group(Label("Custom Theme (only applicable when PlantUML Theme is '_none_' ):")),
						Group(
							Grid(
								Group(Label("Document Root:"), new ColorPreference("", Info.DOCROOT_CLASSES_COLOR)),
								Group(Label("Concrete Classes:"), new ColorPreference("", Info.CONCRETE_CLASSES_COLOR)),
								Group(Label("Abstract Classes:"), new ColorPreference("", Info.ABSTRACT_CLASSES_COLOR)),
								Group(Label("Enumerations:"), new ColorPreference("", Info.ENUMERATIONS_COLOR)),
								Group(Label("CIMDatatypes:"), new ColorPreference("", Info.CIMDATATYPES_COLOR)),
								Group(Label("Compounds:"), new ColorPreference("", Info.COMPOUNDS_COLOR)),
								Group(Label("Primitives:"), new ColorPreference("", Info.PRIMITIVES_COLOR))
							), 
							Grid(Group(Image("theme", "theme-example")))
							//Grid(Group(Image("theme example", "")))
						),
						Group(new PreferenceOption(Info.ENABLE_DARK_MODE,
								"Enable 'Dark Mode' (overrides colors above)")),
						Group(new PreferenceOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes"))
					);
			}
		};
		*/
		

			@Override
			protected Template define() {
				return Grid(
						Group(Label("Default Schema Namespace:"),
								new Preference(Info.SCHEMA_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Default Instance Namespace:"),
								new Preference(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Default Profile Namespace:"),
								new Preference(Info.PROFILE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Default Envelope Element Name:"),
								new Preference(Info.PROFILE_ENVELOPE, Validators.NCNAME)),
						Group(new PreferenceOption(Info.PRESERVE_NAMESPACES, "Preserve schema namespaces in profiles")),
						Group(new PreferenceOption(Info.USE_PACKAGE_NAMES,
								"Add package names to default schema namespace")),
						Group(new PreferenceOption(Info.PROBLEM_PER_SUBJECT,
								"Limit validation output by subject and message type")),
						Group(new PreferenceOption(Info.MERGE_SHADOW_EXTENSIONS, 
								"Merge shadow class extensions into their normative CIM classes")),
						Group(new PreferenceOption(Info.SELF_HEAL_ON_IMPORT, 
								"Enable self-healing when importing EA projects (.eap or .qeap) as schema")),
						Group(HRule()),
						Group(Label("PlantUML Diagram Preferences:")),
						Group(new PreferenceOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")),
						Group(new PreferenceOption(Info.HIDE_CIMDATATYPES, "Hide CIMDatatype classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_COMPOUNDS, "Hide Compound classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_PRIMITIVES, "Hide Primitive classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
								"Hide cardinality for required attributes")),
						Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Custom horizontal spacing:"), 
								new Preference(Info.HORIZONTAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)), 
						Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Custom vertical spacing:"), 
								new Preference(Info.VERTICAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
						Group(HRule()),
						Group(Label("PlantUML Theme:"), 
								new PreferenceReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE, Info.themes)),
						Group(HRule()),
						Group(Label("Custom Theme (only applicable when PlantUML Theme is _none_ ):")),
						Group(Label("Document Root Class Color:"), new ColorPreference("", Info.DOCROOT_CLASSES_COLOR),
								Label(Info.DOCROOT_CLASSES_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.DOCROOT_CLASSES_COLOR.getLocalName()))),
						Group(Label("Concrete Classes Color:"), new ColorPreference("", Info.CONCRETE_CLASSES_COLOR),
								Label(Info.CONCRETE_CLASSES_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.CONCRETE_CLASSES_COLOR.getLocalName()))),
						Group(Label("Abstract Classes Color:"), new ColorPreference("", Info.ABSTRACT_CLASSES_COLOR),
								Label(Info.ABSTRACT_CLASSES_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.ABSTRACT_CLASSES_COLOR.getLocalName()))),
						Group(Label("Enumerations Color:"), new ColorPreference("", Info.ENUMERATIONS_COLOR),
								Label(Info.ENUMERATIONS_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.ENUMERATIONS_COLOR.getLocalName()))),
						Group(Label("CIMDatatypes Color:"), new ColorPreference("", Info.CIMDATATYPES_COLOR),
								Label(Info.CIMDATATYPES_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.CIMDATATYPES_COLOR.getLocalName()))),
						Group(Label("Compounds Color:"), new ColorPreference("", Info.COMPOUNDS_COLOR),
								Label(Info.COMPOUNDS_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.COMPOUNDS_COLOR.getLocalName()))),
						Group(Label("Primitives Color:"), new ColorPreference("", Info.PRIMITIVES_COLOR),
								Label(Info.PRIMITIVES_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.PRIMITIVES_COLOR.getLocalName()))),						
						Group(new PreferenceOption(Info.ENABLE_DARK_MODE,
								"Enable 'Dark Mode' (overrides colors above)")),
						Group(new PreferenceOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes")));
			}
			
		};
	}
}