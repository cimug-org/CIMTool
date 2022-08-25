/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class ImportMultiLineCopyrightTemplate extends Wizard implements IImportWizard {
	
	private ImportMultiLineCopyrightTemplatePage main = new ImportMultiLineCopyrightTemplatePage(false);
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Copyright Template (multiline)"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import a multiline copyright template to be utilized by the builders of the given project.");
		main.setSources(new String[]{"*.copyright-multi-line", "*.txt"});
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		boolean result = Jobs.runInteractive(Task.importMultiLineCopyright(main.getFile(), main.getPathname()), main.getFile().getProject(), getContainer(), getShell());
		Jobs.cleanBuildProject(main.getFile().getProject());
		return result;
	}
}
