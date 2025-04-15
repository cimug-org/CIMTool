/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import au.com.langdale.cim.CIMS;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A <code>SchemaGenerator</code> for creating a <i>legacy RDFS</i> model from
 * a profile model.
 */

public class RDFSGenerator extends RDFSBasedGenerator {

	public RDFSGenerator(OntModel profileModel, OntModel backgroundModel, boolean preserveNamespaces, boolean withInverses) {
		super(profileModel, backgroundModel, preserveNamespaces, withInverses);
		result.setNsPrefix("cims", CIMS.NS);
	}
	
	@Override
	protected void emitHeader(String uri, String label, String comment) {
		
	}

	@Override
	protected void emitClass(String uri, String base) {
		emit(uri, RDFS.Class);
	}

	@Override
	protected void emitDatatype(String uri, String xsdtype) {
		emit(uri, RDFS.Class);
	}

	@Override
	protected void emitObjectProperty(String uri, OntResource baseProp, String domain, String range, PropertySpec propSpec) {
		boolean required = propSpec.required;
		boolean functional = propSpec.functional;
		
		Resource prop = emit(uri, RDF.Property);
		
		int minCard = 0; // Default min
		int maxCard = Integer.MAX_VALUE; // Default max
		
		/**
		 * If set any min or max cardinality restrictions that may exist.
		 * It is possible that none are specified and in that case the 
		 * defaults are used.
		 */ 
		ResIterator resources = model.listSubjectsWithProperty(OWL2.onProperty, baseProp);
		while (resources.hasNext()) {
			OntResource resource = resources.nextResource();
			if (resource.hasProperty(OWL2.minCardinality))
				minCard = resource.getMinCardinality();
			if (resource.hasProperty(OWL2.maxCardinality))
				maxCard = resource.getMaxCardinality();
		}
		
		emitCardinality(prop, required, functional, minCard, maxCard);
		emit( uri, RDFS.domain, domain);
		emit( uri, RDFS.range, range);
	}

	private void emitCardinality(Resource prop, boolean required,
			boolean functional) {
		Resource card;
		if(functional) {
			if(required)
				card = CIMS.M1;
			else
				card = CIMS.M0_1;
		}
		else {
			if(required)
				card = CIMS.M1_n;
			else
				card = CIMS.M0_n;
		}
		result.add(prop, CIMS.multiplicity, card);
	}
	
	private void emitCardinality(Resource prop, boolean required,
			boolean functional, int minCard, int maxCard) {
		
		Resource card;
		if(functional) {
			if(required)
				card = CIMS.M1;
			else
				card = CIMS.M0_1;
		} else {
			if(required) {
				if ((minCard == 0 || minCard == 1) && maxCard == Integer.MAX_VALUE) {
					card = CIMS.M1_n;
				} else if (maxCard == Integer.MAX_VALUE) {
					card = ResourceFactory.createResource(CIMS.NS + "M:" + minCard + "..n");	
				} else {
					card = ResourceFactory.createResource(CIMS.NS + "M:" + minCard + ".." + maxCard);
				}
			} else {
				if ((minCard == 0 || minCard == 1) && maxCard == Integer.MAX_VALUE) {
					if(minCard == 0) {
						card = CIMS.M0_n;
					} else {
						card = CIMS.M1_n;	
					}
				} else if (maxCard == Integer.MAX_VALUE) {
					card = ResourceFactory.createResource(CIMS.NS + "M:" + minCard + "..n");	
				} else {
					card = ResourceFactory.createResource(CIMS.NS + "M:" + minCard + ".." + maxCard);
				}
			}
		}
		
		result.add(prop, CIMS.multiplicity, card);
	}

	@Override
	protected void emitInverse(String uri, String iuri) {
		emit(uri, CIMS.inverseRoleName, iuri);
	}

	@Override
	protected void emitDatatypeProperty(String uri, String base, String domain, String type, String xsdtype, boolean required) {
		Resource prop = emit(uri, RDF.Property);
		emitCardinality(prop, required, true);
		emit( uri, RDFS.domain, domain);
		emit( uri, CIMS.dataType, type != null? type: xsdtype);
	}

	@Override
	protected void emitDefinedBy(String uri, String container) {
		emit(uri, CIMS.belongsToCategory, container);
	}

	@Override
	protected void emitPackage(String uri) {
		emit(uri, CIMS.ClassCategory);
	}

	@Override
	protected void emitStereotype(String uri, String stereo) {
		emit(uri, CIMS.stereotype, stereo);
	}

	@Override
	protected void emitBaseStereotype(String uri, String stereo) {
		emitStereotype(uri, stereo);
	}

	@Override
	protected void emitRestriction(String uri, String domain, String range) {
		// ignored
	}

	@Override
	protected void emitRestriction(String uri, String domain, boolean required,	boolean functional, int minCard, int maxCard) {
		// ignored
	}

}
