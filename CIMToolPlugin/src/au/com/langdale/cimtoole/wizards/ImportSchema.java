/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.ui.builder.FurnishedWizard;

public class ImportSchema extends FurnishedWizard implements IExportWizard {
	
	private SchemaWizardPage main = new SchemaWizardPage();
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Export Schema"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Export the merged schema as an OWL document.");
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		return run(Task.importSchema(main.getFile(), main.getPathname(), main.getNamespace()), main.getFile().getParent());
	}
}
