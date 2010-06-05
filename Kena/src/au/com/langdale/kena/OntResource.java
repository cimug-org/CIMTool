package au.com.langdale.kena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import au.com.langdale.kena.filters.ListIterator;
import au.com.langdale.kena.filters.ListResourceIterator;
import au.com.langdale.kena.filters.ObjectsExcluding;
import au.com.langdale.kena.filters.SubjectsExcluding;
import au.com.langdale.kena.filters.TransitiveIterator;
import au.com.langdale.kena.filters.Wrapper;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

public class OntResource extends Resource  {
	protected final OntModel model;

	OntResource(Node node, OntModel model) {
		super( node );
		this.model = model;
	}
	
	public OntModel getOntModel() {
		return model;
	}
	
	public OntResource inModel(OntModel model) {
		if( this.model == model)
			return this;
		return new OntResource(node, model);
	}
	
	public boolean isList() {
		return isEmpty() || (hasProperty(RDF.first) && hasProperty(RDF.rest));
	}

	public boolean isEmpty() {
		return node.equals(RDF.nil.asNode());
	}
	
	public NodeIterator listElements() {
		return new ListIterator(this);
	}
	
	public Node[] toElementArray() {
		ArrayList buffer = new ArrayList();
		NodeIterator it = listElements();
		while( it.hasNext()) {
			buffer.add(it.next());
		}
		Node[] result = new Node[buffer.size()];
		for(int ix = 0; ix < result.length; ix ++) {
			result[ix] = (Node) buffer.get(ix);
		}
		return result;
	}
	
	public ResIterator listResourceElements() {
		return new Wrapper( model, new ListResourceIterator(this));
	}
	
	public OntResource getFirstResource() {
		return getResource(RDF.first);
	}
	
	public Node getFirst() {
		return getNode(RDF.first);
	}

	public OntResource getRest() {
		return getResource(RDF.rest);
	}
	
	public boolean contains(FrontsNode element) {
		return contains(element.asNode());
	}
	
	public boolean contains(Node element) {
		for (NodeIterator it = listElements(); it.hasNext();) {
			if( element.equals(it.nextNode()))
				return true;			
		}
		return false;
	}
	
	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append(node);
		Iterator it = model.getGraph().find(node, Node.ANY, Node.ANY);
		if( it.hasNext()) {
			do {
				Triple t = (Triple) it.next();
				sb.append("\n  ");
				sb.append(t.getPredicate());
				sb.append(" = ");
				sb.append( t.getObject());
			} while( it.hasNext());
		}
		else {
			sb.append(" has no properties.");
		}
		return sb.toString();
	}
	
	public boolean isClass() {
		return hasRDFType(OWL.Class) || hasRDFType(OWL.Restriction);
	}
	
	public boolean isProperty() {
		return hasRDFType(RDF.Property) 
			|| hasRDFType(OWL.ObjectProperty)
			|| hasRDFType(OWL.DatatypeProperty)
			|| hasRDFType(OWL.AnnotationProperty)
			|| hasRDFType(OWL.OntologyProperty);
	}
	
	public boolean isObjectProperty() {
		return hasRDFType(OWL.ObjectProperty);
	}
	
	public boolean isDatatypeProperty() {
		return hasRDFType(OWL.DatatypeProperty);
	}
	
	public boolean isDatatype() {
		return hasRDFType(RDFS.Datatype);
	}
	
	public boolean isFunctionalProperty() {
		return hasRDFType(OWL.FunctionalProperty);
	}
	
	public boolean isInverseFunctionalProperty() {
		return hasRDFType(OWL.InverseFunctionalProperty);
	}
	
	public boolean isRestriction() {
		return hasRDFType(OWL.Restriction);
	}
	
	public boolean isAllValuesFromRestriction() {
		return hasProperty(OWL.allValuesFrom);
	}
	
	public boolean isSomeValuesFromRestriction() {
		return hasProperty(OWL.someValuesFrom);
	}
	
	public boolean isCardinalityRestriction() {
		return hasProperty(OWL.cardinality);
	}
	
	public boolean isMaxCardinalityRestriction() {
		return hasProperty(OWL.maxCardinality);
	}
	
	public boolean isMinCardinalityRestriction() {
		return hasProperty(OWL.minCardinality);
	}
	
	public OntResource getOnProperty() {
		return getResource(OWL.onProperty);
	}
	
	public OntResource getSomeValuesFrom() {
		return getResource(OWL.someValuesFrom);
	}
	
	public OntResource getAllValuesFrom() {
		return getResource(OWL.allValuesFrom);
	}
	
	public int getCardinality() {
		return getInteger(OWL.cardinality).intValue();
	}
	
	public int getMaxCardinality() {
		return getInteger(OWL.maxCardinality).intValue();
	}
	
	public int getMinCardinality() {
		return getInteger(OWL.minCardinality).intValue();
	}
	
	public OntResource getIsDefinedBy() {
		return getResource(RDFS.isDefinedBy);
	}
	
	public OntResource getInverseOf() {
		return getResource(OWL.inverseOf);
	}
	
	public OntResource getInverse() {
		return getSubject(OWL.inverseOf);
	}
		
	public void addProperty(FrontsNode prop, Node value) {
		model.add(this, prop, value);
	}

	public void addProperty(FrontsNode prop, FrontsNode value) {
		model.add(this, prop, value);
	}

	public void addProperty(FrontsNode prop, String value) {
		model.add(this, prop, value);
	}

	public void setProperty(FrontsNode prop, String value, String lang) {
		model.remove(this, prop);
		model.add(this, prop, value, lang);
	}

	public void setProperty(FrontsNode prop, FrontsNode value) {
		model.remove(this, prop);
		model.add(this, prop, value);
	}

	public void setProperty(FrontsNode prop, Node value) {
		model.remove(this, prop);
		model.add(this, prop, value);
	}
	
	public OntResource cons(FrontsNode element) {
		return cons(element.asNode());
	}

	public OntResource cons(Node element) {
		OntResource cell = model.createResource();
		cell.addProperty(RDF.first, element);
		cell.addProperty(RDF.rest, this);
		return cell;
	}
	
	public void removeList() {
		OntResource cell = this;
		while( cell != null && ! cell.isEmpty()) {
			OntResource old = cell;
			cell = cell.getRest();
			old.remove();
		}
	}
	
	public OntResource remove(Node element) {
		LinkedList stack = new LinkedList();
		OntResource cell = this;
		while( cell != null && ! cell.isEmpty()) {
			Node cand = cell.getFirst();
			OntResource old = cell;
			cell = cell.getRest();
			old.remove();
			if(element.equals(cand))
				break;
			if(cand != null)
				stack.add(cand);
		}
		if( cell == null )
			cell = model.createList();
		while( stack.size() > 0 ) {
			cell = cell.cons((Node)stack.remove(stack.size()-1));
		}
		return cell;
	}
	
	public void removeRecursive() {
		ResIterator it = new TransitiveIterator(this) {
			@Override
			protected ResIterator traverse(OntResource subject) {
				return model.listUnnamedObjects(subject);
			}
		};
		
		Iterator jt = it.toSet().iterator();
		while( jt.hasNext()) {
			OntResource obj = (OntResource) jt.next();
			obj.removeProperties();
		}
		remove();
	}
	
	public OntResource remove(FrontsNode element) {
		return remove(element.asNode());
	}

	public void addProperty(FrontsNode prop, String value, String lang) {
		model.add(this, prop, value, lang);
	}

	public void addComment(String value, String lang) {
		model.add(this, RDFS.comment, value, lang);
	}
	
	public void setComment(String value, String lang) {
		setProperty(RDFS.comment, value, lang);
	}
	
	public String getComment() {
		return getString(RDFS.comment);
	}
	
	public String getComment(String lang) {
		return getString(RDFS.comment, lang);
	}

	public void addLabel(String value, String lang) {
		model.add(this, RDFS.label, value, lang);
	}
	
	public void setLabel(String value, String lang) {
		setProperty(RDFS.label, value, lang);
	}
	
	public String getLabel() {
		return getString(RDFS.label);
	}
	
	public String getLabel(String lang) {
		return getString(RDFS.label, lang);
	}
	
	public void addIsDefinedBy(FrontsNode value) {
		model.add(this, RDFS.isDefinedBy, value);
	}
	
	public void addDomain(FrontsNode value) {
		model.add(this, RDFS.domain, value);
	}
	
	public OntResource getDomain() {
		return getResource(RDFS.domain);
	}
	
	public OntResource getSameAs() {
		return getResource(OWL.sameAs);
	}
	
	public OntResource getEquivalentClass() {
		return getResource(OWL.equivalentClass);
	}

	public void addRange(FrontsNode value) {
		model.add(this, RDFS.range, value);
	}
	
	public OntResource getRange() {
		return getResource(RDFS.range);
	}
	
	public void addSubClass(FrontsNode value) {
		model.add(value, RDFS.subClassOf, this);
	}
	
	public void addSuperClass(FrontsNode value) {
		model.add(this, RDFS.subClassOf, value);
	}
	
	public void addInverseOf(FrontsNode value) {
		model.add(this, OWL.inverseOf, value);
	}
	
	public void setOnProperty(FrontsNode prop) {
		setProperty(OWL.onProperty, prop);
	}
	
	public void addRDFType(FrontsNode type) {
		model.add(this, RDF.type, type);
	}
	
	public void convertToFunctionalProperty() {
		addRDFType(OWL.FunctionalProperty);
	}
	
	public void convertToInverseFunctionalProperty() {
		addRDFType(OWL.InverseFunctionalProperty);
	}
	
	public void convertToDatatypeProperty() {
		addRDFType(OWL.DatatypeProperty);
	}
	
	public void removeSubClass(FrontsNode value) {
		model.remove(value, RDFS.subClassOf, this);
	}
	
	public void removeSuperClass(FrontsNode value) {
		model.remove(this, RDFS.subClassOf, value);
	}
	
	public void remove() {
		model.removeSubject(this);
		model.removeObject(this);
	} 
	
	public void removeProperties() {
		model.removeSubject(this);
	}
	
	public void removeProperty(FrontsNode prop, Node value) {
		model.remove(this, prop, value);
	}
	
	public void removeProperty(FrontsNode prop, FrontsNode value) {
		model.remove(this, prop, value);
	}
	
	public void removeAll(FrontsNode prop) {
		model.remove(this, prop);
	}
	
	public OntResource getResource(FrontsNode prop) {
		ResIterator it = model.listResourceObjectsOfProperty(this, prop);
		if( it.hasNext()) 
			return it.nextResource();
		else
			return null;
	}
	
	public Node getNode(FrontsNode prop) {
		NodeIterator it = model.listObjectsOfProperty(this, prop);
		if( it.hasNext()) 
			return it.nextNode();
		else
			return null;
	}
		
	public String getString(FrontsNode prop) {
		NodeIterator it = model.listLiteralObjectsOfProperty(this, prop);
		if( it.hasNext()) 
			return it.nextNode().getLiteralLexicalForm();
		else
			return null;
	}
	
	public String getString(FrontsNode prop, String lang) {
		NodeIterator it = model.listLiteralObjectsOfProperty(this, prop);
		while( it.hasNext()) { 
			Node value = it.nextNode();
			if( lang == null || value.getLiteralLanguage().equals(lang))
				return value.getLiteralLexicalForm();
		}
		return null;
	}
	
	public boolean hasProperty(FrontsNode prop, FrontsNode value) {
		return model.contains(this, prop, value);
	}
	
	public boolean hasProperty(FrontsNode prop) {
		return model.contains(this, prop, Node.ANY);
	}
	
	public ResIterator listProperties(FrontsNode prop) {
		return model.listResourceObjectsOfProperty(this, prop);
	}
	
	public NodeIterator listObjects(FrontsNode prop) {
		return model.listObjectsOfProperty(this, prop);
	}
	
	public NodeIterator listLiteralProperties(FrontsNode prop) {
		return model.listLiteralObjectsOfProperty(this, prop);
	}
	
	public ResIterator listRDFTypes(boolean direct) {
		assert ! direct;
		return model.listResourceObjectsOfProperty(this, RDF.type);
	}
	
	public ResIterator listIsDefinedBy() {
		return model.listResourceObjectsOfProperty(this, RDFS.isDefinedBy);
	}
	
	public boolean hasRDFType(FrontsNode type) {
		return model.contains(this, RDF.type, type);
	}
	
	public boolean hasRDFType() {
		return model.contains(this, RDF.type);
	}
	
	public boolean hasSuperClass(FrontsNode type, boolean direct) {
		if(direct)
			return !node.equals(type.asNode()) && hasProperty(ReasonerVocabulary.directSubClassOf, type);
		else
			return hasProperty(RDFS.subClassOf, type);
	}
	
	public boolean hasSubClass(FrontsNode type, boolean direct) {
		if(direct)
			return !node.equals(type.asNode()) && model.contains(type, ReasonerVocabulary.directSubClassOf, this);
		else
			return model.contains(type, RDFS.subClassOf, this);
	}
	
    public boolean hasSuperClass( FrontsNode cls ) {
        return hasSuperClass( cls, false );
    }
	
    public boolean hasSubClass( FrontsNode cls ) {
        return hasSubClass( cls, false );
    }
    
    public ResIterator listSuperClasses(boolean direct) {
    	if(direct)
    		return listDirectObjects(ReasonerVocabulary.directSubClassOf);
    	else
    		return model.listResourceObjectsOfProperty(this, RDFS.subClassOf);
    }

	private ResIterator listDirectObjects(FrontsNode prop) {
		return new Wrapper(model, new ObjectsExcluding(node, model.getGraph().find(node, prop.asNode(), Node.ANY)));
	}
    
    public ResIterator listSubClasses(boolean direct) {
    	if(direct)
    		return listDirectSubjects(ReasonerVocabulary.directSubClassOf);
    	else
    		return model.listSubjectsWithProperty(RDFS.subClassOf, this);
    }

	private ResIterator listDirectSubjects(Property prop) {
   		return new Wrapper( model, new SubjectsExcluding(node, model.getGraph().find(Node.ANY, prop.asNode(), node)));
	}

	public ResIterator listInstances() {
		return model.listIndividuals(this);
	}

	public void addProperty(FrontsNode prop, int value) {
		model.add(this, prop, value);
	}

	public void setProperty(FrontsNode prop, int value) {
		model.remove(this, prop);
		model.add(this, prop, value);
	}
	
	public OntResource getSubject(FrontsNode prop) {
		ResIterator it = model.listSubjectsWithProperty(prop, this);
		if( it.hasNext()) 
			return it.nextResource();
		else
			return null;
	}
	
	public Integer getInteger(FrontsNode prop) {
		NodeIterator it = model.listLiteralObjectsOfProperty(this, prop);
		while( it.hasNext()) {
			Object result = it.nextNode().getLiteralValue();
			if( result instanceof Integer)
				return (Integer) result;
		}
		return null;
	}

	public boolean isEnumeratedClass() {
		return hasProperty(OWL.oneOf);
	}
	
	public OntResource getOneOf() {
		return getResource(OWL.oneOf);
	}
	
	public void setOneOf(OntResource list) {
		setProperty(OWL.oneOf, list);
	}
}
