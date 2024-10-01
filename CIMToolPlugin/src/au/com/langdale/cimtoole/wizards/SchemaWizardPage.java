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
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.RadioButton;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Button;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.registries.ModelParserRegistry;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.LocalFileBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class SchemaWizardPage extends FurnishedWizardPage {
	private final boolean expectNewProject;

	public SchemaWizardPage(boolean expectNewProject) {
		super("schema");
		this.expectNewProject = expectNewProject;
		filename = new LocalFileBinding(getExtSources(), false);
	}

	public SchemaWizardPage() {
		this(false);
	}

	private String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);
	private static String[] sources = {"*.xmi;*.owl;*.eap;*.eapx;*.feap;*.qea;*.qeax", "*.xmi", "*.owl", "*.eap", "*.eapx", "*.feap", "*.qea", "*.qeax"};

	private IFile file;
	boolean importing;

	private TextBinding source = new TextBinding(Validators.OPTIONAL_EXTANT_FILE);
	private LocalFileBinding filename;
	private RadioTextBinding namespace = new RadioTextBinding(Validators.NAMESPACE, NAMESPACE);
	
	private class RadioTextBinding extends TextBinding {
		
		private Button[] radios = new Button[0];
		private String[] values = new String[0];
		
		public RadioTextBinding(Validator validator, String initial) {
			super(validator, initial);
		}
		
		public void bind(String name, String[] nameValues, Assembly plumbing) {
			bind(name, plumbing, null);
			radios = new Button[nameValues.length/2];
			values = new String[nameValues.length/2];
			for(int ix = 0; ix + 1 < nameValues.length; ix += 2) {
				radios[ix/2] = plumbing.getButton(nameValues[ix]);
				values[ix/2] = nameValues[ix+1];
			}
		}
		
		@Override
		protected String createSuggestion() {
			for(int ix = 0; ix < radios.length; ix++) {
				if( radios[ix].getSelection())
					return values[ix];
			}
			return null;
		}
		
		public void setExistingSchemaNSPreset(String namespace) {
			if (namespace!= null && !namespace.equals(values[3])) {
				values[3] = namespace;
				refresh();
			}
		}
		
		@Override
		public void refresh() {
			super.refresh();
			for(int ix = 0; ix < radios.length; ix++) {
				radios[ix].setSelection(values[ix].equals(getValue()));
			}
		}
		
	}
	
	private String[] presets = new String[] {
            "cim16", "http://iec.ch/TC57/2012/CIM-schema-cim16#",
			"cim17", "http://iec.ch/TC57/CIM100#",
            "cim18", "http://iec.ch/TC57/CIM101#",
            "existing", "",
			"preset", NAMESPACE
	};

	private ProjectBinding projects = new ProjectBinding();
	private IProject newProject;

	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
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

	public String getPathname() {
		return source.getText();
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				String[] sources = getExtSources();
				return Grid(
					Group(FileField("source", "File to import:", sources)),
					Group(
                        RadioButton("cim16", "CIM 16 (2012)"),
                        RadioButton("cim17", "CIM 17 (CIM100)"),
                        RadioButton("cim18", "CIM 18 (CIM101)"),
                        RadioButton("existing", "Current Schema NS"),
						RadioButton("preset", "Preference*")),
					Group(Label("Namespace URI:"), Field("namespace")),
					Group(Label("Project")), 
					expectNewProject? null :Group(CheckboxTableViewer("projects")),
					Group(Label("Schema name:"), Field("filename")),
					Group(CheckBox("replace", "Replace existing schema.")),
					Group(Label("* Set this under Windows > Preferences > CIMTool"))
				);
			}

			@Override
			protected void addBindings() {
				if( ! expectNewProject ) {
					projects.bind("projects", this);
					if (projects.getProject() != null)  {
						String ns;
						try {
							ns = Task.getSchemaNamespace(projects.getProject());
							presets[7] = ns;
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
				source.bind("source", this);
				filename.bind("filename", this, source);
				namespace.bind("namespace", presets, this);
			}

			@Override
			public String validate() {
				if( source.getText().length() == 0)
					if(expectNewProject) {
						return null;
					} else {
						populateCurrentSchemaNS(projects.getProject());
						return "A schema XMI, OWL, EA Project or other valid schema file is required";
					}

				IProject project = expectNewProject? newProject: projects.getProject();
				file = filename.getFile(Info.getSchemaFolder(project));
				if( file == null )
					return "A project resource name is required";

				boolean exists = file.exists();
				getButton("replace").setEnabled(exists);
				populateCurrentSchemaNS(project);
				if( exists && ! getButton("replace").getSelection()) {
					return "A schema named " + filename.getText() + " already exists. " +
					"Check option to replace.";
				}

				String check = null;
				if( source.getText().toLowerCase().endsWith(".eap") || source.getText().toLowerCase().endsWith(".eapx")) {
					check = Info.checkValidEAProject(new File(source.getText()));
				}
				if( check != null)
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
		};
	}
	
	private String[] getExtSources(){
		String[] sources = SchemaWizardPage.sources;
		String[] extended = ModelParserRegistry.INSTANCE.getExtensions();
		if (extended.length>0){
			Set<String> extExtra = new TreeSet<String>();
			for (String s : extended) extExtra.add(s);
			for (String s : sources) extExtra.remove(s);
			if (extExtra.size()>0){
				String[] combined = new String[sources.length+extExtra.size()];
				System.arraycopy(sources, 0, combined, 0, sources.length);
				int i = sources.length;
				for (String s : extExtra)
					combined[i++] = "*."+s;
				sources = combined;
			}
		}
		return sources;
	}
}