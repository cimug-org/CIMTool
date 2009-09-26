package au.com.langdale.cimtoole.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;

public abstract class SelectionFollower extends ViewPart implements ISelectionListener {
	private IWorkbenchPage page;
	
	public abstract void selectProject(IProject project);
	public abstract boolean previewTarget(Resource base);
	public abstract boolean ignoreNode( ModelNode node);

	public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				followSelection(selection);
				
			}
		});
	}
	
	public void followSelection(ISelection selection) {
		if( selection instanceof IStructuredSelection && ! selection.isEmpty()) {
			IStructuredSelection struct = (IStructuredSelection) selection;
			if( struct.getFirstElement() instanceof ModelNode) {
				ModelNode node = (ModelNode) struct.getFirstElement();
				//System.out.println("ProjectModelView saw a " + node);
				
				if( ignoreNode(node))
					return;
				
				String source_name = node.getModel().getSource();
				if( source_name != null) {
					Path source_path = new Path(source_name);
					IResource target = ResourcesPlugin.getWorkspace().getRoot().getFile(source_path);
					selectProject(target.getProject());
				}
				
				OntResource base = node.getBase();
				if( base != null ) {
					previewTarget(base);
				}
			}
			else if( struct.getFirstElement() instanceof IResource) {
				IResource target = (IResource) struct.getFirstElement();
				//System.out.println("ProjectModelView saw " + target.getFullPath());
				selectProject(target.getProject());
			}
		}
	}
	
	public void listenToSelection(IWorkbenchPage page) {
		page.addSelectionListener(this);
		this.page = page;
		selectionChanged(null, page.getSelection());
	}
	
	public void  dispose() {
		if( page != null )
			page.removeSelectionListener(this);
		super.dispose();
	}
}
