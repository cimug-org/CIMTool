/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class NewProfile extends Wizard implements INewWizard {

    private NewProfilePage main = new NewProfilePage();
    private IWorkbench workbench;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        setWindowTitle("New Profile");
        setNeedsProgressMonitor(true);
        main.setTitle(getWindowTitle());
        main.setDescription("Create a new OWL profile definition.");
        main.setSelected(selection);
    }

    @Override
    public void addPages() {
        addPage(main);
    }

    @Override
    public boolean performFinish() {
        if( Jobs.runInteractive(Task.createProfile(main.getFile(), main.getNamespace(), main.getEnvname(), main.getFillProfile()), main.getFile().getParent(), getContainer(), getShell())) {
            try {
                workbench.getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput(main.getFile()), "au.com.langdale.cimtoole.editors.MessageModelEditor");
                return true;
            } catch (PartInitException e) {
                e.printStackTrace();
                return true;  // we created the file but didn't open an editor
            }
        }
        else
            return false;
    }
}
