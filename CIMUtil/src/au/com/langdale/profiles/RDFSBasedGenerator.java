/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;


import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Common support for the simple owl and legacy rdfs <code>SchemaGenerator</code>s.
 */
public abstract class RDFSBasedGenerator extends SchemaGenerator {

	public RDFSBasedGenerator(OntModel profileModel, OntModel backgroundModel, boolean preserveNamespaces, boolean withInverses) {
		super(profileModel, backgroundModel, preserveNamespaces, withInverses);
	}
	
	protected OntModel result = ModelFactory.createMem();

	public OntModel getResult() {
		return result;
	}

	protected void emit(String subject, FrontsNode prop, String object) {
		if( subject != null && object != null)
			result.createResource(subject).addProperty(prop, result.createResource(object));
		else
			System.out.println("Null statement with predicate " + prop);
	}
	
	protected Resource emit(String uri, FrontsNode type) {
		OntResource node = result.createIndividual(uri, type);
		return node;
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

	@Override
	protected void emitFlag(String uri) {
		// ignored;
	}
	
	@Override
	protected void emitImport(String uri) {
		// ignored
	}
}
