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
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.DC_11;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class OntModel {

    /**
        A PrefixMapping that contains the "standard" prefixes we know about.
    */
    public static final PrefixMapping Standard = PrefixMapping.Factory.create()
        .setNsPrefix( "rdfs", RDFS.getURI() )
        .setNsPrefix( "rdf", RDF.getURI() )
        .setNsPrefix( "dc", DC_11.getURI() )
        .setNsPrefix( "owl", OWL2.getURI() )
        .setNsPrefix( "xsd", XSD.getURI() )
        .setNsPrefix( "uml", "http://langdale.com.au/2005/UML#")
        .setNsPrefix( "msg", "http://langdale.com.au/2005/Message#")
        .lock();

	private final Graph graph;
	private PrefixMapping prefixes = Standard;

	OntModel(Graph graph) {
		this.graph = graph;
	}
	
	public void setNsPrefix(String prefix, String uri) {
		if(prefixes == Standard) {
			prefixes = PrefixMapping.Factory.create();
			prefixes.setNsPrefixes(Standard);
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
	
	public OntResource getValidOntology() {
		ResIterator it = listSubjectsWithProperty(RDF.type, OWL2.Ontology);
		if( it.hasNext()) {
			OntResource ont = it.nextResource();
			if( ! it.hasNext() && ont.isURIResource() && ! ont.getURI().contains("#"))
				return ont;
		}
		return null;
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
		return listIndividuals(OWL2.ObjectProperty);
	}
	
	public ResIterator listDatatypeProperties() {
		return listIndividuals(OWL2.DatatypeProperty);
	}
	
	public ResIterator listAnnotationProperties() {
		return listIndividuals(OWL2.AnnotationProperty);
	}
		
	public ResIterator listNamedClasses() {
		return new Wrapper( this, new NamedSubjects(graph.find(Node.ANY, RDF.type.asNode(), OWL2.Class.asNode())));
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
		OntResource result = createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(OWL2.allValuesFrom, type);
		return result;
	}
	
	public OntResource createSomeValuesFromRestriction(String uri, FrontsNode prop, FrontsNode type) {
		OntResource result = createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(OWL2.someValuesFrom, type);
		return result;
	}
	
	public OntResource createCardinalityRestriction(String uri, FrontsNode prop, int card) {
		OntResource result = createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(OWL2.cardinality, card);
		return result;
	}
	
	public OntResource createMaxCardinalityRestriction(String uri, FrontsNode prop, int card) {
		OntResource result = createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(OWL2.maxCardinality, card);
		return result;
	}
	
	public OntResource createMinCardinalityRestriction(String uri, FrontsNode prop, int card) {
		OntResource result = createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(OWL2.minCardinality, card);
		return result;
	}

	public OntResource createClass(String uri) {
		return createIndividual(uri, OWL2.Class);
	}

	public OntResource createClass() {
		return createIndividual(OWL2.Class);
	}
	
	public OntResource createOntProperty(String uri) {
		return createIndividual(uri, RDF.Property);
	}
	
	public OntResource createObjectProperty(String uri) {
		return createIndividual(uri, OWL2.ObjectProperty);
	}
	
	public OntResource createDatatypeProperty(String uri) {
		return createIndividual(uri, OWL2.DatatypeProperty);
	}
	
	public OntResource createAnnotationProperty(String uri) {
		return createIndividual(uri, OWL2.AnnotationProperty);
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
		graph.add(Triple.create(subject.asNode(), prop.asNode(), Node.createLiteral(LiteralLabelFactory.create(new Integer(value)))));
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
