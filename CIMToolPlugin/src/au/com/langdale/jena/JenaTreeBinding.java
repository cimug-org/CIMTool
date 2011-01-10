/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

import au.com.langdale.ui.binding.FilteredContentProvider;
import au.com.langdale.ui.binding.FilteredContentProvider.Filter;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.plumbing.Binding;
/**
 * Bind a hierarchy of resources to a tree view.
 */
public abstract class  JenaTreeBinding implements Binding {
	
	private TreeViewer viewer;
	private final JenaTreeModelBase tree;
	private JenaTreeProvider unfiltered;
	private FilteredContentProvider filtered;
	/**
	 * Initialise with a TreeModel that determines how the resource hierarchy will
	 * be extracted from an RDF graph.
	 * @param tree: the TreeModel
	 */
	public JenaTreeBinding(JenaTreeModelBase tree) {
		this.tree = tree;
		unfiltered = new JenaTreeProvider(true);
		filtered = new FilteredContentProvider(FilteredContentProvider.passAll, unfiltered);
	}
	/**
	 * @param visible: true if the root node should be displayed, 
	 * false if the root node's children should be displayed at the first level.
	 */
	public void setRootVisible(boolean visible) {
		unfiltered.setShowRoot(visible);
	}
	
	/**
	 * @param filter: filter the displayed hierarchy by the given rule.
	 */
	protected void setFilter(Filter filter) {
		filtered.setFilter(filter == null? FilteredContentProvider.passAll: filter);
	}
	/**
	 * Bind to a TreeViewer.
	 * @param name: the name of the TreeViewer
	 * @param plumbing: the event plumbing to which the TreeViewer is connected. 
	 */
	public void bind(String name, Assembly plumbing) {
		bind(name, plumbing, null);
	}
	
	protected void bind(String name, Assembly plumbing, Object after) {
		viewer = (TreeViewer) plumbing.getViewer(name);
		viewer.setContentProvider(filtered);
		viewer.setInput(tree);
		plumbing.addBinding(this, after);
	}

	protected JenaTreeModelBase getTree() {
		return tree;
	}

	protected ITreeContentProvider getProvider() {
		return filtered;
	}

	protected TreeViewer getViewer() {
		return viewer;
	}
}