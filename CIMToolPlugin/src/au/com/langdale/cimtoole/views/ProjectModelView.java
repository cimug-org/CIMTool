/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.ModelMinder;
import au.com.langdale.cimtoole.project.Cache.CacheListener;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.ui.binding.JenaTreeProvider;
import au.com.langdale.xmi.UML;

/**
* View the merged information model: CIM plus extensions.
*/
public class ProjectModelView extends ViewPart implements ISelectionListener, CacheListener, Searchable {

	private TreeViewer treeViewer;
	private ModelMinder models = new ModelMinder(this);
	private UMLTreeModel tree;
	private IProject activeProject;

	@Override
	public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		JenaTreeProvider.displayJenaTree(treeViewer, getTree());
		getSite().setSelectionProvider(treeViewer);
		IWorkbenchPage page = getSite().getPage();
		page.addSelectionListener(this);
		selectionChanged(null, page.getSelection());
		treeViewer.addDoubleClickListener(jump);
	}
	
	private IDoubleClickListener jump = new IDoubleClickListener() {

		public void doubleClick(DoubleClickEvent event) {
			ITreeSelection selection = (ITreeSelection) event.getSelection();
			Node node = (Node) selection.getFirstElement();
			OntResource subject = node.getSubject();
			if( subject != null ) {
				if( subject.isProperty()) {
					OntProperty prop = subject.asProperty();
					OntProperty inverse = prop.getInverseOf();
					if(inverse != null)
						previewTarget(inverse);
					else {
						OntResource range = prop.getRange();
						if( range != null )
							previewTarget(range);
					}
				}
				else {
					previewTarget(subject);
				}
			}
		}
		
	};

	private boolean followProject(IResource target) {
		IProject project = target.getProject();
		if( activeProject == null || ! project.equals(activeProject)) {
			//System.out.println("ProjectModelView switching to " + project);
			activeProject = project;
			modelCached(project);
			return true;
		}
		return false;
	}
	
	private void followSelection(ISelection selection) {
		if( selection instanceof IStructuredSelection && ! selection.isEmpty()) {
			IStructuredSelection struct = (IStructuredSelection) selection;
			if( struct.getFirstElement() instanceof ModelNode) {
				ModelNode node = (ModelNode) struct.getFirstElement();
				//System.out.println("ProjectModelView saw a " + node);
				
				String source_name = node.getModel().getSource();
				if( source_name == null)
					return;
				
				Path source_path = new Path(source_name);
				IResource target = ResourcesPlugin.getWorkspace().getRoot().getFile(source_path);
				if( followProject(target))
					return;

				OntResource base = node.getBase();
				if( base != null ) {
					previewTarget(base);
				}
			}
			else if( struct.getFirstElement() instanceof IResource) {
				IResource target = (IResource) struct.getFirstElement();
				//System.out.println("ProjectModelView saw " + target.getFullPath());
				followProject(target);
			}
		}
	}

	public boolean previewTarget(Resource base) {
		Node[] path = tree.findPathTo(base, false);
		if( path != null) {
			//System.out.println("ProjectModelView selecting " + base);
			treeViewer.setSelection(new TreeSelection(new TreePath(path)), true);
			return true;
		}
		return false;
	}
	
	public void selectTarget(Resource base) {
		// no action
	}
	
	public OntModel getOntModel() {
		return tree.getOntModel();
	}
	
	@Override
	public void dispose() {
		models.dispose();
		super.dispose();
	}

	private JenaTreeModelBase getTree() {
		if( tree == null ) {
			tree = new UMLTreeModel();
			tree.setRootResource(UML.global_package);
		}
		return tree;
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();

	}

	public void modelCached(IResource key) {
		if( activeProject == null)
			return;
		
		OntModel merger = models.getProjectOntology(Info.getSchemaFolder(activeProject));
		tree.setOntModel(merger);
		treeViewer.setSelection(treeViewer.getSelection());
		followSelection(getSite().getPage().getSelection());
	}

	public void modelDropped(IResource key) {
		// ignored
	}

	public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				followSelection(selection);
				
			}
		});
	}
}
