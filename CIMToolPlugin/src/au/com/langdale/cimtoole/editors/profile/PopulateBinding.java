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

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
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
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.SuperTypeNode;
import au.com.langdale.ui.binding.JenaTreeBinding;
import au.com.langdale.ui.binding.FilteredContentProvider.Filter;
import au.com.langdale.ui.plumbing.Plumbing;
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

		public void bind(String name, Plumbing plumbing, ProfileEditor master ) {
			this.master = master;
			super.bind(name, plumbing);
		}

		public void refresh() {

			Node node = master.getNode();
			subject = node.getSubject();
			
			getTree().setRootResource((OntResource)null);
			getTree().setOntModel(master.getProfileModel());
			((ProfileModel)getTree()).setBackgroundModel(master.getProjectModel());
			
			if( node instanceof ElementNode) {
				parent = node.getParent().getSubject();
				getTree().setRootResource(parent);
			}
			else if(subject != null && (subject.canAs(OntClass.class) || subject.equals(MESSAGE.Message))) {
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
				return ! bases.contains(subject) && typeCheck(node);
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
					|| value instanceof SuperClassNode
					|| value instanceof SubClassNode
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
		protected Set bases;
		protected Button control;

		public void bind(String name, String duplicates, Plumbing plumbing, ProfileEditor master) {
			this.master = master;
			this.control = (Button) plumbing.getControl(duplicates);
			super.bind(name, plumbing, duplicates);
		}

		public void refresh() {

			Node node = master.getNode();
			Resource offer;
			Filter filter;

			if (node instanceof NaturalNode) {
				offer = ((NaturalNode)node).getBaseClass();
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
			
			bases = new HashSet();
			
			boolean duplicates = (node instanceof CatalogNode) && control.getSelection();
			if(! duplicates) {
				Iterator it = node.iterator();
				while (it.hasNext()) {
					ModelNode child = (ModelNode) it.next();
					bases.add(child.getBase());
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

		public boolean previewTarget(Resource base) {
			Node[] path = getTree().findPathTo(base, false);
			if( path != null) {
				if(path.length > 1) {
					Node node = path[path.length-1];
					if( node instanceof PropertyNode || node instanceof IndividualNode) 
						path = copyOf(path, path.length-1);
				}
				getViewer().setSelection(new TreeSelection(new TreePath(path)), true);
				return ! getViewer().getSelection().isEmpty();
			}
			return false;
		}
		
		private Node[] copyOf(Node[] nodes, int length) {
			Node[] result = new Node[length];
			for (int ix = 0; ix < length; ix++) 
				result[ix] = nodes[ix];
			
			return result;
		}

		public void selectTarget(Resource target) {
			// TODO Auto-generated method stub
			
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
