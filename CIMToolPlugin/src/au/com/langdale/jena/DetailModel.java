package au.com.langdale.jena;



import au.com.langdale.validation.LOG;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DetailModel extends JenaTreeModelBase {

	public abstract class DetailNode extends ModelNode {
		private OntResource subject;
		private Node child;
		
		public void setChild(Node child) {
			this.child = child;
		}

		@Override
		protected void populate() {
			add(child);
		}
		
		protected void setSubject(OntResource subject) {
			this.subject = subject;
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}
		
		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}

		@Override
		public boolean getAllowsChildren() {
			return child != null;
		}
	}

	protected class PropertyNode extends DetailNode {
		
		@Override
		public String toString() {
			return prop_label(getSubject().asProperty());
		}
	}
	
	protected class PackageNode extends DetailNode {}
	protected class ClassNode extends DetailNode {}
	protected class SubClassNode extends DetailNode {}
	protected class FunctionalNode extends PropertyNode {}
	protected class InverseNode extends PropertyNode {}
	protected class DatatypeNode extends PropertyNode {}

	private DetailNode prop_node;
	private DetailNode type_node;
	
	/**
	 * Add one of the subclasses of the range to the display.
	 */
	public void setRangeRestriction(OntResource range) {
		if( prop_node != null && range != null ) {
			OntResource declared = prop_node.getSubject().asProperty().getRange();
			if( declared == null || ! declared.equals(range)) {
				type_node = new SubClassNode();
				type_node.setSubject(range);
				prop_node.setChild(type_node);
			}
		}
	}

	@Override
	protected Node classify(OntResource root) throws ConversionException {
		DetailNode node = null;
		OntResource domain = null;
		OntProperty prop = null;
		prop_node = null;
		type_node = null;
		
		if( root.canAs(OntProperty.class)) {
			prop = root.asProperty();
			// classify the property
			if(prop.isDatatypeProperty())
				prop_node = new DatatypeNode();
			else if( prop.isFunctionalProperty()) 
				prop_node = new FunctionalNode();
			else if( prop.isInverseFunctionalProperty())
				prop_node = new InverseNode();
			else 
				prop_node = new PropertyNode();
			prop_node.setSubject(prop);
	
			// the provisional root node
			node = prop_node;
//			
//			// add a range type node
//			OntResource range = prop.getRange();
//			if( range != null && range.canAs(OntClass.class)) {
//				type_node = new ClassNode();
//				type_node.setSubject(range);
//				prop_node.setChild(type_node);
//			}
//			else
//				type_node = null;
			
			domain = prop.getDomain();
		}
		else {
			domain = root;
		}
		
		// add a domain type node
		if( domain != null && domain.canAs(OntClass.class)) {
			DetailNode domain_node = new ClassNode();
			domain_node.setSubject(domain);
			domain_node.setChild(prop_node);
			node = domain_node;
			
			// add a package node
			RDFNode def = domain.getPropertyValue(RDFS.isDefinedBy);
			if(def != null && def.canAs(OntResource.class)) {
				DetailNode pack_node = new PackageNode();
				pack_node.setSubject((OntResource) def.as(OntResource.class));
				pack_node.setChild(domain_node);
				node = pack_node;
			}
		}
		return node;
	}
}
