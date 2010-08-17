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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.registries.ModelParser;
import au.com.langdale.cimtoole.registries.ModelParserRegistry;
import au.com.langdale.ui.binding.RadioTextBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.LocalFileBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class SchemaWizardPage extends FurnishedWizardPage {
	private final boolean expectNewProject;

	public SchemaWizardPage(boolean expectNewProject) {
		super("schema");
		this.expectNewProject = expectNewProject;
	}

	public SchemaWizardPage() {
		this(false);
		filename = new LocalFileBinding(getExtSources(), false);
	}

	private String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);
	private static String[] sources = {"*.xmi", "*.owl", "*.eap"};

	private IFile file;
	boolean importing;

	private TextBinding source = new TextBinding(Validators.OPTIONAL_EXTANT_FILE);
	private LocalFileBinding filename;
	private RadioTextBinding namespace = new RadioTextBinding(Validators.NAMESPACE, NAMESPACE);

	private String[] presets = new String[] {
			"cim12", "http://iec.ch/TC57/2007/CIM-schema-cim12#",
			"cim13", "http://iec.ch/TC57/2008/CIM-schema-cim13#",
                        "cim14", "http://iec.ch/TC57/2009/CIM-schema-cim14#",
                        "cim15", "http://iec.ch/TC57/2010/CIM-schema-cim15#",
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
						RadioButton("cim12", "CIM 12 (2007)"), 
						RadioButton("cim13", "CIM 13 (2008)"),
                                                RadioButton("cim14", "CIM 14 (2009)"),
                                                RadioButton("cim15", "CIM 15 (2010)"),
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
				if( ! expectNewProject )
					projects.bind("projects", this);
				source.bind("source", this);
				filename.bind("filename", this, source);
				namespace.bind("namespace", presets, this);
			}

			@Override
			public String validate() {
				if( source.getText().length() == 0)
					if(expectNewProject)
						return null;
					else
						return "A schema XMI, OWL, EAP or other valid schema file is required";

				IProject project = expectNewProject? newProject: projects.getProject();
				file = filename.getFile(Info.getSchemaFolder(project));
				if( file == null )
					return "A project resource name is required";

				boolean exists = file.exists();
				getButton("replace").setEnabled(exists);
				if( exists && ! getButton("replace").getSelection())
					return "A schema named " + filename.getText() + " already exists. " +
					"Check option to replace.";

				if( source.getText().endsWith(".eap")) {
					String check = Info.checkValidEAP(new File(source.getText()));
					if( check != null)
						return check;
				}
				return null;
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