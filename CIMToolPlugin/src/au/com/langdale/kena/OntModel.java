package au.com.langdale.kena;

import java.util.HashSet;
import java.util.Iterator;

import au.com.langdale.kena.filters.Buffer;
import au.com.langdale.kena.filters.LiteralObjects;
import au.com.langdale.kena.filters.NamedSubjects;
import au.com.langdale.kena.filters.Objects;
import au.com.langdale.kena.filters.ResourceObjects;
import au.com.langdale.kena.filters.Subjects;
import au.com.langdale.kena.filters.UniqueObjects;
import au.com.langdale.kena.filters.UniqueSubjects;
import au.com.langdale.kena.filters.UnnamedObjects;
import au.com.langdale.kena.filters.Wrapper;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class OntModel {
	private final Graph graph;
	private PrefixMapping prefixes = PrefixMapping.Standard;

	OntModel(Graph graph) {
		this.graph = graph;
	}
	
	public void setNsPrefix(String prefix, String uri) {
		if(prefixes == PrefixMapping.Standard) {
			prefixes = PrefixMapping.Factory.create();
			prefixes.setNsPrefixes(PrefixMapping.Standard);
		}
		prefixes.setNsPrefix(prefix, uri);
	}
	
	public PrefixMapping getNsPrefixMap() {
		return prefixes;
	}
	
	public Graph getGraph() {
		return graph;
	}
	
	public int size() {
		return graph.size();
	}
	
	public ResIterator listSubjectsWithNoProperty(FrontsNode prop) {
		HashSet universe = new HashSet();
		HashSet excluded = new HashSet();
		Iterator it = graph.find(Triple.ANY);
		while( it.hasNext()) {
			Triple t = (Triple) it.next();
			universe.add(t.getSubject());
			if( ! t.getObject().isLiteral())
				universe.add(t.getObject());
			if( t.getPredicate().equals(prop.asNode()))
				excluded.add(t.getSubject());
		}
		universe.removeAll(excluded);
		return new Wrapper( this, universe.iterator());
	}
	
	public ResIterator listSubjectsWithProperty(FrontsNode prop) {
		return new Wrapper( this, new UniqueSubjects(graph.find(Node.ANY, prop.asNode(), Node.ANY)));
	}

	public ResIterator listSubjectsWithProperty(FrontsNode prop, Node value) {
		return new Wrapper( this, new Subjects(graph.find(Node.ANY, prop.asNode(), value)));
	}

	public ResIterator listSubjectsWithProperty(FrontsNode prop, FrontsNode value) {
		return listSubjectsWithProperty(prop, value.asNode());
	}

	public ResIterator listSubjectsWithProperty(FrontsNode prop, String value) {
		return listSubjectsWithProperty(prop, Node.createLiteral(value, null, false));
	}

	public ResIterator listSubjectsBuffered(FrontsNode prop, FrontsNode value) {
		return new Wrapper( this, new Buffer(new Subjects(graph.find(Node.ANY, prop.asNode(), value.asNode()))));
	}
	
	public NodeIterator listObjectsOfProperty(FrontsNode subject, FrontsNode prop) {
		return new Objects(graph.find(subject.asNode(), prop.asNode(), Node.ANY));
	}
	
	public ResIterator listResourceObjectsOfProperty(FrontsNode subject, FrontsNode prop) {
		return new Wrapper(this, new ResourceObjects(graph.find(subject.asNode(), prop.asNode(), Node.ANY)));
	}
	
	public ResIterator listUnnamedObjects(FrontsNode subject) {
		return new Wrapper(this, new UnnamedObjects(graph.find(subject.asNode(), Node.ANY, Node.ANY)));
	}
	
	public NodeIterator listLiteralObjectsOfProperty(FrontsNode subject, FrontsNode prop) {
		return new LiteralObjects(graph.find(subject.asNode(), prop.asNode(), Node.ANY));
	}
	
	public ResIterator listSubjects() {
		return new Wrapper( this, new UniqueSubjects(graph.find(Triple.ANY)));
	}
	
	public NodeIterator listObjects() {
		return new UniqueObjects( graph.find(Triple.ANY));
	}
	
	public ResIterator listIndividuals(FrontsNode type) {
		return listSubjectsWithProperty( RDF.type, type );
	}
	
	public ResIterator listObjectProperties() {
		return listIndividuals(OWL.ObjectProperty);
	}
	
	public ResIterator listDatatypeProperties() {
		return listIndividuals(OWL.DatatypeProperty);
	}
	
	public ResIterator listAnnotationProperties() {
		return listIndividuals(OWL.AnnotationProperty);
	}
		
	public ResIterator listNamedClasses() {
		return new Wrapper( this, new NamedSubjects(graph.find(Node.ANY, RDF.type.asNode(), OWL.Class.asNode())));
	}
	
	public boolean contains(FrontsNode subject, FrontsNode prop, FrontsNode value) {
		return graph.contains(subject.asNode(), prop.asNode(), value.asNode());
	}
	
	public boolean contains(FrontsNode subject, FrontsNode prop, Node value) {
		return graph.contains(subject.asNode(), prop.asNode(), value);
	}
	
	public boolean contains(FrontsNode subject, FrontsNode prop, String value) {
		return graph.contains(subject.asNode(), prop.asNode(), Node.createLiteral(value, null, false));
	}
	
	public boolean contains(FrontsNode subject, FrontsNode prop, String value, String lang) {
		return graph.contains(subject.asNode(), prop.asNode(), Node.createLiteral(value, lang, false));
	}

	public boolean contains(FrontsNode subject, FrontsNode prop) {
		return graph.contains(subject.asNode(), prop.asNode(), Node.ANY);
	}

	public boolean contains(FrontsNode subject) {
		return graph.contains(subject.asNode(), Node.ANY, Node.ANY);
	}

	public OntResource createList() {
		return new OntResource(RDF.nil.asNode(), this);
	}
	
	public OntResource createList(ResIterator it) {
		OntResource result = new OntResource(RDF.nil.asNode(), this);
		while( it.hasNext())
			result = result.cons(it.nextResource());
		return result;
	}
	
	public OntResource createList(Node[] elements) {
		return createList(elements, 0, elements.length);
	}	
	
	public OntResource createList(Node[] elements, int offset1, int offset2) {
		OntResource result = new OntResource(RDF.nil.asNode(), this);
		for( int ix = offset2 - 1; ix >= offset1; ix-- ) 
			result = result.cons(elements[ix]);
		return result;
	}
	
	public OntResource createResource() {
		return new OntResource(Node.createAnon(), this);
	}
	
	public OntResource createResource(String uri) {
		return new OntResource(Node.createURI(uri), this);
	}
	
	public OntResource createResource(Node node) {
		return new OntResource(node, this);
	}
	
	public OntResource createIndividual( FrontsNode type) {
		OntResource result = new OntResource(Node.createAnon(), this);
		add(result, RDF.type, type);
		return result;
	}
	
	public OntResource createIndividual( String uri, FrontsNode type) {
		OntResource result = new OntResource(uri == null ? Node.createAnon():Node.createURI(uri), this);
		add(result, RDF.type, type);
		return result;
	}
	
	public OntResource createAllValuesFromRestriction(String uri, FrontsNode prop, FrontsNode type) {
		OntResource result = createIndividual(uri, OWL.Restriction);
		result.addProperty(OWL.onProperty, prop);
		result.addProperty(OWL.allValuesFrom, type);
		return result;
	}
	
	public OntResource createSomeValuesFromRestriction(String uri, FrontsNode prop, FrontsNode type) {
		OntResource result = createIndividual(uri, OWL.Restriction);
		result.addProperty(OWL.onProperty, prop);
		result.addProperty(OWL.someValuesFrom, type);
		return result;
	}
	
	public OntResource createCardinalityRestriction(String uri, FrontsNode prop, int card) {
		OntResource result = createIndividual(uri, OWL.Restriction);
		result.addProperty(OWL.onProperty, prop);
		result.addProperty(OWL.cardinality, card);
		return result;
	}
	
	public OntResource createMaxCardinalityRestriction(String uri, FrontsNode prop, int card) {
		OntResource result = createIndividual(uri, OWL.Restriction);
		result.addProperty(OWL.onProperty, prop);
		result.addProperty(OWL.maxCardinality, card);
		return result;
	}
	
	public OntResource createMinCardinalityRestriction(String uri, FrontsNode prop, int card) {
		OntResource result = createIndividual(uri, OWL.Restriction);
		result.addProperty(OWL.onProperty, prop);
		result.addProperty(OWL.minCardinality, card);
		return result;
	}

	public OntResource createClass(String uri) {
		return createIndividual(uri, OWL.Class);
	}

	public OntResource createClass() {
		return createIndividual(OWL.Class);
	}
	
	public OntResource createOntProperty(String uri) {
		return createIndividual(uri, RDF.Property);
	}
	
	public OntResource createObjectProperty(String uri) {
		return createIndividual(uri, OWL.ObjectProperty);
	}
	
	public OntResource createDatatypeProperty(String uri) {
		return createIndividual(uri, OWL.DatatypeProperty);
	}
	
	public OntResource createAnnotationProperty(String uri) {
		return createIndividual(uri, OWL.AnnotationProperty);
	}
	
	public void add(OntModel other) {
		graph.getBulkUpdateHandler().add(other.graph, false);
	}
	
	public void add(OntModel other, boolean excludeReifications) {
		graph.getBulkUpdateHandler().add(other.graph, ! excludeReifications);
	}

	public void add(FrontsNode subject, FrontsNode prop, FrontsNode value) {
		add(subject, prop, value.asNode());
	}

	public void add(FrontsNode subject, FrontsNode prop, String value) {
		add(subject, prop, Node.createLiteral(value, null, false));
	}

	public void add(FrontsNode subject, FrontsNode prop, String value, String lang) {
		add(subject, prop, Node.createLiteral(value, lang, false));
	}

	public void add(FrontsNode subject, FrontsNode prop, Node value) {
		graph.add(Triple.create(subject.asNode(), prop.asNode(), value));
	}

	public void add(FrontsNode subject, FrontsNode prop, int value) {
		graph.add(Triple.create(subject.asNode(), prop.asNode(), Node.createLiteral(new LiteralLabel(new Integer(value)))));
	}
	
	public void remove(FrontsNode subject, FrontsNode prop, Node value) {
		graph.delete(Triple.create(subject.asNode(), prop.asNode(), value));
	}
	
	public void remove(FrontsNode subject, FrontsNode prop, FrontsNode value) {
		graph.delete(Triple.create(subject.asNode(), prop.asNode(), value.asNode()));
	}
	
	public void remove(FrontsNode subject, FrontsNode prop) {
		graph.getBulkUpdateHandler().remove(subject.asNode(), prop.asNode(), Node.ANY);
	}
	
	public void removeSubject(FrontsNode subject) {
		graph.getBulkUpdateHandler().remove(subject.asNode(), Node.ANY, Node.ANY);
	}
	
	public void removeObject(FrontsNode value) {
		graph.getBulkUpdateHandler().remove(Node.ANY, Node.ANY, value.asNode());
	}
}
