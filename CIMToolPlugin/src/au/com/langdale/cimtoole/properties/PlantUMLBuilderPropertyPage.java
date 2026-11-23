/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.properties;

import static au.com.langdale.ui.builder.Templates.GridArray;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.QualifiedName;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.PlantUMLRealTimePreviewBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.project.FurnishedPropertyPage;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.builder.Templates.GroupTemplate;
import au.com.langdale.util.Jobs;

/**
 * The <b>PlantUML Builder Preferences</b> property page, a child of the
 * <b>CIMTool</b> property page. It is contributed for a CIMTool project, a
 * profile and a generated <code>.puml</code> file (see
 * {@link CIMToolPropertyTester}), and presents the same controls in the same
 * order as the workspace-level PlantUML Builder Preferences page.
 * <p>
 * The project and profile levels end with the Profile Real-Time Preview
 * <b>Diagram Style</b> setting. A generated <code>.puml</code> file does not
 * carry it, because that setting governs the real-time preview of a profile
 * rather than a generated diagram.
 */
public class PlantUMLBuilderPropertyPage extends FurnishedPropertyPage {

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

	public PlantUMLBuilderPropertyPage() {
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
				List<GroupTemplate> groups = new ArrayList<GroupTemplate>();
				addDiagramControls(groups);
				if (resource instanceof IProject) {
					addDiagramStyle(groups,
							"(Specifies a project-level default PlantUML style for real-time previews. If left empty will",
							"default to the global default value for this preference setting unless overridden at the",
							"individual profile-level.)");
				} else if (Info.isProfile(resource)) {
					addDiagramStyle(groups,
							"(Specifies a profile-level PlantUML style for real-time previews. If left empty will default",
							"to the project-level default value if one is specified. Otherwise CIMTool will use the global",
							"default value for this preference setting.)");
				}
				return GridArray(groups.toArray(new GroupTemplate[groups.size()]));
			}

			/**
			 * The controls common to every level, in the order of the workspace-level
			 * PlantUML Builder Preferences page.
			 */
			private void addDiagramControls(List<GroupTemplate> groups) {
				groups.add(Group(Label("PlantUML Diagram Preferences:")));
				groups.add(Group(new BuilderPreferenceOption(Info.HIDE_ENUMERATIONS, "Hide enumerations in diagrams")));
				groups.add(Group(
						new BuilderPreferenceOption(Info.HIDE_CIMDATATYPES, "Hide CIMDatatype classes in diagrams")));
				groups.add(
						Group(new BuilderPreferenceOption(Info.HIDE_COMPOUNDS, "Hide Compound classes in diagrams")));
				groups.add(
						Group(new BuilderPreferenceOption(Info.HIDE_PRIMITIVES, "Hide Primitive classes in diagrams")));
				groups.add(Group(new BuilderPreferenceOption(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES,
						"Hide cardinality for required attributes")));
				groups.add(Group(Label(Info.HORIZONTAL_SPACING.getLocalName() + "-label", "Horizontal spacing:"),
						new BuilderPreference(Info.HORIZONTAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)));
				groups.add(Group(Label(Info.VERTICAL_SPACING.getLocalName() + "-label", "Vertical spacing:"),
						new BuilderPreference(Info.VERTICAL_SPACING, Validators.OPTIONAL_NON_NEGATIVE_INTEGER)));
				groups.add(Group(HRule()));
				groups.add(Group(Label("PlantUML Theme:"),
						new BuilderPreferenceReadOnlyCombo(Info.PLANTUML_THEME, Validators.NONE, Info.themes)));
				groups.add(Group(HRule()));
				groups.add(Group(Label("Custom Theme (only applicable when PlantUML Theme is _none_ ):")));
				groups.add(color("Document Root Class Color:", Info.DOCROOT_CLASSES_COLOR));
				groups.add(color("Concrete Classes Color:", Info.CONCRETE_CLASSES_COLOR));
				groups.add(color("Abstract Classes Color:", Info.ABSTRACT_CLASSES_COLOR));
				groups.add(color("Enumerations Color:", Info.ENUMERATIONS_COLOR));
				groups.add(color("CIMDatatypes Color:", Info.CIMDATATYPES_COLOR));
				groups.add(color("Compounds Color:", Info.COMPOUNDS_COLOR));
				groups.add(color("Primitives Color:", Info.PRIMITIVES_COLOR));
				groups.add(color("Choices Color:", Info.CHOICES_COLOR));
				groups.add(color("Refs Color:", Info.REFS_COLOR));
				groups.add(color("Shadow Classes Color:", Info.SHADOW_CLASSES_COLOR));
				groups.add(color("Errors Color:", Info.ERRORS_COLOR));
				groups.add(Group(new BuilderPreferenceOption(Info.ANONYMOUS_CLASSES_COLOR_WHITE,
						"Set the color of anonymous classes to white")));
				groups.add(Group(new BuilderPreferenceOption(Info.ENABLE_DARK_MODE,
						"Enable 'Dark Mode' (overrides themes and colors above)")));
				groups.add(Group(new BuilderPreferenceOption(Info.ENABLE_SHADOWING, "Enable shadowing on classes")));
			}

			private GroupTemplate color(String caption, QualifiedName symbol) {
				return Group(Label(caption), new BuilderColorPreference("", symbol),
						Label(symbol.getLocalName() + "-label", Info.getBuilderPreference(getResource(), symbol)));
			}

			/**
			 * The Profile Real-Time Preview section, last on the page at the project and
			 * profile levels as it is at the workspace level.
			 */
			private void addDiagramStyle(List<GroupTemplate> groups, String... hint) {
				String displayStyleInitialSetting = Info
						.getCurrentProfileRealTimePreviewDisplayStyleByLevel(getResource());
				groups.add(Group(HRule()));
				groups.add(Group(Label("Profile Real-Time Preview PlantUML Diagram Preferences:")));
				groups.add(Group(Label("Diagram Style: "),
						new DisplayStyleReadOnlyCombo(Info.CURRENT_PROFILE_PREVIEW_STYLE, Validators.NONE,
								getPlantUMLBuildetStyles(), displayStyleInitialSetting)));
				for (String line : hint)
					groups.add(Group(Label(line)));
			}

			@Override
			public void update() {
				refreshPlantUMLViewParts(getResource());
			}

			private void refreshPlantUMLViewParts(IResource resource) {
				if (resource instanceof IProject) {
					// Project-level preference change: refresh the focused profile editor's
					// preview, but only if that profile lives in this project.
					ProfileEditor.refreshActiveProfilePreview((IProject) resource);
					return;
				}
				IFolder profileFolder = Info.getProfilesFolder(resource.getProject());
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
