/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.properties;

import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;

import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.PlantUMLRealTimePreviewBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.SchemaBuildlet;
import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.project.FurnishedPropertyPage;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;

public class PropertyPage extends FurnishedPropertyPage {

	public class DisplayStyleReadOnlyCombo extends ReadOnlyComboBinding {

		public DisplayStyleReadOnlyCombo(QualifiedName symbol, Validator validator, Object data,
				Object initialSelection) {
			super(symbol, validator, data, initialSelection);
		}

		public void refresh() {
			setValue(Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(getResource()));
		}

		public void update() {
			IResource resource = getResource();
			String value = (getValue() == null ? "" : getValue());
			if (resource instanceof IProject) {
				Info.putProperty(getResource(), symbol, value);
			} else if (Info.isProfile(resource) || Info.isPlantUML(resource)) {
				Info.putBuilderPreference(getResource(), symbol, value);
			} else {
				throw new RuntimeException("Invalid entry...");
			}
		}

		public void reset() {
			Object value = Info.getCurrentProfileRealTimePreviewDisplayStyleByLevel(getResource());
			setValue(value);
		}

	}

	public PropertyPage() {
		setPreferenceStore(CIMToolPlugin.getDefault().getPreferenceStore());
	}

	private LinkedList<String> getPlantUMLBuildetStyles() {
		LinkedList<String> styles = new LinkedList<String>(ProfileBuildletConfigUtils.getPlantUMLBuildletStyles());
		styles.add(0, "");
		return styles;
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				IResource resource = getResource();
				if (Info.isPlantUML(resource))
					return definePlantUMLPage(false);
				else if (Info.isProfile(resource))
					return definePlantUMLPage(true);
				else if (Info.isSchema(resource))
					return defineSchemaPage();
				else if (Info.isInstance(resource) || Info.isSplitInstance(resource))
					return defineInstancePage();
				else if (Info.isIncremental(resource))
					return defineIncrementalPage();
				else if (resource instanceof IProject)
					return defineProjectPage();
				else
					return Label("No properties or preferences available for this resource.");
			}

			private Template defineInstancePage() {
				return Grid(Group(Label("Namespace URI:"), new Property(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Profile Name:"),
								new Property(Info.PROFILE_PATH, Validators.OptionalFileWithExt("owl"))));
			}

			private Template defineIncrementalPage() {
				return Grid(Group(Label("Namespace URI:"), new Property(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Base Model Name:"),
								new Property(Info.BASE_MODEL_PATH, Validators.OptionalFileAnyExt())));
			}

			private Template defineProjectPage() {
				String displayStyleInitialSetting = Info
						.getCurrentProfileRealTimePreviewDisplayStyleByLevel(getResource());
				Boolean enabled = Info.isMergeShadowExtensionsEnabled(getResource());
				return Grid(Group(Label("Merged Schema Output")),
						Group(Label("File Name:"),
								new Property(Info.MERGED_SCHEMA_PATH, Validators.OptionalFileWithExt("merged-owl"))),
						Group(Label("")), //
						Group(HRule()), //
						Group(Label("Real-Time Profile Preview PlantUML Diagram Preferences:")), //
						Group(Label("Diagram Style: "),
								new DisplayStyleReadOnlyCombo(Info.CURRENT_PROFILE_PREVIEW_STYLE, Validators.NONE,
										getPlantUMLBuildetStyles(), displayStyleInitialSetting)),
						Group(Label(
								"(Specifies a project-level default PlantUML style for real-time previews. If left empty will")),
						Group(Label(
								"default to the global default value for this preference setting unless overridden at the")),
						Group(Label("individual profile-level.)")), Group(HRule()), //
						Group(new PropertyOption(Info.SELF_HEAL_ON_IMPORT,
								"Enable self-healing when importing EA projects (.eap, .qea, etc.) as schema.")),
						Group(HRule()), //
						Group(new PropertyOption(Info.MERGE_SHADOW_EXTENSIONS,
								"During import merge shadow/mixin extensions into the CIM classes that inherit from them")),
						Group(Label("NOTE: ")),
						Group(Label("This project was created with the \"merge shadow/mixin extensions setting\" "
								+ (enabled ? "enabled" : "disabled") + ".")),
						Group(Label(
								"If you change the above setting you should immediatly reimport the project's schema file. ")),
						Group(Label("")),
						Group(Label(
								"The above setting should almost always be enabled when the schema format you are using ")),
						Group(Label(
								"is an .eap, .eapx, .qea or .qeax project file. If using .xmi files as your project schemas then ")),
						Group(Label("uncheck this setting.")), Group(Label("")),
						Group(Label(
								"Finally, there may be specialized projects that may need this setting disabled in order to")),
						Group(Label(
								"allow for shadow class extensions to be left unmerged and available for inclusion in the ")),
						Group(Label(
								"generated artifacts. Example artifacts could include inline asciidoc documentation or UML ")),
						Group(Label("class diagrams to be used within such documentation.")));
			}

			/**
			 * Below is an example of a Preferences page for a builder that can be
			 * overridden from the Global Preferences screen. These builder preferences get
			 * passed as parameters to the specific builder that they are associated with.
			 * Note that for each builder-specific preference that one of the Builder* type
			 * fields is used and not the Property related fields.
			 */
			private Template definePlantUMLPage(boolean isProfileLevel) {
				String displayStyleInitialSetting = Info
						.getCurrentProfileRealTimePreviewDisplayStyleByLevel(getResource());
				Template plantUMLTemplate = null;
				if (isProfileLevel) {
					plantUMLTemplate = Grid(Group(Label("Real-Time Profile Preview PlantUML Diagram Preferences:")), //
							Group(Label("Diagram Style: "),
									new DisplayStyleReadOnlyCombo(Info.CURRENT_PROFILE_PREVIEW_STYLE, Validators.NONE,
											getPlantUMLBuildetStyles(), displayStyleInitialSetting)),
							Group(Label(
									"(Specifies a profile-level PlantUML style for real-time previews. If left empty will default")),
							Group(Label(
									"to the project-level default value if one is specified. Otherwise CIMTool will use the global")),
							Group(Label("default value for this preference setting.)")), //
							Group(HRule()), //
							Group(new BuilderPreferenceOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_CIMDATATYPES,
									"Hide CIMDatatype classes in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_COMPOUNDS,
									"Hide Compound classes in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_PRIMITIVES,
									"Hide Primitive classes in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
									"Hide cardinality for required attributes")),
							Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Horizontal spacing:"),
									new BuilderPreference(Info.HORIZONTAL_SPACING,
											Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
							Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Vertical spacing:"),
									new BuilderPreference(
											Info.VERTICAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
							Group(HRule()), //
							Group(Label("PlantUML Theme:"),
									new BuilderPreferenceReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE,
											Info.themes)),
							Group(HRule()), //
							Group(Label("Custom Theme (only applicable when PlantUML Theme is _none_ ):")),
							Group(Label("Document Root Class Color:"),
									new BuilderColorPreference("", Info.DOCROOT_CLASSES_COLOR),
									Label(Info.DOCROOT_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.DOCROOT_CLASSES_COLOR))),
							Group(Label("Concrete Classes Color:"),
									new BuilderColorPreference("", Info.CONCRETE_CLASSES_COLOR),
									Label(Info.CONCRETE_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.CONCRETE_CLASSES_COLOR))),
							Group(Label("Abstract Classes Color:"),
									new BuilderColorPreference("", Info.ABSTRACT_CLASSES_COLOR),
									Label(Info.ABSTRACT_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.ABSTRACT_CLASSES_COLOR))),
							Group(Label("Enumerations Color:"), new BuilderColorPreference("", Info.ENUMERATIONS_COLOR),
									Label(Info.ENUMERATIONS_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.ENUMERATIONS_COLOR))),
							Group(Label("CIMDatatypes Color:"), new BuilderColorPreference("", Info.CIMDATATYPES_COLOR),
									Label(Info.CIMDATATYPES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.CIMDATATYPES_COLOR))),
							Group(Label("Compounds Color:"), new BuilderColorPreference("", Info.COMPOUNDS_COLOR),
									Label(Info.COMPOUNDS_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.COMPOUNDS_COLOR))),
							Group(Label("Primitives Color:"), new BuilderColorPreference("", Info.PRIMITIVES_COLOR),
									Label(Info.PRIMITIVES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.PRIMITIVES_COLOR))),
							Group(Label("Choices Color:"), new BuilderColorPreference("", Info.CHOICES_COLOR),
									Label(Info.CHOICES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.CHOICES_COLOR))),
							Group(Label("Refs Color:"), new BuilderColorPreference("", Info.REFS_COLOR),
									Label(Info.REFS_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.REFS_COLOR))),
							Group(Label("Shadow Classes Color:"),
									new BuilderColorPreference("", Info.SHADOW_CLASSES_COLOR),
									Label(Info.SHADOW_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.SHADOW_CLASSES_COLOR))),
							Group(new BuilderPreferenceOption(Info.ANONYMOUS_CLASSES_COLOR_WHITE,
									"Set the color of anonymous classes to white")),
							Group(new BuilderPreferenceOption(Info.ENABLE_DARK_MODE,
									"Enable 'Dark Mode' (overrides themes and colors above)")),
							Group(new BuilderPreferenceOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes")));
				} else {
					plantUMLTemplate = Grid(Group(Label("PlantUML Diagram Preferences:")), //
							Group(new BuilderPreferenceOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_CIMDATATYPES,
									"Hide CIMDatatype classes in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_COMPOUNDS,
									"Hide Compound classes in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_PRIMITIVES,
									"Hide Primitive classes in diagrams")),
							Group(new BuilderPreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
									"Hide cardinality for required attributes")),
							Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Horizontal spacing:"),
									new BuilderPreference(Info.HORIZONTAL_SPACING,
											Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
							Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Vertical spacing:"),
									new BuilderPreference(
											Info.VERTICAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
							Group(HRule()), //
							Group(Label("PlantUML Theme:"),
									new BuilderPreferenceReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE,
											Info.themes)),
							Group(HRule()), //
							Group(Label("Custom Theme (only applicable when PlantUML Theme is _none_ ):")),
							Group(Label("Document Root Class Color:"),
									new BuilderColorPreference("", Info.DOCROOT_CLASSES_COLOR),
									Label(Info.DOCROOT_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.DOCROOT_CLASSES_COLOR))),
							Group(Label("Concrete Classes Color:"),
									new BuilderColorPreference("", Info.CONCRETE_CLASSES_COLOR),
									Label(Info.CONCRETE_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.CONCRETE_CLASSES_COLOR))),
							Group(Label("Abstract Classes Color:"),
									new BuilderColorPreference("", Info.ABSTRACT_CLASSES_COLOR),
									Label(Info.ABSTRACT_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.ABSTRACT_CLASSES_COLOR))),
							Group(Label("Enumerations Color:"), new BuilderColorPreference("", Info.ENUMERATIONS_COLOR),
									Label(Info.ENUMERATIONS_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.ENUMERATIONS_COLOR))),
							Group(Label("CIMDatatypes Color:"), new BuilderColorPreference("", Info.CIMDATATYPES_COLOR),
									Label(Info.CIMDATATYPES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.CIMDATATYPES_COLOR))),
							Group(Label("Compounds Color:"), new BuilderColorPreference("", Info.COMPOUNDS_COLOR),
									Label(Info.COMPOUNDS_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.COMPOUNDS_COLOR))),
							Group(Label("Primitives Color:"), new BuilderColorPreference("", Info.PRIMITIVES_COLOR),
									Label(Info.PRIMITIVES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.PRIMITIVES_COLOR))),
							Group(Label("Choices Color:"), new BuilderColorPreference("", Info.CHOICES_COLOR),
									Label(Info.CHOICES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.CHOICES_COLOR))),
							Group(Label("Refs Color:"), new BuilderColorPreference("", Info.REFS_COLOR),
									Label(Info.REFS_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.REFS_COLOR))),
							Group(Label("Shadow Classes Color:"),
									new BuilderColorPreference("", Info.SHADOW_CLASSES_COLOR),
									Label(Info.SHADOW_CLASSES_COLOR.getLocalName() + "-label",
											Info.getBuilderPreference(getResource(), Info.SHADOW_CLASSES_COLOR))),
							Group(new BuilderPreferenceOption(Info.ANONYMOUS_CLASSES_COLOR_WHITE,
									"Set the color of anonymous classes to white")),
							Group(new BuilderPreferenceOption(Info.ENABLE_DARK_MODE,
									"Enable 'Dark Mode' (overrides themes and colors above)")),
							Group(new BuilderPreferenceOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes")));
				}
				return plantUMLTemplate;
			}

			private Template defineSchemaPage() {
				return Grid(Group(Label("Namespace URI:"), new Property(Info.SCHEMA_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Warning: changing this namespace will affect existing profiles.")));
			}

			@Override
			public void update() {
				try {
					IResource resource = getResource();
					if (resource instanceof IProject) {
						String merged = Info.getProperty(resource, Info.MERGED_SCHEMA_PATH);
						if (merged != null && merged.length() != 0) {
							IFile file = resource.getProject().getFile(merged);
							Jobs.runJob(new SchemaBuildlet().asRunnable(file, false), resource,
									"Generating merged OWL");
						}
						refreshPlantUMLViewParts(resource);
					} else if (Info.isProfile(resource) || Info.isPlantUML(resource)) {
						refreshPlantUMLViewParts(resource);
					} else {
						resource.touch(null);
					}
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}

			private void refreshPlantUMLViewParts(IResource resource) {
				if (resource instanceof IProject) {
					// Project-level preference change: refresh the focused profile editor's
					// preview, but only if that profile lives in this project.
					ProfileEditor.refreshActiveProfilePreview((IProject) resource);
					return;
				}
				IFolder profileFolder = Info.getProfileFolder(resource.getProject());
				if (profileFolder == null || !profileFolder.exists())
					return;
				if (Info.isProfile(resource)) {
					IFile owlFile = (IFile) resource;
					String baseName = owlFile.getName().replaceFirst("\\.[^.]+$", "");
					IFile previewFile = profileFolder
							.getFile("." + baseName + "." + PlantUMLRealTimePreviewBuildlet.PREVIEW_EXT);
					IWorkspaceRunnable runnable = new PlantUMLRealTimePreviewBuildlet().asRunnable(previewFile, false);
					Jobs.runJob(runnable, previewFile.getProject(), "Regenerating real-time preview diagram");
				} else if (Info.isPlantUML(resource)) {
					IFile file = profileFolder.getFile(resource.getName());
					String extension = file.getName().substring(file.getName().indexOf(".") + 1);
					TransformBuildlet buildlet = ProfileBuildletConfigUtils.getTransformBuildletForExtension(extension);
					IWorkspaceRunnable runnable = buildlet.asRunnable(file, false);
					Jobs.runJob(runnable, resource.getProject(), "Regenerating PlantUML diagram");
				}
			}
		};
	}
}