/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.properties;

import static au.com.langdale.ui.builder.Templates.DisplayField;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.SchemaBuildlet;
import au.com.langdale.cimtoole.project.FurnishedPropertyPage;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;

/**
 * The <b>CIMTool</b> property page, contributed for every resource. The
 * PlantUML settings of a project, a profile or a generated <code>.puml</code>
 * file are on its <b>PlantUML Builder Preferences</b> child page
 * ({@link PlantUMLBuilderPropertyPage}). For a profile and a generated
 * <code>.puml</code> file this page is read-only.
 */
public class PropertyPage extends FurnishedPropertyPage {

	private static final String LOCATION = "location";

	public PropertyPage() {
		setPreferenceStore(CIMToolPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * True when this page carries no editable settings for the resource, so the
	 * Restore Defaults and Apply buttons are not shown.
	 */
	private boolean isReadOnly(IResource resource) {
		return !(Info.isSchema(resource) || Info.isInstance(resource) || Info.isSplitInstance(resource)
				|| Info.isIncremental(resource) || resource instanceof IProject);
	}

	@Override
	protected Content createContent() {
		if (isReadOnly(getResource()))
			noDefaultAndApplyButton();

		return new Content() {

			@Override
			protected Template define() {
				IResource resource = getResource();
				if (Info.isPlantUML(resource))
					return definePlantUMLPage();
				else if (Info.isProfile(resource))
					return defineProfilePage();
				else if (Info.isSchema(resource))
					return defineSchemaPage();
				else if (Info.isInstance(resource) || Info.isSplitInstance(resource))
					return defineInstancePage();
				else if (Info.isIncremental(resource))
					return defineIncrementalPage();
				else if (resource instanceof IProject)
					return defineProjectPage();
				else if (isAsciiDoc(resource))
					return Grid(Group(Label("No properties or preferences available for this resource.")),
							Group(Label("Diagrams that this document includes from a generated .puml file take their")),
							Group(Label("settings from that file. Set them on that file's Properties page.")));
				else
					return Label("No properties or preferences available for this resource.");
			}

			@Override
			protected void addBindings() {
				IResource resource = getResource();
				if (Info.isProfile(resource) || Info.isPlantUML(resource))
					setTextValue(LOCATION, resource.getFullPath().toString());
			}

			private boolean isAsciiDoc(IResource resource) {
				return resource instanceof IFile && "adoc".equals(resource.getFileExtension())
						&& resource.getParent().equals(Info.getProfilesFolder(resource.getProject()));
			}

			private Template defineProfilePage() {
				return Grid(Group(Label("Location:"), DisplayField(LOCATION)), //
						Group(Label("The profile's namespace is set on the Summary tab of the Profile Editor.")),
						Group(Label("This profile's diagram settings are under PlantUML Builder Preferences.")));
			}

			private Template definePlantUMLPage() {
				return Grid(Group(Label("Location:"), DisplayField(LOCATION)), //
						Group(Label("This diagram's settings are under PlantUML Builder Preferences.")));
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
				Boolean enabled = Info.isMergeShadowExtensionsEnabled(getResource());
				return Grid(Group(Label("Merged Schema Output")),
						Group(Label("File Name:"),
								new Property(Info.MERGED_SCHEMA_PATH, Validators.OptionalFileWithExt("merged-owl"))),
						Group(Label("")), //
						Group(HRule()), //
						Group(new PropertyOption(Info.SELF_HEAL_ON_IMPORT,
								"Enable self-healing when importing EA projects (.eap, .qea, etc.) as schema.")),
						Group(HRule()), //
						Group(new PropertyOption(Info.MERGE_SHADOW_EXTENSIONS,
								"During import merge shadow/mixin extensions into the CIM classes that inherit from them")),
						Group(Label("NOTE: ")),
						Group(Label("This project was created with the \"merge shadow/mixin extensions setting\" "
								+ (enabled ? "enabled" : "disabled") + ".")),
						Group(Label(
								"If you change the above setting you should immediately reimport the project's schema file. ")),
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
					} else if (Info.isProfile(resource) || Info.isPlantUML(resource)) {
						// Read-only here. Diagram settings are applied, and their views
						// refreshed, by PlantUMLBuilderPropertyPage.
					} else {
						resource.touch(null);
					}
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
