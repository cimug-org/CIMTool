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

public class ImportXSDRules extends Wizard implements IImportWizard {
	
	private RuleWizardPage main = new RuleWizardPage();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import XSD Generation Rules"); 
		main.setTitle(getWindowTitle());
		main.setDescription("Import a set of XML schema generation rules for a profile.");
		main.setType("xsd-xslt");
		main.setSources(new String[] {"*.xsd-xslt", "*.xslt"});
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		return Jobs.runInteractive(Task.importRules(main.getFile(), main.getPathname()), main.getFile().getParent(), getContainer(), getShell());
	}
}
