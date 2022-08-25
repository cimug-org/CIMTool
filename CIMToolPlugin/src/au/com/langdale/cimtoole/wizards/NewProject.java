/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class NewProject extends Wizard implements INewWizard {

	public NewProject() {
		setNeedsProgressMonitor(true);
	}

	private WizardNewProjectCreationPage main = new WizardNewProjectCreationPage("main") {
		
		@Override
		protected boolean validatePage() {
			if( !super.validatePage())
				return false;
			
			schema.setNewProject(main.getProjectHandle());
			multiline.setNewProject(main.getProjectHandle());
			singleline.setNewProject(main.getProjectHandle());
			return true;
		}
	};
	
	private SchemaWizardPage schema = new SchemaWizardPage(true); 

	private ImportMultiLineCopyrightTemplatePage multiline = new ImportMultiLineCopyrightTemplatePage("multiline", true); 
	
	private ImportSingleLineCopyrightTemplatePage singleline = new ImportSingleLineCopyrightTemplatePage("singleline", true); 
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		main.setTitle("New CIMTool Project");
		main.setDescription("Create and configure a new CIMTool project and import a copy of the CIM.");
		//
		multiline.setTitle("Project Copyright Template Configuration (multiline)");
		multiline.setDescription("Choose a multiline copyright configuration option for the project.");
		multiline.setSources(new String[]{"*.copyright-multi-line", "*.txt"});
		//multiline.setSelected(selection);
		//
		singleline.setTitle("Project Copyright Template Configuration (single-line)");
		singleline.setDescription("Choose a single-line copyright configuration option for the project.");
		singleline.setSources(new String[]{"*.copyright-single-line", "*.txt" });
		//singleline.setSelected(selection);
		//
		schema.setTitle("Import Initial Schema");
		schema.setDescription("Import an XMI or OWL base schema.");
	}

	@Override
	public void addPages() {
		addPage(main);
		addPage(multiline);
		addPage(singleline);
		addPage(schema);
	}

	@Override
	public boolean performFinish() {
		IWorkspaceRunnable job =  Task.createProject(main.getProjectHandle(), main.useDefaults()? null: main.getLocationURI());
		
		String multilineCopyright = multiline.getCopyrightTemplateTextForSelectedOption();
		InputStream multilineInputStream = new ByteArrayInputStream(multilineCopyright.getBytes());
		IFile multilineCopyrightTemplateFile = multiline.getFile();
		job = Task.chain(job, Task.importInputStreamToFile(multilineCopyrightTemplateFile, multilineInputStream));
		
		String singleLineCopyright = singleline.getCopyrightTemplateTextForSelectedOption();
		InputStream singleLineInputStream = new ByteArrayInputStream(singleLineCopyright.getBytes());
		IFile singleLineCopyrightTemplateFile = singleline.getFile();
		job = Task.chain(job, Task.importInputStreamToFile(singleLineCopyrightTemplateFile, singleLineInputStream));
		
		String pathname = schema.getPathname();
		if(pathname != null && pathname.length() != 0) {
			IFile schemaFile = schema.getFile();
			String namespace = schema.getNamespace();
			job = Task.chain( job, Task.importSchema(schemaFile, pathname, namespace));
		}

		return Jobs.runInteractive(job, ResourcesPlugin.getWorkspace().getRoot(), getContainer(), getShell());
	}
}
