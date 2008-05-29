/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
/**
 * Bind a hierarchy of resources to a tree view with checkboxes.
 */
public abstract class  JenaCheckTreeBinding extends JenaTreeBinding {

	public JenaCheckTreeBinding(JenaTreeModelBase tree) {
		super(tree);
	}

	protected CheckboxTreeViewer getCheckViewer() {
		return (CheckboxTreeViewer) getViewer();
	}

	public void refresh() {
		fillTree();
		fillChecks();
	}
	
	public void update() {
		fetchChecks();
	}

	public void reset() {
		getCheckViewer().setAllChecked(false);
	}

	public String validate() {
		return null;
	}

	protected void fillChecks() {
		fillChecks(getTree().getRoot());
	}

	private void fillChecks(Node node) {
		getCheckViewer().setChecked(node, toBeChecked(node));
		Object[] children = getProvider().getChildren(node);
		for (int ix = 0; ix < children.length; ix++) {
			Node child = (Node) children[ix];
			fillChecks(child);
		}
	}
	
	protected void fetchChecks() {
		Object[] elements = getCheckViewer().getCheckedElements();
		for (int ix = 0; ix < elements.length; ix++) {
			Object element = elements[ix];
			if( ! getCheckViewer().getGrayed(element) && element instanceof Node) {
				Node node = (Node) element;
				hasBeenChecked(node);
			}
		}
	}
	
	/**
	 * Set the root of the tree model returned by getTree().
	 */
	protected abstract void fillTree();
	/**
	 * Get the state of a node in the model.
	 * @param node: the node
	 * @return: true if the node should appear with a check, false otherwise.
	 */
	protected abstract boolean toBeChecked(Node node);
	/**
	 * Set the state of a node in the model as checked. 
	 * @param node: the node
	 */
	protected abstract void hasBeenChecked(Node node);
}