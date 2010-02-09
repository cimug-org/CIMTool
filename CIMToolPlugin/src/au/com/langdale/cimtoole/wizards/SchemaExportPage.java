/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.RadioButton;
import static au.com.langdale.ui.builder.Templates.Row;
import static au.com.langdale.ui.builder.Templates.SaveButton;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class SchemaExportPage extends FurnishedWizardPage {

	private String SCHEMA = "schema.merged-owl";
	private String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);

	private boolean internal;
	private TextBinding path = new TextBinding(Validators.NEW_FILE);
	private TextBinding namespace = new TextBinding(Validators.NAMESPACE, NAMESPACE);
	private ProjectBinding projects = new ProjectBinding();

	public SchemaExportPage(String schemaFileName) {
		super("schema");
		this.SCHEMA = schemaFileName;
	}
	
	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}
	
	public String getNamespace() {
		return namespace.getText();
	}

	public String getPathname() {
		return path.getText();
	}
	
	public boolean isInternal() {
		return internal;
	}
	
	public IProject getProject() {
		return projects.getProject();
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(
					Group(Label("Project")), 
					Group(CheckboxTableViewer("projects")),
					Group(Label("Namespace URI:"), Field("namespace")),
					Group(RadioButton("internal", "Create "+ SCHEMA + " in the project")),
					Group(RadioButton("external", "Export a file to filesystem")),
					Group(Label("File to export:"), Field("path"), Row(SaveButton("save", "path", "*.owl")))
				);
			}

			@Override
			protected void addBindings() {
				projects.bind("projects", this);
				path.bind("path", this);
				namespace.bind("namespace", this);
			}
			
			private IProject last;
			
			@Override
			public void refresh() {
				IProject project = projects.getProject();
				if( project != null && ! project.equals(last)) {
					last = project;
					getButton("external").setSelection(project.getFile(SCHEMA).exists());
				}
				boolean external = getButton("external").getSelection();
				getButton("internal").setSelection(! external);
				getControl("path").setEnabled(external);
				getControl("save").setEnabled(external);
			}

			@Override
			public String validate() {
				internal = getButton("internal").getSelection();
				if( internal && projects.getProject().getFile(SCHEMA).exists())
					return "The file " + SCHEMA + " already exists in the project";
					
				return null;
			}
		};
	}
}