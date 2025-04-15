/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.properties;

import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.SchemaBuildlet;
import au.com.langdale.cimtoole.project.FurnishedPropertyPage;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;

public class PropertyPage extends FurnishedPropertyPage {

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				IResource resource = getResource();
				if(Info.isPlantUML(resource))
					return definePlantUMLPage();
				else if(Info.isSchema(resource ))
					return defineSchemaPage();
				else if(Info.isInstance(resource) || Info.isSplitInstance(resource))
					return defineInstancePage();
				else if(Info.isIncremental(resource))
					return defineIncrementalPage();
				else if(resource instanceof IProject)
					return defineProjectPage();
				else
					return Label("No properties available for this resource.");
			}

			private Template defineInstancePage() {
				return Grid(
						Group(Label("Namespace URI:"), 
								new Property(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Profile Name:"), 
								new Property(Info.PROFILE_PATH, Validators.OptionalFileWithExt("owl")))
				);
			}

			private Template defineIncrementalPage() {
				return Grid(
						Group(Label("Namespace URI:"), 
								new Property(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Base Model Name:"), 
								new Property(Info.BASE_MODEL_PATH, Validators.OptionalFileAnyExt()))
				);
			}

			private Template defineProjectPage() {
				Boolean enabled = Info.isMergeShadowExtensionsEnabled(getResource());
				return Grid(
						Group(Label("Merged Schema Output")),
						Group(Label("File Name:"), 
								new Property(Info.MERGED_SCHEMA_PATH, Validators.OptionalFileWithExt("merged-owl"))),
						Group(Label("")),
						Group(new PropertyOption(Info.SELF_HEAL_ON_IMPORT, "Enable self-healing when importing EA projects (.eap or .qeap) as schema.")),
						Group(HRule()),
						Group(Label("")),
						Group(new PropertyOption(Info.MERGE_SHADOW_EXTENSIONS, "During schema imports merge shadow class extensions into the CIM classes they shadow")),
						Group(Label("")),
						Group(Label("NOTE: ")),
						Group(Label("This project was created with the \"merge shadow class extensions setting\" " + (enabled ? "enabled" : "disabled") + ".")),
						Group(Label("If you change the above setting you should immediatly reimport the project's schema file. ")),
						Group(Label("")),
						Group(Label("The above setting should always be enabled when the schema format you are using is an ")),
						Group(Label(".eap, .eapx, .feap, .qea or .qeap project file. ")),
						Group(Label("")),
						Group(Label("If using multiple .xmi files as your project schemas with one containing your base CIM model ")),
						Group(Label("and one or more .xmi file(s) each containing a set of extensions associated with their own ")),
						Group(Label("namspaces then uncheck this setting. This is provided primarily for legacy projects created")),
						Group(Label("in much older releases of CIMTool.")),
						Group(Label("")),
						Group(Label("Finally, there may be specialized projects that may need this setting disabled in order to")),
						Group(Label("allow for shadow class extensions to be left unmerged and available for inclusion in the ")),
						Group(Label("generated artifacts. Example artifacts could include inline asciidoc documentation or UML ")),
						Group(Label("class diagrams to be used within such documentation.")),
						Group(Label(""))
				);
			}

			private Template definePlantUMLPage() {
				return Grid(
						Group(Label("PlantUML Diagram Preferences:")),
						//Group(HRule()),
						Group(new PropertyOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")),
						Group(new PropertyOption(Info.HIDE_CIMDATATYPES, "Hide CIMDatatype classes in diagrams")),
						Group(new PropertyOption(Info.HIDE_COMPOUNDS, "Hide Compound classes in diagrams")),
						Group(new PropertyOption(Info.HIDE_PRIMITIVES, "Hide Primitive classes in diagrams")),
						Group(new PropertyOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
								"Hide cardinality for required attributes")),
						Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Custom horizontal spacing:"), 
								new Property(Info.HORIZONTAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)), 
						Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Custom vertical spacing:"), 
								new Property(Info.VERTICAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)),
						Group(HRule()),
						Group(Label("PlantUML Theme:"), 
								new PropertyReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE, Info.themes)),
						Group(HRule()),
						Group(Label("Custom Theme (only applicable when PlantUML Theme is _none_ ):")),
						Group(Label("Document Root Class Color:"), new ColorProperty("", Info.DOCROOT_CLASSES_COLOR),
								Label(Info.DOCROOT_CLASSES_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.DOCROOT_CLASSES_COLOR))),
						Group(Label("Concrete Classes Color:"), new ColorProperty("", Info.CONCRETE_CLASSES_COLOR),
								Label(Info.CONCRETE_CLASSES_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.CONCRETE_CLASSES_COLOR))),
						Group(Label("Abstract Classes Color:"), new ColorProperty("", Info.ABSTRACT_CLASSES_COLOR),
								Label(Info.ABSTRACT_CLASSES_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.ABSTRACT_CLASSES_COLOR))),
						Group(Label("Enumerations Color:"), new ColorProperty("", Info.ENUMERATIONS_COLOR),
								Label(Info.ENUMERATIONS_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.ENUMERATIONS_COLOR))),
						Group(Label("CIMDatatypes Color:"), new ColorProperty("", Info.CIMDATATYPES_COLOR),
								Label(Info.CIMDATATYPES_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.CIMDATATYPES_COLOR))),
						Group(Label("Compounds Color:"), new ColorProperty("", Info.COMPOUNDS_COLOR),
								Label(Info.COMPOUNDS_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.COMPOUNDS_COLOR))),
						Group(Label("Primitives Color:"), new ColorProperty("", Info.PRIMITIVES_COLOR),
								Label(Info.PRIMITIVES_COLOR.getLocalName() + "-label",
										Info.getPropertyNoException(getResource(), Info.PRIMITIVES_COLOR))),						
						Group(new PropertyOption(Info.ENABLE_DARK_MODE,
								"Enable 'Dark Mode' (overrides colors above)")),
						Group(new PropertyOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes")));
			}

			private Template defineSchemaPage() {
				return Grid(
						Group(Label("Namespace URI:"), 
								new Property(Info.SCHEMA_NAMESPACE, Validators.NAMESPACE)),
						Group(Label("Warning: changing this namespace will affect existing profiles."))
				);
			}
			
			@Override
			public void update() {
				try {
					IResource resource = getResource();
					if(resource instanceof IProject) {
						String merged = Info.getProperty(resource, Info.MERGED_SCHEMA_PATH);
						if( merged != null && merged.length() != 0) {
							IFile file = resource.getProject().getFile(merged);
							Jobs.runJob(new SchemaBuildlet().asRunnable(file, false), resource, "Generating merged OWL");
						}
					} else if(Info.isPlantUML(resource)) {
						IFolder profileFolder = Info.getProfileFolder(resource.getProject());
						IFile file = profileFolder.getFile(resource.getName());
						String extension = file.getName().substring(file.getName().indexOf(".") + 1);
						TransformBuildlet buildlet = ProfileBuildletConfigUtils.getTransformBuildletForExtension(extension);
						IWorkspaceRunnable runnable = buildlet.asRunnable(file, false);
						Jobs.runJob(runnable, profileFolder, "Regenerating PlantUML diagram");
					}
					else {
						resource.touch(null);
					}
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
