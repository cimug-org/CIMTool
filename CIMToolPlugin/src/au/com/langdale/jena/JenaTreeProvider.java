/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import au.com.langdale.jena.TreeModelBase.Empty;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.TreeModelBase.NodeListener;
import au.com.langdale.kena.OntResource;
import au.com.langdale.ui.util.IconCache;
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
			return IconCache.getIcons().get(element);
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
	private static class TreeState {
		
		private TreePath[] expanded;
		private ISelection selected;
		
		public TreeState(TreeViewer viewer) {
			expanded = viewer.getExpandedTreePaths();
			selected = viewer.getSelection();

		}
		
		public void applyTo( TreeViewer viewer) {
			viewer.setExpandedTreePaths(expanded);
			viewer.setSelection(selected);
			
		}
		
		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			result.append("TreeState:\n");
			for( int i = 0; i < expanded.length; i++) {
				result.append(" ");
				result.append(expanded[i].getLastSegment().toString());
				result.append("\n");
			}
			result.append("Selection:\n  ");
			result.append(selected.toString());
			result.append("\n");
			return result.toString();
		}
	}

	private static class RefreshAdapter implements NodeListener {
		private TreeViewer viewer;
		private HashMap states = new HashMap();
		private Class currentRootType = Empty.class;
		
		
		private Class getRootType(TreeViewer viewer) {
			Object input = viewer.getInput();
			if( input instanceof TreeModelBase) 
				return ((TreeModelBase)input).getRoot().getClass();
			else
				return input.getClass();
		}
		
		public RefreshAdapter(TreeViewer viewer) {
			this.viewer = viewer;
		}

		public void nodeChanged(Node node) {
			viewer.update(node, null);
		}
		
		public void nodeStructureChanged(Node node) {
			saveState();
			
			if(node.getParent() == null)
				viewer.refresh();
			else
				viewer.refresh(node);
			currentRootType = getRootType(viewer);
			
			restoreState();
		}

		private void saveState() {
			TreeState state = new TreeState(viewer);
//			System.out.println("Save: " + currentRootType + "\n" + state);
			states.put(currentRootType, state);
		}

		private void restoreState() {
			TreeState state = (TreeState) states.get(currentRootType);
//			System.out.println("Restore: " + currentRootType);
			if( state != null) {
//				System.out.println(state);
				state.applyTo(viewer);
			}
		}
	}
	
	public JenaTreeProvider(boolean showRoot) {
		this.showRoot = showRoot;
	}

	public boolean isShowRoot() {
		return showRoot;
	}

	public void setShowRoot(boolean showRoot) {
		this.showRoot = showRoot;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		dispose();
		if( newInput instanceof TreeModelBase ) {
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