/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.ui.builder.FurnishedWizard;

public class NewProject extends FurnishedWizard implements INewWizard {

	public NewProject() {
		setNeedsProgressMonitor(true);
	}

	private WizardNewProjectCreationPage main = new WizardNewProjectCreationPage("main") {
		
		@Override
		protected boolean validatePage() {
			if( !super.validatePage())
				return false;
			
			schema.setProject(main.getProjectHandle());
			return true;
		}
	};
	
	private OptionalSchemaWizardPage schema = new OptionalSchemaWizardPage(); 

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		main.setTitle("New CIMTool Project");
		main.setDescription("Create a new CIMTool project and import a copy of the CIM.");
		schema.setTitle("Import Initial Schema");
		schema.setDescription("Import an XMI or OWL base schema.");
	}

	@Override
	public void addPages() {
		addPage(main);
		addPage(schema);
	}

	@Override
	public boolean performFinish() {
		IWorkspaceRunnable job =  Task.createProject(main.getProjectHandle(), main.useDefaults()? null: main.getLocationURI());

		IFile schemaFile = schema.getFile();
		if( schemaFile != null )
			job = Task.chain( job, Task.importSchema(schemaFile, schema.getPathname(), schema.getNamespace()));

		return run( job, ResourcesPlugin.getWorkspace().getRoot());
	}
}
