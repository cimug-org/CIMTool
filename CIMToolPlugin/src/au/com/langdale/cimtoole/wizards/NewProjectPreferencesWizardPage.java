/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.Label;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import au.com.langdale.ui.binding.CheckBoxBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class NewProjectPreferencesWizardPage extends FurnishedWizardPage {

	public NewProjectPreferencesWizardPage() {
		super("preferences");
	}

	private IFile file;
	boolean importing;

	private ProjectBinding projects = new ProjectBinding();
	private IProject newProject;

	public void setNewProject(IProject newProject) {
		this.newProject = newProject;
	}

	public IFile getFile() {
		return file;
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {

				return Label("Display Style:");
			}

			@Override
			protected void addBindings() {
			}

			@Override
			public void refresh() {
			}

			@Override
			public String validate() {
				return null;
			}

		};
	}

}