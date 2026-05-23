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
import au.com.langdale.cimtoole.project.GlobalPreferencesExporter;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;

public class PlantUMLBuildersPreferencesPage extends FurnishedPropertyPage {

	public PlantUMLBuildersPreferencesPage() {
		setPreferenceStore(CIMToolPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(Group(Label("PlantUML Diagram Preferences:")),
						Group(new PreferenceOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")),
						Group(new PreferenceOption(Info.HIDE_CIMDATATYPES, "Hide CIMDatatype classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_COMPOUNDS, "Hide Compound classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_PRIMITIVES, "Hide Primitive classes in diagrams")),
						Group(new PreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
								"Hide cardinality for required attributes")),
						Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Horizontal spacing:"),
								new Preference(Info.HORIZONTAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
						Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Vertical spacing:"),
								new Preference(Info.VERTICAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
						Group(HRule()),
						Group(Label("PlantUML Theme:"),
								new PreferenceReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE, Info.themes)),
						Group(HRule()), Group(Label("Custom Theme (only applicable when PlantUML Theme is _none_ ):")),
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
						Group(Label("Choices Color:"), new ColorPreference("", Info.CHOICES_COLOR),
								Label(Info.CHOICES_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.CHOICES_COLOR.getLocalName()))),
						Group(Label("Refs Color:"), new ColorPreference("", Info.REFS_COLOR),
								Label(Info.REFS_COLOR.getLocalName() + "-label",
										getPreferenceStore().getString(Info.REFS_COLOR.getLocalName()))),
						Group(new PreferenceOption(Info.ANONYMOUS_CLASSES_COLOR_WHITE,
								"Set the color of anonymous classes to white")),
						Group(new PreferenceOption(Info.ENABLE_DARK_MODE,
								"Enable 'Dark Mode' (overrides colors above)")),
						Group(new PreferenceOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes")),
						Group(HRule()), Group(Label("Real-Time Profile Preview PlantUML Diagram Preferences:")), //
						Group(Label("Diagram Style: "),
								new PreferenceReadOnlyCombo(Info.CURRENT_PROFILE_PREVIEW_STYLE, Validators.NONE,
										ProfileBuildletConfigUtils.getPlantUMLBuildletStyles())),
						Group(Label("(Specifies the global default PlantUML style to be used for real-time previews.")),
						Group(Label("This value may be overridden at either the project-level or profile-level.)")));
			};
		};
	}

	@Override
	public boolean performOk() {
		boolean result = super.performOk();

		if (result) {
			GlobalPreferencesExporter.exportGlobalPreferencesToAllOpenProjects();
		}

		return result;
	}

}