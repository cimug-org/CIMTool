/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;


import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Common support for the simple owl and legacy rdfs <code>SchemaGenerator</code>s.
 */
public abstract class RDFSBasedGenerator extends SchemaGenerator {

	public RDFSBasedGenerator(OntModel profileModel, OntModel backgroundModel, String namespace, boolean withInverses) {
		super(profileModel, backgroundModel, namespace, withInverses);
		if(namespace != null)
			result.setNsPrefix("cim", namespace);
	}
	
	protected Model result = ModelFactory.createDefaultModel();

	public Model getResult() {
		return result;
	}

	protected void emit(String subject, Property prop, String object) {
		if( subject != null && object != null)
			result.createResource(subject).addProperty(prop, result.createResource(object));
	}
	
	protected Resource emit(String uri, Resource type) {
		return result.createResource(uri, type);
	}

	@Override
	protected void emitLabel(String uri, String label) {
		result.createResource(uri).addProperty(RDFS.label, label, (String)null);
	}

	@Override
	protected void emitComment(String uri, String base_comment, String profile_comment) {
		String comment = appendComment(base_comment, profile_comment);
		if( comment != null)
			result.createResource(uri).addProperty(RDFS.comment, comment, (String)null);
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		emit(subClass, RDFS.subClassOf, superClass);
	}

	@Override
	protected void emitInstance(String uri, String base, String type) {
		emit(uri, result.createResource(type));
	}
}
