/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import au.com.langdale.xmi.UML;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A <code>SchemaGenerator</code> for creating a <i>simple OWL</i> model from
 * a profile model.
 */
public class OWLGenerator extends RDFSBasedGenerator {
	private boolean useRestrictions = true;

	public OWLGenerator(OntModel profileModel, OntModel backgroundModel, String namespace, boolean preserveNamespaces, boolean withInverses) {
		super(profileModel, backgroundModel, namespace, preserveNamespaces, withInverses);
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

	@Override
	protected void emitObjectProperty(String uri, String base, String domain, String range, boolean required, boolean functional) {
		emit(uri, OWL.ObjectProperty);
		if( ! useRestrictions ) {
			emit(uri, RDFS.domain, domain);
			emit(uri, RDFS.range, range);
			if( functional )
				emit(uri, OWL.FunctionalProperty);
		}
		
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		emit(uri, OWL.inverseOf, iuri);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain, String type, String xsdtype, boolean required) {
		emit(uri, OWL.DatatypeProperty);
		emit(uri, RDFS.subPropertyOf, type);
		if( ! useRestrictions) {
			emit(uri, RDFS.domain, domain);
			emit(uri, RDFS.range, xsdtype);
			emit(uri, OWL.FunctionalProperty);
		}
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

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		emitStereotype(uri, stereo);
	}

	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		if( useRestrictions ) {
			Resource prop = result.createResource(uri);
			Resource type = result.createResource(range);
			OntResource restrict = result.createAllValuesFromRestriction(null, prop, type);
			result.createResource(domain).addSuperClass(restrict);
		}
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,	boolean functional) {
		if( useRestrictions ) {
			Resource prop = result.createResource(uri);
			if( functional && required ) 
				emitRestriction(prop, domain, OWL.cardinality);
			else if( functional )
				emitRestriction(prop, domain, OWL.maxCardinality);
			else if( required )
				emitRestriction(prop, domain, OWL.minCardinality);
		}

	}
	
	private void emitRestriction(FrontsNode prop, String domain, FrontsNode kind) {
		OntResource restrict = result.createIndividual(null, OWL.Restriction);
		restrict.addProperty(kind, 1);
		restrict.addProperty(OWL.onProperty, prop);
		result.createResource(domain).addSuperClass(restrict);
	}
}
