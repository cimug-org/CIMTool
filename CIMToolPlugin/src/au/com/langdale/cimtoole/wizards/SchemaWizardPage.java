/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.FileField;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.RadioButton;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Button;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.registries.ModelParserRegistry;
import au.com.langdale.cimtoole.reporting.ReportGenerationSettings;
import au.com.langdale.ui.binding.CheckBoxBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.LocalFileBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class SchemaWizardPage extends FurnishedWizardPage {

	protected final boolean expectNewProject;

	/**
	 * These booleans are only used when importing a schema for an existing project.
	 */
	private Boolean mergeShadowExtensionsEnabled;
	private Boolean selfHealingOnScheamImportEnabled;

	public SchemaWizardPage(final boolean expectNewProject) {
		super("schema");
		this.expectNewProject = expectNewProject;
		//
		mergeShadowExtensionsEnabled = Info.getPreferenceOption(Info.MERGE_SHADOW_EXTENSIONS);
		selfHealingOnScheamImportEnabled = Info.getPreferenceOption(Info.SELF_HEAL_ON_IMPORT);
		//
		if (this.expectNewProject) {
			mergeShadowExtensions = new CheckBoxBinding(mergeShadowExtensionsEnabled);
			selfHealingOnScheamImport = new CheckBoxBinding(selfHealingOnScheamImportEnabled);
		}
		//
		shouldGenerateReport = new DependentCheckBoxBinding() {

			@Override
			public void refreshDependentCheckBoxes() {
				boolean isGenerateReportSelected = getCheckBox().getSelection();
				boolean isNormativeSelected = getContent().getButton("include-normative").getSelection();
				//
				/**  Remove the comments around this section once reports are ready post the 2.3.0
				getContent().getButton("include-extensions").setEnabled(isGenerateReportSelected);
				getContent().getButton("include-normative").setEnabled(isGenerateReportSelected);
				if (!isGenerateReportSelected) {
					getContent().getButton("include-grid").setEnabled(false);
					getContent().getButton("include-enterprise").setEnabled(false);
					getContent().getButton("include-market").setEnabled(false);
				} else {
					getContent().getButton("include-grid").setEnabled(isNormativeSelected);
					getContent().getButton("include-enterprise").setEnabled(isNormativeSelected);
					getContent().getButton("include-market").setEnabled(isNormativeSelected);
				}
				*/
			}
		};
		includeExtensions = new CheckBoxBinding(this.expectNewProject);
		includeNormative = new DependentCheckBoxBinding(this.expectNewProject) {
			@Override
			public void refreshDependentCheckBoxes() {
				boolean isNormativeSelected = getCheckBox().getSelection();
				boolean isGenerateReportSelected = getContent().getButton("generate-report").getSelection();
				//
				/**  Remove the comments around this section once reports are ready post 2.3.0
				if (!isGenerateReportSelected) {
					getContent().getButton("include-grid").setEnabled(false);
					getContent().getButton("include-enterprise").setEnabled(false);
					getContent().getButton("include-market").setEnabled(false);
				} else {
					getContent().getButton("include-grid").setEnabled(isNormativeSelected);
					getContent().getButton("include-enterprise").setEnabled(isNormativeSelected);
					getContent().getButton("include-market").setEnabled(isNormativeSelected);
				}
				*/
			}
		};
		includeGrid = new CheckBoxBinding(this.expectNewProject);
		includeEnterprise = new CheckBoxBinding(this.expectNewProject);
		includeMarket = new CheckBoxBinding(this.expectNewProject);

		filename = new LocalFileBinding(getExtSources(), false, true);
	}

	public SchemaWizardPage() {
		this(false);
	}

	private String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);
	private static String[] sources = { "*.xmi;*.owl;*.eap;*.eapx;*.qea;*.qeax", "*.xmi", "*.owl", "*.eap",
			"*.eapx", "*.qea", "*.qeax" };

	private IFile file;
	boolean importing;

	private TextBinding source = new TextBinding(Validators.OPTIONAL_EXTANT_FILE);
	private LocalFileBinding filename;
	private RadioTextBinding namespace = new RadioTextBinding(Validators.NAMESPACE, NAMESPACE);
	private CheckBoxBinding mergeShadowExtensions;
	private CheckBoxBinding selfHealingOnScheamImport;
	private DependentCheckBoxBinding shouldGenerateReport;
	private CheckBoxBinding includeExtensions;
	private DependentCheckBoxBinding includeNormative;
	private CheckBoxBinding includeGrid;
	private CheckBoxBinding includeEnterprise;
	private CheckBoxBinding includeMarket;

	public class DependentCheckBoxBinding extends CheckBoxBinding {

		public DependentCheckBoxBinding() {
			super();
		}

		public DependentCheckBoxBinding(boolean initialState) {
			super(initialState);
		}

		public void reset() {
			super.reset();
			refreshDependentCheckBoxes();
		}

		public void update() {
			super.update();
			refreshDependentCheckBoxes();
		}

		public void refresh() {
			if (getCheckBox().getSelection() != getChecked()) {
				super.refresh();
				refreshDependentCheckBoxes();
			}
		}

		public void refreshDependentCheckBoxes() {
		}

	}

	private class RadioTextBinding extends TextBinding {

		private Button[] radios = new Button[0];
		private String[] values = new String[0];

		public RadioTextBinding(Validator validator, String initial) {
			super(validator, initial);
		}

		public void bind(String name, String[] nameValues, Assembly plumbing) {
			bind(name, plumbing, null);
			radios = new Button[nameValues.length / 2];
			values = new String[nameValues.length / 2];
			for (int ix = 0; ix + 1 < nameValues.length; ix += 2) {
				radios[ix / 2] = plumbing.getButton(nameValues[ix]);
				values[ix / 2] = nameValues[ix + 1];
			}
		}

		@Override
		protected String createSuggestion() {
			for (int ix = 0; ix < radios.length; ix++) {
				if (radios[ix].getSelection())
					return values[ix];
			}
			return null;
		}

		public void setExistingSchemaNSPreset(String namespace) {
			if (namespace != null && !namespace.equals(values[3])) {
				values[3] = namespace;
				refresh();
			}
		}

		@Override
		public void refresh() {
			super.refresh();
			for (int ix = 0; ix < radios.length; ix++) {
				radios[ix].setSelection(values[ix].equals(getValue()));
			}
		}

	}

	private String[] presets = new String[] { "cim16", "http://iec.ch/TC57/2013/CIM-schema-cim16#", "cim17",
			"http://iec.ch/TC57/CIM100#", "cim18", "http://cim.ucaiug.io/ns#", "existing", "", "preset", NAMESPACE };

	private ProjectBinding projects = new ProjectBinding();
	private IProject newProject;

	public void setSelected(IStructuredSelection selection) {
		// Set the selected project.
		projects.setSelected(selection);

		// Then determine if merging shadow extensions is enabled.
		if (projects.getProject() != null) {
			mergeShadowExtensionsEnabled = Info.isMergeShadowExtensionsEnabled(projects.getProject());
		}

		// Then determine if self healing on import is enabled.
		if (projects.getProject() != null) {
			selfHealingOnScheamImportEnabled = Info.isSelfHealingOnSchemaImportEnabled(projects.getProject());
		}
	}

	public void setNewProject(IProject newProject) {
		this.newProject = newProject;
	}

	public IFile getFile() {
		return file;
	}

	public String getNamespace() {
		return namespace.getText();
	}

	public Boolean isMergeShadowExtensionsEnabled() {
		if (expectNewProject)
			return mergeShadowExtensions.getChecked();
		else
			return mergeShadowExtensionsEnabled;
	}

	public Boolean isSelfHealingOnImportEnabled() {
		if (expectNewProject)
			return selfHealingOnScheamImport.getChecked();
		else
			return selfHealingOnScheamImportEnabled;
	}

	public ReportGenerationSettings getReportGenerationSettings() {
		return new ReportGenerationSettings.Builder() //
				.schemaFile(file) //
				.isMergeShadowExtensionsEnabled(
						(expectNewProject ? mergeShadowExtensions.getChecked() : mergeShadowExtensionsEnabled)) //
				.isSelfHealingOnSchemaImportEnabled(
						(expectNewProject ? selfHealingOnScheamImport.getChecked() : selfHealingOnScheamImportEnabled)) //
				.shouldGenerateReport(shouldGenerateReport.getChecked()) //
				.includeExtensions(getContent().getButton("include-extensions").getSelection()) //
				.includeNormative(getContent().getButton("include-normative").getSelection()) //
				.includeGrid(getContent().getButton("include-grid").getSelection()) //
				.includeEnterprise(getContent().getButton("include-enterprise").getSelection()) //
				.includeMarket(getContent().getButton("include-market").getSelection()) //
				.build();
	}

	public String getPathname() {
		return source.getText();
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				String[] sources = getExtSources();
				return Grid(Group(FileField("source", "File to import:", sources)),
						Group(RadioButton("cim16", "CIM 16 (2013)"), RadioButton("cim17", "CIM 17 (CIM100)"),
								RadioButton("cim18", "CIM 18 (CIM101)"), RadioButton("existing", "Current Schema NS"),
								RadioButton("preset", "Preference*")),
						Group(Label("Namespace URI:"), Field("namespace")),
						(expectNewProject
								? Group(CheckBox(Info.MERGE_SHADOW_EXTENSIONS.getLocalName(),
										"During import merge shadow/mixin extensions into the CIM classes that inherit from them"))
								: null),
						(expectNewProject
								? Group(CheckBox(Info.SELF_HEAL_ON_IMPORT.getLocalName(),
										"Enable self-healing when importing EA projects (.eap, .qea, etc.) as schema"))
								: null),
						Group(Label(expectNewProject ? "" : "Project")), expectNewProject ? null : Group(CheckboxTableViewer("projects")),
						Group(Label("Schema name:"), Field("filename")),
						Group(CheckBox("replace", "Replace existing schema.")),
						Group(Label("* Set this under Windows > Preferences > CIMTool")), Group(Label("")),
						Group(HRule()),
						Group(Grid(Group(Label("CIMTool Model Integrity Reporting:")),
								Group(CheckBox("generate-report",
										"Generate a 'CIM Modeling Guide' validation report during import", false)),
								Group(Label("    "), Grid(
										Group(CheckBox("include-extensions",
												"Include custom extensions in the model validation report", false)),
										Group(CheckBox("include-normative",
												"Include the following normative CIM packages in the model validation report",
												false)),
										Group(Label("    "), Grid(Group(CheckBox("include-grid",
												"Include the top-level Grid (formerly IEC61970) package", false)),
												Group(CheckBox("include-enterprise",
														"Include the top-level Enterprise (formerly IEC61968)", false)),
												Group(CheckBox("include-market",
														"Include the top-level Market (formerly IEC62325) package",
														false)))))))));
			}

			@Override
			protected void addBindings() {
				if (expectNewProject) {
					mergeShadowExtensions.bind(Info.MERGE_SHADOW_EXTENSIONS.getLocalName(), this);
					selfHealingOnScheamImport.bind(Info.SELF_HEAL_ON_IMPORT.getLocalName(), this);
				} else {
					projects.bind("projects", this);
					if (projects.getProject() != null) {
						String ns;
						try {
							ns = Task.getSchemaNamespace(projects.getProject());
							presets[7] = ns;
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
				//=================================================================================
				// These next lines to be removed post 2.3.0 release once reports are ready
				getButton("generate-report").setEnabled(false);
				getButton("include-extensions").setEnabled(false);
				getButton("include-normative").setEnabled(false);
				getButton("include-grid").setEnabled(false);
				getButton("include-enterprise").setEnabled(false);
				getButton("include-market").setEnabled(false);
				//=================================================================================
				// Uncomment these next lines at the time the the reports are ready...
				//shouldGenerateReport.bind("generate-report", this);
				//includeNormative.bind("include-normative", this);
				//includeExtensions.bind("include-extensions", this);
				//includeGrid.bind("include-grid", this);
				//includeEnterprise.bind("include-enterprise", this);
				//includeMarket.bind("include-market", this);
				source.bind("source", this);
				filename.bind("filename", this, source);
				namespace.bind("namespace", presets, this);
				//
				if (expectNewProject) {
					getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName()).setSelection(false);
					getButton(Info.SELF_HEAL_ON_IMPORT.getLocalName()).setSelection(false);
					getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName()).setEnabled(false);
					getButton(Info.SELF_HEAL_ON_IMPORT.getLocalName()).setEnabled(false);
					// The next two must be initialized to disabled...
					getButton("include-extensions").setEnabled(false);
					getButton("include-normative").setEnabled(false);
				}
			}

			@Override
			public void refresh() {
				if (expectNewProject) {
					if (filename.getFile(Info.getSchemaFolder(newProject)) != null) {
						IFile theFile = filename.getFile(Info.getSchemaFolder(newProject));
						boolean isEAProject = Info.isEAProject(theFile);
						boolean isCurrentStateEnabled = getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName())
								.getEnabled();
						if (!isCurrentStateEnabled & isEAProject) {
							getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName()).setEnabled(true);
							getButton(Info.SELF_HEAL_ON_IMPORT.getLocalName()).setEnabled(true);
							getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName()).setSelection(true);
							getButton(Info.SELF_HEAL_ON_IMPORT.getLocalName()).setSelection(true);
						} else if (isCurrentStateEnabled && !isEAProject) {
							getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName()).setEnabled(false);
							getButton(Info.SELF_HEAL_ON_IMPORT.getLocalName()).setEnabled(false);
							getButton(Info.MERGE_SHADOW_EXTENSIONS.getLocalName()).setSelection(false);
							getButton(Info.SELF_HEAL_ON_IMPORT.getLocalName()).setSelection(false);
						}

						if (getButton("generate-report").isEnabled() && !isEAProject) {
							// uncomment when reports are ready after 2.3.
							//getButton("generate-report").setSelection(false);
							//getButton("generate-report").setEnabled(false);
						} else if (!getButton("generate-report").isEnabled() && isEAProject) {
							// uncomment when reports are ready after 2.3.
							//getButton("generate-report").setEnabled(true);
							//getButton("generate-report").setSelection(true);
						}
						// uncomment when reports are ready after 2.3.0
						//shouldGenerateReport.refreshDependentCheckBoxes();
					}
				}
			}

			@Override
			public String validate() {
				if (source.getText().length() == 0) {
					if (expectNewProject) {
						return null;
					} else {
						populateCurrentSchemaNS(projects.getProject());
						return "An XMI, OWL, EA Project or other valid schema file is required.";
					}
				}
				IProject project = expectNewProject ? newProject : projects.getProject();
				file = filename.getFile(Info.getSchemaFolder(project));
				if (file == null)
					return "A project resource name is required";

				boolean exists = file.exists();
				getButton("replace").setEnabled(exists);
				
				if (exists) 
					populateCurrentSchemaNS(file);
				else
					populateCurrentSchemaNS(project);
				
				if (exists && !getButton("replace").getSelection()) {
					return "The selected schema already exists in the project. Check 'Replace existing schema' to proceed.";
				}

				String check = null;
				if (source.getText().toLowerCase().endsWith(".eap")
						|| source.getText().toLowerCase().endsWith(".eapx")) {
					check = Info.checkValidEAProject(new java.io.File(source.getText()));
				}

				if (check != null)
					return check;
				return null;
			}

			private void populateCurrentSchemaNS(IProject project) {
				try {
					String existingSchemaNamespace = Task.getSchemaNamespace(project);
					if (namespace.radios[3].getSelection()) {
						namespace.setText(existingSchemaNamespace);
					}
					namespace.setExistingSchemaNSPreset(existingSchemaNamespace);
				} catch (CoreException e) {
					namespace.setExistingSchemaNSPreset("");
				}
			}
			
			private void populateCurrentSchemaNS(IFile schemaFile) {
				try {
					String existingSchemaNamespace = Task.getSchemaNamespace(schemaFile);
					if (namespace.radios[3].getSelection()) {
						namespace.setText(existingSchemaNamespace);
					}
					namespace.setExistingSchemaNSPreset(existingSchemaNamespace);
				} catch (CoreException e) {
					namespace.setExistingSchemaNSPreset("");
				}
			}
		};
	}

	private String[] getExtSources() {
		String[] sources = SchemaWizardPage.sources;
		String[] extended = ModelParserRegistry.INSTANCE.getExtensions();
		if (extended.length > 0) {
			Set<String> extExtra = new TreeSet<String>();
			for (String s : extended)
				extExtra.add(s);
			for (String s : sources)
				extExtra.remove(s);
			if (extExtra.size() > 0) {
				String[] combined = new String[sources.length + extExtra.size()];
				System.arraycopy(sources, 0, combined, 0, sources.length);
				int i = sources.length;
				for (String s : extExtra)
					combined[i++] = "*." + s;
				sources = combined;
			}
		}
		return sources;
	}

}