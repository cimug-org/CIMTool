/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import au.com.langdale.ui.NodeTraits;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class TreeModelBase {
	/**
	 * Construct an initially empty tree.
	 *
	 */
	public TreeModelBase() {
	}
	
	public interface NodeListener {

		void nodeChanged(Node node);

		void nodeStructureChanged(Node node);
		
	}
	
	private NodeListener listener;
	
	public static int create_count;
	
	public void setNodeListener(NodeListener listener) {
		this.listener = listener;
	}
	
	private Node root;
	
	/**
	 * All tree nodes provided by this model extend this class.
	 * It implements the TreeNode interface required by DefaultTreeModel
	 * and in turn provides cut-out methods written in terms of 
	 * Jena types for concrete tree nodes.
	 */
	public abstract class Node implements Comparable, NodeTraits {
		private Node parent;
		private ArrayList members;
		
		protected Node() {
			create_count++;
		}
		
		public List getChildren() {
			materialise();
			return members;
		}
		
		/** See javax.swing.tree.TreeNode */
		public Node getParent() {
			return parent;
		}

		/** See javax.swing.tree.TreeNode */
		public boolean isLeaf() {
			return members != null && members.size() == 0;
		}
		
		/**
		 * This is the central method of all tree nodes. 
		 * It is called on demand to create and sort the
		 * child nodes.
		 */
		private void materialise() {
			if( members != null)
				return;
			members = new ArrayList();
			populate();
			Collections.sort(members);
		}
		
		/**
		 *  Add one subordinate node. To be called from populate().
		 */
		protected void add(Node node) {
			if( node != null ) {
				node.parent = this;
			    members.add(node);
			}
		}
		
		/**
		 * Refresh this node.
		 *
		 */
		public void changed() {
			nodeChanged(this);
		}
		
		/**
		 * Refresh the tree below this point.
		 *
		 */
		public void structureChanged() {
			members = null;
			nodeStructureChanged(this);
		}
		
		/**
		 * Create a child node 
		 */
		public OntResource create(OntResource subject)  {
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Destroy this node 
		 */
		public void destroy()  {
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Iterate the children of this node.
		 */
		public Iterator iterator() {
			materialise();
			return members.iterator();
		}
		
		/**
		 * Find the child node with the given subject.
		 */
		public Node findChild(OntResource subject) {
			Iterator jt = iterator();
			while(jt.hasNext()) {
				Node cand = (Node) jt.next();
				if(cand.getSubject().equals(subject)) 
					return cand;
			}
			return null;
		}
		
		/**
		 * Adopt the children of another node. To be called from populate().
		 */
		protected void adopt(Node node) {
			for (Iterator it = node.iterator(); it.hasNext();) {
				add((Node) it.next());
			}
			node.members = null;
		}
		
		/**
		 * Override in concrete node classes to add() child nodes.
		 */
		protected abstract void populate();
		
		/**
		 * Natural sort order is used for Nodes.
		 */
		public int compareTo(Object other) {
			if( other instanceof Node)
				return collation().compareTo(((Node)other).collation());
			else
				return 1;
		}
		/**
		 * Override in concrete nodes to control sort order.
		 * @return a String that will serve as the sort key.
		 */
		protected String collation() {
			return toString();
		}
		
		/**
		 * Access the ontology resource underlying the tree Node.
		 * @return the onology resource
		 */
		abstract public OntResource getSubject();
		
		/**
		 * Determine whether there are semantic errors or conflicts 
		 * associated with the resource underlying the tree Node.
		 */
		abstract public boolean getErrorIndicator();

		/**
		 * The simple identifier for this node.
		 * @return
		 */
		public String getName() {
			return label(getSubject());
		}
		
		/**
		 * Access an information model resource associated with this node.
		 * For most nodes this is the same as getSubject().  For nodes in
		 * a profile model, this is the resource being profiled rather
		 * than the profile itself.
		 * 
		 */
		public OntResource getBase() {
			return getSubject();
		}

		@Override
		public String toString() {
			return getName();
		}
		
		/**
		 * A class whose name selects the icon for the node.
		 */
		public Class getIconClass() {
			return getClass();
		}

		public boolean getAllowsChildren() {
			return true;
		}
	}

	/**
	 * Empty node represents no resource but carries a message.
	 * 
	 *
	 */
	public class Empty extends Node {
		private String message;
		
		public Empty(String message) {
			this.message = message;
		}
		
		@Override
		public String toString() {
			return message;
		}
		
		@Override
		public OntResource getSubject() {
			return null;
		}
		
		@Override
		public boolean getAllowsChildren() {
			return false;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		protected void populate() {
			// no children
		}
	}
	
	/**
	 * Create a placemarker node that displays a message.
	 */
	public Node createEmpty(String message) {
		return new Empty(message); // FIXME: never used?
	}
	
	/**
	 * Utility to format a user label text for a resource. 
	 */
	public static String label(OntResource subject) {
		if( subject == null)
			return "Unknown";
			
		String result = subject.getLabel(null);
		if( result != null)
			return result;
			
		if( subject.isAnon())
			return "Unnamed";
		
		return subject.getLocalName();
	}
	
	public void nodeStructureChanged(Node node) {
		if( listener != null) 
			listener.nodeStructureChanged(node);
	}

	public void nodeChanged(Node node) {
		if( listener != null) 
			listener.nodeChanged(node);
		
	}

	/**
	 * Utility to format a user label text specifically for a property. 
	 */
	public static String prop_label(OntProperty subject) {
		String pname = label(subject);
		String tname = label(subject.getRange());
		if( pname.equals(tname) || pname.equals(tname + "s"))
			return pname;
		else
			return pname + ": " + tname;
	}

	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
		nodeStructureChanged(root);
	}
	/**
	 * Construct a list of resources representing a path starting 
	 * from the root resource of this tree to the given target.
	 */
	protected List findResourcePathTo(Resource target) {
		return null;
	}
	
	/**
	 * Find the node representing the given resource and 
	 * construct a path from the root of the tree to that node. 
	 * 
	 * This is an "informed" search that takes advantage of 
	 * the defined structure of the tree.
	 */
	public Node[] findPathTo(Resource target, boolean includeRoot) {
		List rpath = findResourcePathTo(target);
		if( rpath == null )
			return null;

		Node[] path;
		int ix;
		
		if( includeRoot ) {
			path = new Node[rpath.size()];
			path[0] = getRoot();
			ix = 1;
		}
		else {
			path = new Node[rpath.size()-1];
			ix = 0;
		}

		Node parent = getRoot();
		Iterator it = rpath.iterator();
		it.next(); // skip root resource which is not displayed
		
		while( it.hasNext()) {
			OntResource res = (OntResource) it.next();
			Node found = parent.findChild(res);
			if( found == null ) 
				return null;
			path[ix++] = found;
			parent = found;
		}
		return path;
	}
}
