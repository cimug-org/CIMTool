/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import java.util.List;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.TreeModelBase.NodeListener;
import au.com.langdale.ui.util.IconCache;

import au.com.langdale.kena.OntResource;
/**
 * A JFace content provider that links a TreeModel to a Viewer.
 * This provider in combination with a TreeModel translates a graph
 * or resources to the displayed tree.  The TreeModel is assigned
 * to the  Viewer's input, the provider to the Viewer's contentProvider.
 */
public class JenaTreeProvider implements ITreeContentProvider {
	private boolean showRoot;
	private TreeModelBase tree;
	
	private static class LabelAdapter extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return IconCache.get(element);
		}
		
		@Override
		public String getText(Object element) {
			return element.toString();
		}
	}
	
	private static class Comparer implements  IElementComparer {

		public boolean equals(Object a, Object b) {
			return a == b || a.getClass().equals(b.getClass()) && valueOf(a).equals(valueOf(b));
		}

		private Object valueOf(Object element) {
			if( element instanceof Node) {
				Node node = (Node) element;
				OntResource subject = node.getSubject();
				if( subject != null)
					return subject;
			}
			return element;
		}

		public int hashCode(Object element) {
			return valueOf(element).hashCode();
		}
	}
	
	private static class RefreshAdapter implements NodeListener {
		private TreeViewer viewer;
		
		public RefreshAdapter(TreeViewer viewer) {
			this.viewer = viewer;
		}

		public void nodeChanged(Node node) {
			viewer.update(node, null);
		}
		
		public void nodeStructureChanged(Node node) {
			TreePath[] elements = viewer.getExpandedTreePaths();
			if(node.getParent() == null)
				viewer.refresh();
			else
				viewer.refresh(node);
			viewer.setExpandedTreePaths(elements);
		}
	}
	
	public JenaTreeProvider(boolean showRoot) {
		this.showRoot = showRoot;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		dispose();
		if( newInput instanceof TreeModelBase) {
			tree = (TreeModelBase) newInput;
			if( viewer instanceof TreeViewer) 
				tree.setNodeListener(new RefreshAdapter((TreeViewer) viewer));
			if( viewer instanceof StructuredViewer) {
				StructuredViewer struct = ((StructuredViewer)viewer);
				struct.setComparer(new Comparer());
				struct.setLabelProvider(new LabelAdapter());
			}
		}
	}

	public Object getParent(Object element) {
		Node parent = ((Node)element).getParent();
		if( showRoot || parent == null || parent.getParent() != null) 
			return parent;
		else
			return null;
	}

	public Object[] getChildren(Object element) {
		if( element instanceof Node)
			return ((Node)element).getChildren().toArray();
		else
			return getElements(element);
	}

	public boolean hasChildren(Object element) {
		if( element instanceof Node) {
			Node node = (Node) element;
			return node.getAllowsChildren() && ! node.isLeaf();
		}
		return true;
	}

	public Object[] getElements(Object inputElement) {
		if( inputElement instanceof TreeModelBase) {
			TreeModelBase tree = (TreeModelBase) inputElement;
			Node root = tree.getRoot();
			if( root != null ) {
				if( showRoot )
					return new Object[] {root};

				List children = root.getChildren();
				if( ! children.isEmpty())
					return children.toArray();
			}
			return new Object[] {tree.createEmpty("No information available.")};
		}
		return new Object[0];
	}

	public void dispose() {
		if( tree != null) {
			tree.setNodeListener(null);
			tree = null;
		}
	}
	/**
	 * Utility to combine a TreeViewer, TreeModel and new JenaTreeProvider to
	 * create a display of a resource hierarchy. 
	 */
	public static void displayJenaTree(TreeViewer viewer, TreeModelBase tree) {
		displayJenaTree(viewer, tree, false);
	}
	/**
	 * Utility to combine a TreeViewer, TreeModel and new JenaTreeProvider to
	 * create a display of a resource hierarchy.
	 * Optionally display the root node. 
	 */
	public static void displayJenaTree(StructuredViewer viewer, TreeModelBase tree, boolean showRoot) {
		viewer.setContentProvider(new JenaTreeProvider(showRoot));
		viewer.setInput(tree);
	}
}