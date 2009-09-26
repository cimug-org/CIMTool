/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;
import static au.com.langdale.ui.builder.Templates.*;

public class SchemaExportPage extends FurnishedWizardPage {

	public static final String SCHEMA = "schema.merged-owl";
	private String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);

	private boolean internal;
	private TextBinding path = new TextBinding(Validators.NEW_FILE);
	private TextBinding namespace = new TextBinding(Validators.NAMESPACE, NAMESPACE);
	private ProjectBinding projects = new ProjectBinding();

	public SchemaExportPage() {
		super("schema");
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
			public Control realise(Composite parent) {
				Control panel = super.realise(parent);
				projects.bind("projects", this);
				path.bind("path", this);
				namespace.bind("namespace", this);
				return panel;
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