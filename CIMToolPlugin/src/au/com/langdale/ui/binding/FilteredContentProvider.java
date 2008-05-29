/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** 
 * Decorates <code>ITreeContentProvider</code> and filters or flattens content
 * according to a supplied <code>Filter</code> instance.
 */
public class FilteredContentProvider implements ITreeContentProvider  {
	/**
	 * Indicate which nodes in a tree to allow, flatten or prune.
	 */
	public interface Filter {
		/**
		 * This test is applied before any node is provided.
		 * @param value: a tree node
		 * @return: true if this node should be provided (e.g. displayed)
		 */
		public boolean allow(Object value);
		/**
		 * If a node is not provided, this test indicates that its children
		 * should be inserted in its place. 
		 * @param value: a tree node
		 * @return: true if this node's children should be inserted in its place 
		 */
		public boolean flatten(Object value);
		/**
		 * If a node is provided, this test indicates that its children
		 * should not be provided.  It is equivalent to allow(child) == false
		 * for each child, but more efficient.
		 * @param value
		 * @return
		 */
		public boolean prune(Object value);
	}

	private ITreeContentProvider delegate;
	private Filter filter;
	
	/**
	 * Decorate a content provider.
	 * @param filter: the <code>Filter</code> to apply.
	 * @param delegate: the content provider to decorate.
	 */
	public FilteredContentProvider(Filter filter, ITreeContentProvider delegate) {
		this.filter = filter;
		this.delegate = delegate;
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		delegate.inputChanged(viewer, oldInput, newInput);
	}

	public Object getParent(Object element) {
		Object parent = delegate.getParent(element);
		if( parent != null) {
			if( filter.allow(parent))
				return parent;
			if( filter.flatten(parent))
				return getParent(parent);
		}
		return null;
	}

	public Object[] getChildren(Object element) {
		return apply(delegate.getChildren(element));
	}

	public Object[] getElements(Object input) {
		return apply(delegate.getElements(input));
	}

	private Object[] apply(Object[] raw) {
		ArrayList result = new ArrayList();
		for( int ix = 0; ix < raw.length; ix++)	 {
			if( filter.allow(raw[ix])) {
				result.add(raw[ix]);
			}
			else if( filter.flatten(raw[ix])) {
				Object[] subs = getChildren(raw[ix]);
				for(int iy = 0; iy < subs.length; iy++)
					result.add(subs[iy]);
			}
		}
		return result.toArray();
	}

	public boolean hasChildren(Object element) {
		//return getChildren(element).length > 0;
		return ! filter.prune(element) && delegate.hasChildren(element);
	}

	public void dispose() {
		delegate.dispose();
	}
}