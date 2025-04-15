/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.HashMap;
import java.util.Map;

import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.xmi.UML;

import au.com.langdale.kena.Composition;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResourceFactory;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Transform a profile model to conform with CIM/XML RDFS schema rules as defined
 * in IEC 61970-501 Ed 1.0.  
 * 
 * In the resulting profile model, each base class or property is represented
 * by at most one profile class or property.
 */
public class Reorganizer extends SchemaGenerator {
	
	protected OntModel model, result;
	protected Map classes = new HashMap();
	protected Map proxies = new HashMap();
	protected boolean useRefs;
	protected OntResource ontNode;
	
	public Reorganizer(OntModel profile, OntModel background, boolean useRefs) {
		super(profile, background);
		result = ModelFactory.createMem();
		model = Composition.merge(result, background);
		this.useRefs = useRefs;
	}
	
	public OntModel getResult() {
		return result;
	}
	
	@Override
	protected void emitHeader(String uri, String label, String comment) {
		ontNode = model.createIndividual(uri, OWL.Ontology);
		if( label != null )
			ontNode.addLabel(label, null);
		if( comment != null )
			ontNode.addComment(comment, null);
	}
	
	@Override
	protected void emitImport(String uri) {
		ontNode.addProperty(OWL.imports, model.createResource(uri));
	}

	@Override
	protected void emitFlag(String uri) {
		model.add(model.createResource(uri), RDF.type, MESSAGE.Flag);
	}

	@Override
	protected void emitClass(String uri, String base) {
		OntResource clss = model.createClass(uri);
		ProfileClass profile = new ProfileClass(clss, clss.getNameSpace());
		profile.setBaseClass(model.createResource(base));
		classes.put(uri, profile);
	}

	@Override
	protected void emitComment(String uri, String base_comment, String comment) {
		if( comment == null)
			return;
		
		OntResource subject = getResultResource(uri);
		if( subject != null )
			subject.setComment(comment, null); 
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
		// information resides in base model only
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain,
			String type, String xsdtype, boolean required) {
		OntResource prop = model.createResource(base);
		ProfileClass profile = (ProfileClass) classes.get(domain);
		
		SelectionOptions options = new SelectionOptions((required ? SelectionOption.PropertyRequired : SelectionOption.NoOp));
		OntResource proxy = profile.createAllValuesFrom(prop, options);
		proxies.put(uri, proxy);
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		// information resides in base model only
	}

	@Override
	protected void emitInstance(String uri, String base, String type) {
		ProfileClass profile = (ProfileClass) classes.get(type);
		OntResource value = model.createResource(base);
		profile.addIndividual(value);
		proxies.put(uri, value);
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		// information resides in base model only
	}

	@Override
	protected void emitLabel(String uri, String label) {
		OntResource subject = getResultResource(uri);
		if( subject != null )
			subject.setLabel(label, null); 
	}

	@Override
	protected void emitObjectProperty(String uri, OntResource profileProp, String domain,
			String range, PropertySpec propSpec) {
		boolean required = propSpec.required;
		boolean functional = propSpec.functional;
		OntResource prop = model.createResource(profileProp.getURI());
		ProfileClass profile = (ProfileClass) classes.get(domain);
		
		OntResource proxy = profile.createAllValuesFrom(prop, getSelectionOptions(required));
		proxy.addSuperClass(model.createResource(range));
		proxies.put(uri, proxy);
		
		PropertyInfo info = profile.getPropertyInfo(prop);
		if(functional)
			info.setMaxCardinality(1);
		
		ProfileClass range_profile = (ProfileClass) classes.get(range);
		if(useRefs && ! range_profile.isEnumerated())
			proxy.addProperty(UML.hasStereotype, UML.byreference);
	}

	@Override
	protected void emitPackage(String uri) {
		// information resides in base model only
	}

	@Override
	protected void emitSuperClass(String subClass, String superClass) {
		ProfileClass subprof = (ProfileClass) classes.get(subClass);
		ProfileClass superprof = (ProfileClass) classes.get(superClass);
		subprof.addSuperClass(superprof.getSubject());
	}

	@Override
	protected void emitStereotype(String uri, String stereo) {
		OntResource subject = getResultResource(uri);
		if( subject != null)
			subject.addProperty(UML.hasStereotype, ResourceFactory.createResource(stereo));
	}

	@Override
	protected void emitBaseStereotype(String uri, String iuri) {
		// ignore base stereotypes
	}
	
	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,
			boolean functional, int minCard, int maxCard) {
		// TODO Auto-generated method stub
		
	}

	private OntResource getResultResource(String uri) {
		OntResource subject = (OntResource) proxies.get(uri);
		if( subject == null )
			subject = result.createResource(uri);
		return subject;
	}

	protected SelectionOptions getSelectionOptions(boolean required) {
		return new SelectionOptions(required ? SelectionOption.PropertyRequired : SelectionOption.NoOp);
	}
	
}
