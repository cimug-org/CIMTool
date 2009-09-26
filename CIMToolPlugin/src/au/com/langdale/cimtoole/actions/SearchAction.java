/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.part.PageBookView;

import au.com.langdale.cimtoole.wizards.SearchWizard;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;

public class SearchAction extends WizardLauncher implements Runnable {

	@Override
	public void init(IWorkbenchWindow window) {
		IViewPart view = window.getActivePage().findView("au.com.langdale.cimtoole.views.ProjectModelView");
		if( view != null) 
			init(view);
	}
	
	@Override
	protected IWorkbenchWizard createWizard() {
		return new SearchWizard(getSearchArea());
	}

	private Searchable getSearchArea() {

		Object part = getPart();
		if( part instanceof PageBookView) {
			part = ((PageBookView)part).getCurrentPage();
		}
		
		if (part instanceof Searchable) {
			Searchable 	result = (Searchable) part;
			if(result.getOntModel() != null)
				return result;
		}
		return SearchWizard.EMPTY_AREA;
	}

	private void refresh() {
		IAction action = getAction();
		if(action != null)
			action.setEnabled((getSearchArea() != SearchWizard.EMPTY_AREA));
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		getWindow().getWorkbench().getDisplay().asyncExec(this);
	}
	
	public void run() {
		refresh();
	}
}
