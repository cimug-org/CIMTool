/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.FileField;
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

public class ImportProfilePage extends FurnishedWizardPage {

	private IFile file;
	
	private String[] sources;

	private ProjectBinding projects = new ProjectBinding();
	private TextBinding source = new TextBinding(Validators.EXTANT_FILE);
	private LocalFileBinding filename = new LocalFileBinding("owl", true);
	
	public ImportProfilePage() {
		super("main");
	}

	public String getPathname() {
		return source.getText();
	}

	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}

	public IFile getFile() {
		return file;
	}

	public String[] getSources() {
		return sources;
	}

	public void setSources(String[] sources) {
		this.sources = sources;
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			
			@Override
			protected Template define() {
				return Grid(
				    Group(FileField("source", "File to import:", sources)),
					Group(Label("Project")),
					Group(CheckboxTableViewer("projects")),
					Group(Label("Profile name:"), Field("filename"))
				);
			}

			@Override
			protected void addBindings() {
				projects.bind("projects", this);
    			source.bind("source", this);
				filename.bind("filename", this, source);
			}

			@Override
			public String validate() {
				IProject project = projects.getProject();
				file = filename.getFile(Info.getProfileFolder(project));
				
				if( file.exists())
					return "A profile of that name already exists.";

				return null;
			}
		};
	}
}