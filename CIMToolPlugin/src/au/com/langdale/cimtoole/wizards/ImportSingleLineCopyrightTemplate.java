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

public class ImportSingleLineCopyrightTemplate extends Wizard implements IImportWizard {
	
	private ImportSingleLineCopyrightTemplatePage main = new ImportSingleLineCopyrightTemplatePage();
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Copyright Template (single-line)"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import a single-line copyright template to be utilized by the builders of the given project.");
		main.setSources(new String[]{"*.copyright-single-line", "*.txt" });
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		boolean result = Jobs.runInteractive(Task.importSingleLineCopyright(main.getFile(), main.getPathname()), main.getFile().getParent(), getContainer(), getShell());
		Jobs.cleanBuildProject(main.getFile().getProject());
		return result;
	}
}
