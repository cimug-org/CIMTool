package au.com.langdale.jena;

import java.util.Iterator;


import com.hp.hpl.jena.ontology.OntResource;

/**
 * Facade for a Jena TreeModel that exposes only part of the tree.
 * 
 * Nodes in the base model are reflected in the filtered model from the
 * baseNode down (see setBaseNode()).    
 * 
 * Implementations may subclass this class and fill in the filter() method
 * to perform additional filtering..
 */
public class FilteredTreeModel extends TreeModelBase {

	/**
	 * A proxy for nodes in the base model.  All characteristics
	 * of the base node are reflected here except for its children.
	 * 
	 */
	public class ProxyNode extends Node {
		protected Node base;
		
		protected ProxyNode(Node base) {
			super();
			this.base = base;
		}

		@Override
		public OntResource getSubject() {
			return base.getSubject();
		}

		@Override
		public boolean getErrorIndicator() {
			return base.getErrorIndicator();
		}

		@Override
		public int compareTo(Object other) {
			return base.compareTo(other);
		}

		@Override
		public Class getIconClass() {
			return base.getIconClass();
		}

		@Override
		public String toString() {
			return base.toString();
		}
		
		@Override
		public boolean getAllowsChildren() {
			return base.getAllowsChildren();
		}

		@Override
		public boolean isLeaf() {
			return base.isLeaf();
		}

		@Override
		public OntResource create(OntResource subject) {
			OntResource child = base.create(subject);
			structureChanged();
			return child;
		}

		@Override
		public void destroy() {
			base.destroy();
			getParent().structureChanged();
		}

		/**
		 * Filter and wrap the children of the base node.
		 */
		@Override
		protected void populate() {
			Iterator it = base.iterator();
			while(it.hasNext()) {
				Node child = (Node)it.next();
				if( filter(child))
					add(new ProxyNode(child));
			}
		}

		/**
		 * The base node for which this is a proxy.
		 */
		public Node getBase() {
			return base;
		}
	}
	
	/**
	 * Return true if the node should be included in the model.
	 */
	protected boolean filter(Node node) {
		return true;
	}
	
	/**
	 * Set the node within the base model that will be displayed
	 * as the root node in the filtered model. 
	 */
	public void setBaseNode(Node base) {
		if( base != null )
			setRoot(new ProxyNode(base));
		else 
			setRoot(null);
	}
	
	public Node getBaseNode() {
		ProxyNode root = (ProxyNode) getRoot();
		if( root == null)
			return null;
		else
			return root.getBase();
	}
}
