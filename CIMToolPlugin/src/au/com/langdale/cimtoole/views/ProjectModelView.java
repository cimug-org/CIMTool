/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.views;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;

import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.cimtoole.actions.JumpAction.Jumpable;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeProvider;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.xmi.UML;

/**
* View the merged information model: CIM plus extensions.
*/
public class ProjectModelView extends ProjectModelFollower implements Searchable, Jumpable {

	private TreeViewer treeViewer;
	private UMLTreeModel tree;
	@Override
	public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		JenaTreeProvider.displayJenaTree(treeViewer, getTree());
		getSite().setSelectionProvider(treeViewer);
		treeViewer.addDoubleClickListener(jump);
		IWorkbenchPage page = getSite().getPage();
		listenToSelection(page);
	}

	private IDoubleClickListener jump = new IDoubleClickListener() {

		public void doubleClick(DoubleClickEvent event) {
			ITreeSelection selection = (ITreeSelection) event.getSelection();
			Node node = (Node) selection.getFirstElement();
			jump(node);
		}
		
	};
	
	public OntModel getOntModel() {
		return tree.getOntModel();
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
	
	@Override
	public boolean ignoreNode(ModelNode node) {
		return node.getModel() == tree;
	}

	public Node findNode(Resource target) {
		Node[] path = tree.findPathTo(target, false);
		return path != null? path[path.length-1]: null;
	}

	public void previewTarget(Resource target) {
		Node node = findNode(target);
		if( node != null)
			previewTarget(node);
	}

	public String getDescription() {
		return "Search the schema for packages, classes or their members by name.";
	}

	public void previewTarget(Node node) {
		treeViewer.setSelection(new TreeSelection(new TreePath(node.getPath(false))), true);
	}

	public Property getCriterion() {
		return  ResourceFactory.createProperty(RDFS.label);
	}
	
	public void selectModel(OntModel model) {
		tree.setOntModel(model);
		treeViewer.setSelection(treeViewer.getSelection());
	}

	public void jump(Node node) {
		OntResource subject = node.getSubject();
		if( subject != null ) {
			if( subject.isProperty()) {
				OntResource inverse = subject.getInverseOf();
				if(inverse != null)
					previewTarget(inverse);
				else {
					OntResource range = subject.getRange();
					if( range != null )
						previewTarget(range);
				}
			}
			else {
				previewTarget(subject);
			}
		}
	}
}
