/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;


import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import au.com.langdale.cimtoole.project.ModelMinder;
import au.com.langdale.cimtoole.project.Cache.CacheListener;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.ui.binding.JenaTreeProvider;
import au.com.langdale.ui.binding.OntModelProvider;
import au.com.langdale.ui.builder.FurnishedMultiEditor;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

public abstract class ModelEditor extends FurnishedMultiEditor  implements ISelectionChangedListener, CacheListener, OntModelProvider {

	public class ModelOutliner extends ContentOutlinePage {

		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			TreeViewer viewer = getTreeViewer();
			viewer.setUseHashlookup(true);
			viewer.setContentProvider(getProvider());
			viewer.setInput(getTree());
			hookOutline(this);
		}

		public void drillTo(OntResource subject) {
			TreeViewer viewer = getTreeViewer();
			Node root = getTree().getRoot();
			Node top = root.findChild(subject);
			if( top != null ) {
				TreePath path = new TreePath(new Object[]{root, top});
				viewer.setSelection(new TreeSelection(path), true);
			}
			else {
				ITreeSelection selection = (ITreeSelection) viewer.getSelection();
				TreePath[] paths = selection.getPaths();
				if( paths.length == 1) {
					TreePath current = paths[0];
					Node parent = (Node) current.getLastSegment();
					Node child = parent.findChild(subject);
					if( child != null ) {
						TreePath path = current.createChildPath(child);
						viewer.setSelection(new TreeSelection(path), true);
					}
				}
			}
		}
		
		@Override
		public TreeViewer getTreeViewer() {
			return super.getTreeViewer();
		}
	}

	
	abstract public JenaTreeModelBase getTree();

	protected JenaTreeProvider getProvider() {
		return new JenaTreeProvider(false);
	}

	protected void hookOutline(ModelOutliner outline) {
		outline.addSelectionChangedListener(this);
		getSite().setSelectionProvider(outline);
		outline_ready = true;
	}

	public void drillTo(OntResource subject) {
		if( outline_ready)
			outline.drillTo(subject);
	}

	private ModelOutliner outline;
	private boolean outline_ready;
	protected ModelMinder models = new ModelMinder(this);
	
	/**
	 * Check that the input is an instance of <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
	}

	@Override
	public void dispose() {
		models.dispose();
		super.dispose();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if( outline == null) {
				outline = new ModelOutliner();
			}
			return outline;
		}
		return super.getAdapter(adapter);
	}
	
	private boolean guard;
	
	@Override
	public void doRefresh() {
		if( guard )
			return;
		
		guard = true;
		try {
			if( outline_ready && outline.getSelection().isEmpty())
				outline.setSelection(new TreeSelection(new TreePath(new Object[] {getTree().getRoot()})));
			
			super.doRefresh();
		}
		finally {
			guard = false;
		}
	}

	public void selectionChanged(SelectionChangedEvent event) {
		doRefresh();
	}

	public OntResource getSubject() {
		Node node = getNode();
		if( node != null) 
			return node.getSubject();
		else 
			return null;
	}

	public String getComment() {
		Node node = getNode();
		if( node != null) {
			OntResource subject = node.getSubject();
			if( subject != null) {
				OntResource defnode = subject.getIsDefinedBy();
				if( defnode != null) {
					return node.toString() + " (" + ProfileModel.label(defnode) + ")";
				}
			}
			return node.toString();
		}
		else 
			return "Nothing selected in outline";
	}

	public IFile getFile() {
		return ((IFileEditorInput)getEditorInput()).getFile();
	}

	public Node getNode() {
		if( outline_ready) {
			ISelection selection = outline.getSelection();
			if( ! selection.isEmpty() && selection instanceof IStructuredSelection) {
				IStructuredSelection structured = (IStructuredSelection) selection;
				Object raw = structured.getFirstElement();
				if( raw instanceof Node)
					return (Node)raw;
			}
		}
		return getTree().getRoot();
	}

	public OntModel getModel() {
		JenaTreeModelBase tree = getTree();
		if( tree != null)
			return tree.getOntModel();
		else
			return null;
	}
}
