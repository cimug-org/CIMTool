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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.LocalFileBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class NewMappingsPage extends FurnishedWizardPage {

	private final String NAMESPACE = Info.getPreference(Info.MAPPING_NAMESPACE);
	private final String LABEL = Info.getPreference(Info.MAPPING_LABEL);
	
	private IFile file;
	
	private ProjectBinding projects = new ProjectBinding();
	private LocalFileBinding filename = new LocalFileBinding("mapping-owl", true);
	private TextBinding namespace = new TextBinding(Validators.NAMESPACE, NAMESPACE);
	private TextBinding envelope = new TextBinding(Validators.NCNAME, LABEL);
	
	public NewMappingsPage() {
		super("main");
	}

	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}

	public IFile getFile() {
		return file;
	}

	public String getNamespace() {
		return namespace.getText();
	}

	public String getEnvname() {
		return envelope.getText();
	}

	@Override
	protected Content createContent() {
		return new Content() {

			
			@Override
			protected Template define() {
				return Grid(
					Group(Label("Namespace URI:"), Field("namespace")),
					Group(Label("Project")),
					Group(CheckboxTableViewer("projects")),
					Group(Label("File name:"), Field("filename")),
					Group(Label("Display label: "), Field("envelope"))
				);
			}

			@Override
			protected void addBindings() {
				projects.bind("projects", this);
				filename.bind("filename", this);
				namespace.bind("namespace", this);
				envelope.bind("envelope", this);
			}

			@Override
			public String validate() {
				IProject project = projects.getProject();
				file = filename.getFile(Info.getSchemaFolder(project));
				
				if( file.exists())
					return "A schema file of that name already exists.";

				return null;
			}
		};
	}
}
