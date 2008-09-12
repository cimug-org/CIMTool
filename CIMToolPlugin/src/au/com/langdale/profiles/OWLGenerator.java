/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A <code>SchemaGenerator</code> for creating a <i>simple OWL</i> model from
 * a profile model.
 */
public class OWLGenerator extends RDFSBasedGenerator {
	private boolean useRestrictions = true;

	public OWLGenerator(OntModel profileModel, OntModel backgroundModel, String namespace, boolean withInverses) {
		super(profileModel, backgroundModel, namespace, withInverses);
		result.setNsPrefix("uml", UML.NS);
	}

	@Override
	protected void emitClass(String uri, String base) {
		emit(uri, OWL.Class);
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
		emit(uri, OWL.DatatypeProperty);
		emit(uri, RDFS.range, xsdtype);
	}
	
	private void emitRestriction(Resource prop, String domain, Property kind) {
		Resource restrict = result.createResource(OWL.Restriction);
		RDFNode node = result.createTypedLiteral(1);
		restrict.addProperty(kind, node);
		restrict.addProperty(OWL.onProperty, prop);
		result.createResource(domain).addProperty(RDFS.subClassOf, restrict);
	}

	@Override
	protected void emitObjectProperty(String uri, String base, String domain, String range, boolean required, boolean functional) {
		Resource prop = emit(uri, OWL.ObjectProperty);
		if( useRestrictions) {
			if( functional && required ) 
				emitRestriction(prop, domain, OWL.cardinality);
			else if( functional )
				emitRestriction(prop, domain, OWL.maxCardinality);
			else if( required )
				emitRestriction(prop, domain, OWL.minCardinality);
		}
		else {
			if( functional )
				emit(uri, OWL.FunctionalProperty);
		}
		
		emit(uri, RDFS.domain, domain);
		emit(uri, RDFS.range, range);
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		emit(uri, OWL.inverseOf, iuri);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain, String type, String xsdtype, boolean required) {
		Resource prop = emit(uri, OWL.DatatypeProperty);
		emit(uri, RDFS.domain, domain);
		emit(uri, RDFS.range, xsdtype);
		emit(uri, RDFS.subPropertyOf, type);
		if( useRestrictions ) { 
			if( required)
				emitRestriction(prop, domain, OWL.cardinality);
			else
				emitRestriction(prop, domain, OWL.maxCardinality);
		}
		else
			emit(uri, OWL.FunctionalProperty);
	}
	
	@Override
	protected void emitDefinedBy(String uri, String container) {
		emit(uri, RDFS.isDefinedBy, container);
	}

	@Override
	protected void emitPackage(String uri) {
		emit(uri, UML.Package);
	}
	
	@Override
	protected void emitInstance(String uri, String base, String type) {
		super.emitInstance(uri, base, type);
		// emit(type, UML.hasStereotype, UML.enumeration.getURI());
	}

	@Override
	protected void emitStereotype(String uri, String stereo) {
		emit(uri, UML.hasStereotype, stereo);
	}
}
