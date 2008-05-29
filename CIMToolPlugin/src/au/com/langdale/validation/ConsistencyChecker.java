/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Validation operations on models.
 * 
 */

public class ConsistencyChecker {
	
	private Model log, target, reference;
	private String namespace;

	/**
	 * Construct validator from models.
	 * 
	 * @param target the model to be checked
	 * @param ref the reference model
	 * @param namespace the namespace defined by the reference model
	 */
	public ConsistencyChecker(Model target, Model reference, String namespace) {
		this.log = ModelFactory.createDefaultModel();
		this.target = target;
		this.reference = reference;
		this.namespace = namespace;
	}
	
	/**
	 * Retrieve a model containing error annotations.  For each
	 * inconsistent statement in the target, the log contains one
	 * or more LOG.hasProblems statements.  The subjects of the
	 *  problem is a resource in the target associated with the 
	 * inconsistent statement.  
	 * 
	 * @return the log model
	 */
	public Model getLog() {
		return log;
	}
	/**
	 * Check a target model for inconsistent use of a namespace.
	 * Any resource in the given namespace that is used in the target
	 * must be defined in the reference. Errors are recorded in the log
	 * model.
	 * 
	 * @return true if there are validation errors
	 */
	public boolean check() {
		boolean errors = false;
		
		StmtIterator it = target.listStatements();
		while( it.hasNext()) {
			
			Statement s = it.nextStatement();
			if( s.getObject() instanceof Resource ) {
				Resource o = (Resource) s.getObject();
				if( ! s.getSubject().isAnon() &&  s.getSubject().getNameSpace().equals(namespace))
					errors |= checkSubject(s.getSubject(), s.getPredicate(), o);
				if( ! o.isAnon() && o.getNameSpace().equals(namespace))
					errors |= checkObject( s.getSubject(), s.getPredicate(), o);
			}
		}
		
		return errors;
	}

	/**
	 * Check that the subject is defined in the reference
	 */
	private boolean checkSubject(Resource subj, Property pred, Resource obj) {
		return check(subj, obj, null);
	}

	/**
	 * Check the object is appropriately defined in the reference.
	 */
	private boolean checkObject(Resource subj, Property pred, Resource obj) {
		if( pred.equals(RDFS.subClassOf) ||
				pred.equals(RDFS.domain) ||
				pred.equals(RDF.type))
			return check(obj, subj, OWL.Class);
		
		else if ( pred.equals(RDFS.subPropertyOf) ) // TODO: ||	pred.equals(OWL.onProperty)
			return check(obj, subj, RDF.Property);
		
		else
			return check(obj, subj, null);
	}

	/**
	 * Check that the refNode is defined as type in the reference model and
	 * annotate targetNode otherwise.
	 */
	private boolean check(Resource refNode, Resource targetNode, Resource type) {
		if( reference.contains(refNode, RDF.type, type))
			return false;
		if( ! log.contains(refNode, LOG.hasProblems))
			note( refNode, refNode.getLocalName() + " is not defined in " + namespace);
		if( target.contains( targetNode, RDF.type, (Resource)null))
			note(targetNode, 
					targetNode.getLocalName() + " linked with " + refNode.getLocalName() +
					" which is not defined in " + namespace);
		return true;
	}
	
	/**
	 * Create an annotation in the log model.
	 */
	private void note(Resource subj, String mesg) {
		Resource prob = log.createResource(LOG.Problem);
		prob.addProperty(RDFS.comment, mesg);
		log.add(subj, LOG.hasProblems, prob);
		
		// attach problem to ancestors that will appear in the treeview
		if( subj.hasProperty(RDFS.domain)) {
			subj = subj.getProperty(RDFS.domain).getResource();
			log.add( subj, LOG.hasProblems, prob);
		}
		
		while( subj.hasProperty(RDFS.isDefinedBy)) {
			subj = subj.getProperty(RDFS.isDefinedBy).getResource();
			log.add( subj, LOG.hasProblems, prob);
		}
	}
}
