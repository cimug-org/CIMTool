/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.ui.builder.FurnishedWizard;

public class ImportProfile extends FurnishedWizard implements IImportWizard {
	
	private ImportProfilePage main = new ImportProfilePage();
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Profile"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import an OWL profile definition.");
		main.setSources(new String[]{"*.owl"});
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		return run(Task.importProfile(main.getFile(), main.getPathname(), main.getNamespace()), main.getFile().getParent());
	}
}
