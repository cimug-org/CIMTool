package au.com.langdale.cimtoole.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.OntResource;

public class JumpAction implements IViewActionDelegate {

	private IViewPart view;
	private ISelection selection;

	public void run(IAction action) {
		if( view instanceof Searchable && selection instanceof IStructuredSelection) {
			Searchable searchable = (Searchable) view;
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if( element instanceof Node ) {
				OntResource subject = ((Node)element).getSubject();
				if( subject != null ) {
					if( subject.isProperty()) {
						OntResource inverse = subject.getInverseOf();
						if(inverse != null)
							searchable.previewTarget(inverse);
						else {
							OntResource range = subject.getRange();
							if( range != null )
								searchable.previewTarget(range);
						}
					}
					else {
						searchable.previewTarget(subject);
					}
				}
				
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	public void init(IViewPart view) {
		this.view = view;
	}

}
