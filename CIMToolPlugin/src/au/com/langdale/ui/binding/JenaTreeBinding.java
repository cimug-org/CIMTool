/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.ui.binding.FilteredContentProvider.Filter;
import au.com.langdale.ui.plumbing.Binding;
import au.com.langdale.ui.plumbing.Plumbing;
/**
 * Bind a hierarchy of resources to a tree view.
 */
public abstract class  JenaTreeBinding implements Binding {
	
	private TreeViewer viewer;
	private final JenaTreeModelBase tree;
	private ITreeContentProvider provider, unfiltered;
	private Filter filter;
	/**
	 * Initialise with a TreeModel that determines how the resource hierarchy will
	 * be extracted from an RDF graph.
	 * @param tree: the TreeModel
	 */
	public JenaTreeBinding(JenaTreeModelBase tree) {
		this.tree = tree;
		provider = unfiltered = new JenaTreeProvider(true);
	}
	/**
	 * @param visible: true if the root node should be displayed, 
	 * false if the root node's children should be displayed at the first level.
	 */
	public void setRootVisible(boolean visible) {
		unfiltered = new JenaTreeProvider(visible);
		resetProvider();
	}
	
	/**
	 * @param filter: filter the displayed hierarchy by the given rule.
	 */
	protected void setFilter(Filter filter) {
		if( this.filter != filter ) {
			this.filter = filter;
			resetProvider();
		}
	}

	private void resetProvider() {
		if( filter != null)
			provider = new FilteredContentProvider(filter, unfiltered);
		else
			provider = unfiltered;
		if( viewer != null)
			viewer.setContentProvider(provider);
	}
	/**
	 * Bind to a TreeViewer.
	 * @param name: the name of the TreeViewer
	 * @param plumbing: the event plumbing to which the TreeViewer is connected. 
	 */
	public void bind(String name, Plumbing plumbing) {
		viewer = (TreeViewer) plumbing.getViewer(name);
		viewer.setContentProvider(provider);
		viewer.setInput(tree);
		plumbing.addBinding(this);
	}

	protected JenaTreeModelBase getTree() {
		return tree;
	}

	protected ITreeContentProvider getProvider() {
		return provider;
	}

	protected TreeViewer getViewer() {
		return viewer;
	}
}