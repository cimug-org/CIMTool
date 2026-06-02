package au.com.langdale.jena;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;

public class MappingTree extends JenaTreeModelBase {

	private static final String DEFAULT_NS = "http://example.com/MappingSchema/NoName#";

	@Override
	protected Node classify(OntResource root) {
		return new MappingRoot();
	}

	protected String getNamespace() {
		OntResource validOntology = getOntModel().getValidOntology();
		if( validOntology != null )
			return validOntology.getURI() + "#";
		else
			return DEFAULT_NS;
	}

	public class MappingRoot extends Node {

		@Override
		protected void populate() {
			populateFunctional();
			populateDatatype();
			populateSuperClass();
		}

		protected void populateSuperClass() {
			ResIterator it = getOntModel().listSubjectsWithProperty(RDFS.subClassOf);
			while( it.hasNext()) {
				OntResource rela = it.nextResource();
				ResIterator jt = rela.listSuperClasses(false);
				while( jt.hasNext()) {
					OntResource subj = jt.nextResource();
					if( subj.hasSuperClass(rela))
						add( new EquivNode(subj, rela));
					else
						add( new SuperClassNode(subj, rela));
				}
			}
		}

		protected void populateFunctional() {
			ResIterator it = getOntModel().listObjectProperties();
			while( it.hasNext()) {
				OntResource prop = it.nextResource();
				OntResource subj = prop.getRange();
				OntResource rela = prop.getDomain();
				if( rela != null && subj != null)
					add(new FunctionalNode(prop.getLocalName(), subj, rela));
			}
		}

		protected void populateDatatype() {
			ResIterator it = getOntModel().listDatatypeProperties();
			while( it.hasNext()) {
				OntResource prop = it.nextResource();
				OntResource subj = prop.getRange();
				OntResource rela = prop.getDomain();
				if( rela != null && subj != null)
					add(new DatatypeNode(prop.getLocalName(), subj, rela));
			}
		}

		@Override
		public OntResource getSubject() {
			return getOntModel().createResource(UML.global_package.asNode());
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}
	}

	public abstract class MappingNode extends ModelNode {

		protected OntResource subject, related;
		protected String name;

		public MappingNode( String name, OntResource subject, OntResource related ) {
			this.subject = subject;
			this.related = related;
			this.name = name; 
		}
		
		public abstract void create();
		public abstract void remove();
		public abstract boolean extant();
		public abstract boolean isProperty();
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		protected void populate() {
			// no children
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}
		
		public OntResource getRelated() {
			return related;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}
	}
	
	public class SuperClassNode extends MappingNode {

		public SuperClassNode(OntResource subject, OntResource related) {
			super(label(subject), subject, related);
		}
		
		public void create() {
			related.addSuperClass(subject);
		}
		
		public void remove() {
			related.removeSuperClass(subject);
		}
		
		public boolean extant() {
			return related.hasSuperClass(subject);
		}
		
		public boolean isProperty() {
			return false;
		}
		
		@Override
		public String toString() {
			return label(related) + " -> " + label(subject);
		}

	}
	
	public class EquivNode extends MappingNode {

		public EquivNode(OntResource subject, OntResource related) {
			super(label(subject), subject, related);
		}
		
		public void create() {
			related.addSuperClass(subject);
			related.addSubClass(subject);
		}
		
		public void remove() {
			related.removeSuperClass(subject);
			related.removeSubClass(subject);
		}
		
		public boolean extant() {
			return related.hasSuperClass(subject) && related.hasSubClass(subject);
		}

		public boolean isProperty() {
			return false;
		}
		
		@Override
		public String toString() {
			return label(related) + " = " + label(subject);
		}
		
	}
	
	public class FunctionalNode extends MappingNode {

		public FunctionalNode(String name, OntResource subject, OntResource related) {
			super(name, subject, related);
		}
		
		public void create() {
			remove();
			OntResource prop = getOntModel().createObjectProperty(getNamespace() + name);
			prop.addDomain(related);
			prop.addRange(subject);
			prop.addRDFType(OWL2.FunctionalProperty);
		}
		
		public void remove() {
			getOntModel().createResource(getNamespace() + name).remove();
		}
		
		public boolean extant() {
			OntResource prop = getOntModel().createResource(getNamespace() + name);
			return prop.isObjectProperty() && prop.hasProperty(RDFS.domain, related) && prop.hasProperty(RDFS.range, subject);
		}

		public boolean isProperty() {
			return true;
		}
		
		@Override
		public String toString() {
			return name + ": " + label(related) + " -> " + label(subject);
		}
	}
	
	public class DatatypeNode extends MappingNode {

		public DatatypeNode(String name, OntResource subject, OntResource related) {
			super(name, subject, related);
		}
		
		public void create() {
			remove();
			OntResource prop = getOntModel().createDatatypeProperty(getNamespace() + name);
			prop.addDomain(related);
			prop.addRange(subject);
		}
		
		public void remove() {
			getOntModel().createResource(getNamespace() + name).remove();
		}
		
		public boolean extant() {
			OntResource prop = getOntModel().createResource(getNamespace() + name);
			return prop.isDatatypeProperty() && prop.hasProperty(RDFS.domain, related) && prop.hasProperty(RDFS.range, subject);
		}

		public boolean isProperty() {
			return true;
		}
		
		@Override
		public String toString() {
			return name + ": " + label(related) + " -> " + label(subject);
		}
	}
	
	public MappingNode makeNode(String name, OntResource subject, OntResource related, boolean functional, boolean equiv) {
		if( subject != null && related != null && related.isClass()) {
			if( subject.isDatatype()) {
				return new DatatypeNode(name, subject, related);
			}
			else if( subject.isClass()) {
				if(functional)
    				return new FunctionalNode(name, subject.inModel(getOntModel()), related.inModel(getOntModel()));
				else if(equiv)
					return new EquivNode(subject.inModel(getOntModel()), related.inModel(getOntModel()));
				else
					return new SuperClassNode(subject.inModel(getOntModel()), related.inModel(getOntModel()));
			}
		}
		return null;
	}
}
