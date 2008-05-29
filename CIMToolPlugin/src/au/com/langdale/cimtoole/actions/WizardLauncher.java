/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkbenchWizard;

public abstract class WizardLauncher implements IWorkbenchWindowActionDelegate, IViewActionDelegate {

	private IWorkbenchWindow window;
	private IWorkbenchPart part;
	private IAction action;

	public void init(IWorkbenchWindow workbenchWindow) {
		window = workbenchWindow;

	}

	public void init(IViewPart view) {
		part = view;
		window =view.getSite().getWorkbenchWindow();
	}


	protected ISelection getSelection() {
		if( part != null )
			return part.getSite().getSelectionProvider().getSelection();
		else
			return window.getSelectionService().getSelection();
	}
	
	
	public IWorkbenchWindow getWindow() {
		return window;
	}

	public IWorkbenchPart getPart() {
		return part;
	}

	public IAction getAction() {
		return action;
	}

	public void dispose() {
		part = null;
		window = null;
	}

	public void run(IAction action) {
        if (window == null) {
            // action has been disposed
            return;
        }

        IWorkbenchWizard wizard = createWizard();
        IStructuredSelection selectionToPass;

        ISelection selection = getSelection();
        if (selection instanceof IStructuredSelection) {
            selectionToPass = (IStructuredSelection) selection;
        } else {
            selectionToPass = StructuredSelection.EMPTY;
        }
        run(wizard, window, selectionToPass);
	}

	protected abstract IWorkbenchWizard createWizard();

	public void selectionChanged(IAction action, ISelection selection) {
		this.action = action;
	}

	public static void run(IWorkbenchWizard wizard, IWorkbenchWindow window, IStructuredSelection selection) {
        wizard.init(window.getWorkbench(), selection);

        Shell parent = window.getShell();
        WizardDialog dialog = new WizardDialog(parent, wizard);
        //Image image = action.getImageDescriptor().createImage();
		//dialog.getShell().setImage(image);
        dialog.create();
        dialog.open();

	}
}
