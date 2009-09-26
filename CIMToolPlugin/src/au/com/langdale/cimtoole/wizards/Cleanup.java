/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;
import au.com.langdale.workspace.ResourceUI.DiagnosticsBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;
import static au.com.langdale.ui.builder.Templates.*;

public class Cleanup extends Wizard implements IWorkbenchWizard {

	private IResource[] resources;
	private ProjectBinding projects = new ProjectBinding();
	private DiagnosticsBinding models = new DiagnosticsBinding();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		projects.setSelected(selection);
		models.setSelected(selection);
	}

	@Override
	public void addPages() {
		addPage(main);
	}

	@Override
	public boolean performFinish() {
		Jobs.runInteractive(Task.delete(resources), projects.getProject(), getContainer(), getShell());
		return true;
	}

	private FurnishedWizardPage main = new FurnishedWizardPage("main", "Cleanup Projects", null) {
		{
			setDescription(
				"Delete the selected diagnostic reports and associated models"
			);
		}

		@Override
		protected Content createContent() {

			return new Content() {

				@Override
				protected Template define() {
					return Grid(
						Group(
								Label("Project"), 
								Label("Contents")),
						Group(
								CheckboxTableViewer("projects"), 
								CheckboxTableViewer("models", true))

					);
				}
				
				@Override
				public Control realise(Composite parent) {
					Control panel = super.realise(parent);
					projects.bind("projects", this);
					models.bind("models", this, projects);
					return panel;
				}

				@Override
				public String validate() {
					Object[] instances = models.getValues();
					resources = new IResource[instances.length];
					for (int ix = 0; ix < instances.length; ix++) {
						if( ! (instances[ix] instanceof IFile))
							return "Invalid selection";
						IFile ref = (IFile) instances[ix];
						resources[ix] = Info.getRelatedFolder(ref);
					}
					
					return null;
				}
			};
		}
	};
}
