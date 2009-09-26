/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class NewHTMLRules extends Wizard implements INewWizard {
	
	private RuleWizardPage main = new RuleWizardPage();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("New HTML Generation Rules"); 
		main.setTitle(getWindowTitle());
		main.setDescription("Create a new set of HTML generation rules for a profile.");
		main.setType("html-xslt");
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		return Jobs.runInteractive(Task.createRules(main.getFile(), main.isCopyDefault()? "html": null), main.getFile().getParent(), getContainer(), getShell());
	}
}
