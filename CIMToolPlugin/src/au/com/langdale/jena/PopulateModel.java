package au.com.langdale.jena;

import java.util.Iterator;

import au.com.langdale.jena.FilteredTreeModel.ProxyNode;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.ProfileNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.SuperTypeNode;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;

public class PopulateModel {
	private FilteredMessageModel elementTreeModel = new FilteredMessageModel() ;
	private FilteredUMLModel propertyTreeModel = new FilteredUMLModel();
	private UMLTreeModel umlTreeModel = new UMLTreeModel();
	
	private class FilteredMessageModel extends FilteredTreeModel {

		/**
		 * Allow the base node and one level. 
		 */
		@Override
		protected boolean filter(Node node) {
			return node.getParent() == getBaseNode() 
					&& ! (node instanceof SuperTypeNode);
		}
		
		public boolean contains(OntResource child) {
			Iterator it = getBaseNode().iterator();
			while(it.hasNext()) {
				ModelNode node = (ModelNode) it.next();
				if( child.equals(node.getBase()))
					return true;
			}
			return false;
		}
	}
	
	private class FilteredUMLModel extends FilteredTreeModel {

		/**
		 * Allow the base node and property nodes.
		 */
		@Override
		protected boolean filter(Node node) {
			OntResource subject = node.getSubject();
			if( elementTreeModel.contains(subject))
				return false;  // already in message defintion
			
			Node parent = node.getParent();
			return parent == getBaseNode() // first level node
					|| parent instanceof UMLTreeModel.SubClassNode // child of subclass
					|| parent instanceof UMLTreeModel.SuperClassNode // child of a superclass
					|| parent instanceof UMLTreeModel.PackageNode; // child of package
		}
	}
	
	/**
	 * Access the sub-model for elements in the message.
	 */
	public TreeModelBase getElementTreeModel() {
		return elementTreeModel;
	}
	
	/**
	 * Access the sub-model for properties not in the message.
	 */
	public TreeModelBase getPropertyTreeModel() {
		return propertyTreeModel;
	}
	
	/**
	 * Set the Jena model for the domain definitions (ie CIM).
	 */
	public void setOntModel(Model model) {
		umlTreeModel.setOntModel(model);
	}
	
	/**
	 * Set the message model node whose children will be added/removed.
	 */
	public void setBaseNode(ProfileNode base) {
		elementTreeModel.setBaseNode(base);
		umlTreeModel.setRootResource(base.getBaseClass());
		propertyTreeModel.setBaseNode(umlTreeModel.getRoot());
	}
	/**
	 * Set the message model node whose children will be added/removed.
	 */
	public void setBaseNode(EnvelopeNode base) {
		elementTreeModel.setBaseNode(base);
		umlTreeModel.setRootResource(UML.global_package);
		propertyTreeModel.setBaseNode(umlTreeModel.getRoot());
	}
	/**
	 * Set the message model node whose children will be added/removed.
	 */
	public void setBaseNode(CatalogNode base) {
		elementTreeModel.setBaseNode(base);
		umlTreeModel.setRootResource(UML.global_package);
		propertyTreeModel.setBaseNode(umlTreeModel.getRoot());
	}
	
	/**
	 * Get the message model node.
	 */
	public ProfileNode getBaseNode() {
		return (ProfileNode) elementTreeModel.getBaseNode();
	}

	/**
	 * Create a message element for the given property node.
	 */
	public void addToMessage(Node node) {
		// FIXME: this is a crude guard - need better user feedback
		if( ((ProxyNode)node).getBase() instanceof UMLTreeModel.SubClassNode)
			return;
		OntResource subject = node.getSubject();
		elementTreeModel.getRoot().create(subject);
		node.getParent().structureChanged();
	}

	/**
	 * Remove the message element for given node.
	 */
	public void removeFromMessage(Node node) {
		node.destroy();
		propertyTreeModel.getRoot().structureChanged();
	}
}
