/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import au.com.langdale.cimtoole.project.Cache.CacheListener;
import au.com.langdale.cimtoole.project.ModelMinder;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeProvider;
import au.com.langdale.jena.OntModelProvider;
import au.com.langdale.jena.TreeModelBase.Empty;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.ui.builder.FurnishedMultiEditor;
import au.com.langdale.ui.util.SelectionProvider;

public abstract class ModelEditor extends FurnishedMultiEditor implements CacheListener, OntModelProvider {

	abstract public JenaTreeModelBase getTree();

	public JenaTreeProvider getProvider() {
		return new JenaTreeProvider(false);
	}

	protected final ModelMinder models = new ModelMinder(this);
	protected final SelectionProvider selections = new SelectionProvider();
	
	public void listenToSelection(ISelectionProvider source) {
		source.addSelectionChangedListener(selections);
	}
	
	private ModelOutliner outline;
	
	protected ModelOutliner getOutline() {
		if( outline == null) {
			outline = new ModelOutliner(this);
		}
		return outline;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter))
			return getOutline();
		else 
		   return super.getAdapter(adapter);
	}

	private IDoubleClickListener drill = new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Node node = (Node) selection.getFirstElement();
			OntResource subject = node.getSubject();
			if(outline != null)
				outline.drillTo(subject);
		}
	};
	
	public void listenToDoubleClicks(StructuredViewer source) {
		source.addDoubleClickListener(drill);
	}
	
	/**
	 * Check that the input is an instance of <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
		getSite().setSelectionProvider(selections);
	}

	@Override
	public void dispose() {
		models.dispose();
		super.dispose();
	}
	
	private boolean guard;
	
	@Override
	public void doRefresh() {
		if( guard )
			return;
		
		guard = true;
		try {
			if( outline != null && outline.getSelection().isEmpty()) {
				TreePath root = new TreePath(new Object[] {getTree().getRoot()});
				outline.setSelection(new TreeSelection(root));
				outline.getTreeViewer().setExpandedState( root, true );
			}
			
			super.doRefresh();
		}
		finally {
			guard = false;
		}
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

	private Node previous;	

	public Node getNode() {
		if( outline != null ) {
			ISelection selection = outline.getSelection();
			if( ! selection.isEmpty() && selection instanceof IStructuredSelection) {
				IStructuredSelection structured = (IStructuredSelection) selection;
				Object raw = structured.getFirstElement();
				if( raw instanceof Node && ! (raw instanceof Empty)) {
					previous = (Node) raw;
					return previous;
				}
			}
		}
		
		if( previous != null)
			return previous;
		
		return getTree().getRoot();
	}

	public OntModel getModel() {
		JenaTreeModelBase tree = getTree();
		if( tree != null)
			return tree.getOntModel();
		else
			return null;
	}

	protected void configureOutline( ModelOutliner outline) {
		
	}
}
