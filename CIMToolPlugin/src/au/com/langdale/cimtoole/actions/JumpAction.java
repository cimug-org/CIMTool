package au.com.langdale.cimtoole.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import au.com.langdale.jena.TreeModelBase.Node;


public class JumpAction implements IViewActionDelegate {

	private IViewPart view;
	private ISelection selection;
	
	public interface Jumpable {
		public void jump(Node node);
	}

	public void run(IAction action) {
		if( view instanceof Jumpable && selection instanceof IStructuredSelection) {
			Jumpable searchable = (Jumpable) view;
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if( element instanceof Node ) {
				searchable.jump((Node)element);
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
