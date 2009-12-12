/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Button;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.JenaTreeBinding;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.UMLTreeModel.ClassNode;
import au.com.langdale.jena.UMLTreeModel.DatatypeNode;
import au.com.langdale.jena.UMLTreeModel.EnumClassNode;
import au.com.langdale.jena.UMLTreeModel.IndividualNode;
import au.com.langdale.jena.UMLTreeModel.PackageNode;
import au.com.langdale.jena.UMLTreeModel.PropertyNode;
import au.com.langdale.jena.UMLTreeModel.SubClassNode;
import au.com.langdale.jena.UMLTreeModel.SuperClassNode;
import au.com.langdale.jena.UMLTreeModel.ExtensionNode;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.SuperTypeNode;
import au.com.langdale.ui.binding.FilteredContentProvider.Filter;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.xmi.UML;

public class PopulateBinding  {

	public static class LeftBinding extends JenaTreeBinding {

		private class DepthOne implements Filter {

			public boolean allow(Object value) {
				if( value instanceof SuperTypeNode)
					return false;
				
				Node node = (Node) value;
				Node pnode = node.getParent();
				
				if( pnode == null)
					return false;
				
				OntResource cand = pnode.getSubject();
				return cand != null && cand.equals(subject);
			}

			public boolean flatten(Object value) {
				Node node = (Node) value;
				OntResource cand = node.getSubject();
				return cand != null && (cand.equals(parent) || cand.equals(subject));
			}
			
			public boolean prune(Object value) {
				return true;
			}
			
		}
		
		public LeftBinding() {
			super(new ProfileModel());
			setFilter(new DepthOne());
		}

		ProfileEditor master;
		private OntResource subject;
		private OntResource parent;

		public void bind(String name, Assembly plumbing, ProfileEditor master ) {
			this.master = master;
			super.bind(name, plumbing);
		}

		public void refresh() {

			Node node = master.getNode();
			subject = node.getSubject();
			
			ProfileModel tree = (ProfileModel)getTree();
			tree.setRootResource((OntResource)null);
			tree.setNamespace(master.getNamespace());
			tree.setOntModel(master.getProfileModel());
			tree.setBackgroundModel(master.getProjectModel());
			
			if( node instanceof ElementNode) {
				parent = node.getParent().getSubject();
				getTree().setRootResource(parent);
			}
			else if(subject != null && (subject.isClass() || subject.equals(MESSAGE.profile))) {
				parent = null;
				getTree().setRootResource(subject);
			}
		}

		public void reset() {
			
		}

		public void update() {
			
		}

		public String validate() {
			return null;
		}	
	}

	public static class RightBinding extends JenaTreeBinding implements Searchable {
		
		private abstract class BasicFilter implements Filter {
			
			public boolean allow(Object value) {
				Node node = (Node)value;
				OntResource subject = node.getSubject();
				return ! excluded.contains(subject) && typeCheck(node);
			}
			
			protected abstract boolean typeCheck(Node value);

			public boolean flatten(Object value) {
				Node node = (Node)value;
				return node.getParent() == null;
			}
		}
		
		private class ElementFilter extends BasicFilter {
			@Override
			public boolean typeCheck(Node value) {
				return value instanceof ClassNode 
	    			|| value instanceof EnumClassNode
					|| value instanceof SubClassNode;
			}

			@Override
			public boolean flatten(Object value) {
				return false;
			}
			
			public boolean prune(Object value) {
				return value instanceof EnumClassNode;
			}
		}
		
		private class NaturalFilter extends BasicFilter {
			@Override
			protected boolean typeCheck(Node value) {
				if(value.getParent() instanceof PropertyNode)
					return false;
				
				return value instanceof PropertyNode 
					|| value instanceof DatatypeNode
    				|| value instanceof ExtensionNode
					|| (value instanceof SuperClassNode) && showSuper.getSelection()
					|| (value instanceof SubClassNode) && showSub.getSelection()
					|| value instanceof IndividualNode;
			}
			
			public boolean prune(Object value) {
				return value instanceof PropertyNode 
					|| value instanceof IndividualNode
					|| value instanceof DatatypeNode;
			}
		}
		
		private class RootFilter extends BasicFilter {
			@Override
			public boolean typeCheck(Node value) {
				return value instanceof ClassNode
				    || value instanceof EnumClassNode
					|| value instanceof PackageNode;
			}
			
			public boolean prune(Object value) {
				return value instanceof ClassNode;
			}
		}
		
		public RightBinding() {
			super(new UMLTreeModel());
		}
		
		ProfileEditor master;
		protected Set excluded;
		protected Button showDups, showSuper, showSub;

		public void bind(String name, String duplicates, String supers, String subs, Assembly plumbing, ProfileEditor master) {
			this.master = master;
			showDups = (Button) plumbing.getControl(duplicates);
			showSuper = (Button) plumbing.getControl(supers);
			showSub = (Button) plumbing.getControl(subs);
			super.bind(name, plumbing);
		}

		public void refresh() {

			Node node = master.getNode();
			Resource offer;
			Filter filter;

			if (node instanceof NaturalNode) {
				offer = ((NaturalNode)node).getBaseClass();
				if( OWL.Thing.equals(offer))
					offer = null;
				filter = new NaturalFilter();
			}
			else if( node instanceof ElementNode) {
				offer = ((ElementNode)node).getBaseProperty().getRange();
				filter = new ElementFilter();
			}
			else if( node instanceof CatalogNode || node instanceof EnvelopeNode) {
				offer = UML.global_package;
				filter = new RootFilter();
			}
			else {
				offer = null;
				filter = null;
			}
			
			excluded = new HashSet();
			
			boolean duplicates = (node instanceof CatalogNode) && showDups.getSelection();
			if(! duplicates) {
				Iterator it = node.iterator();
				while (it.hasNext()) {
					ModelNode child = (ModelNode) it.next();
					if( ! (child instanceof SuperTypeNode))
					    excluded.add(child.getBase());
				}
			}

			TreePath[] elements = getViewer().getExpandedTreePaths();
			getTree().setRootResource((OntResource)null);
			getTree().setOntModel(master.getProjectModel());
			setFilter(filter);
			getTree().setRootResource(offer);
			getViewer().setExpandedTreePaths(elements);
		}
		
		public OntModel getOntModel() {
			return getTree().getOntModel();
		}

		public Node findNode(Resource target) {
			Node[] path = getTree().findPathTo(target, false);
			return path != null? path[path.length-1]: null;
		}

		public void previewTarget(Node node) {
			Filter filter = new RootFilter();
			Node[] path = truncate(node.getPath(false), filter);
			getViewer().setSelection(new TreeSelection(new TreePath(path)), true);
		}

		public String getDescription() {
			return "Search the schema for packages or classes by their name or the name of a member.";
		}

		private Node[] truncate(Node[] nodes, Filter filter) {
			int length = nodes.length; 
			while( length > 0 && ! filter.allow(nodes[length-1])) 
				length--;

			Node[] result = new Node[length];
			for (int ix = 0; ix < length; ix++)
				result[ix] = nodes[ix];

			return result;
		}

		public Property getCriterion() {
			return  ResourceFactory.createProperty(RDFS.label);
		}	
		
		public void reset() {
			
		}

		public void update() {
			
		}

		public String validate() {
			return null;
		}
	}
}
