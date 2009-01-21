/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;


import java.util.Iterator;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Validation operations on models.
 * 
 */

public class ConsistencyChecker {
	
	private OntModel log, target, reference;
	private String namespace;

	/**
	 * Construct validator from models.
	 * 
	 * @param target the model to be checked
	 * @param ref the reference model
	 * @param namespace the namespace defined by the reference model
	 */
	public ConsistencyChecker(OntModel target, OntModel reference, String namespace) {
		this.log = ModelFactory.createMem();
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
	public OntModel getLog() {
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
		
		Iterator it = target.getGraph().find(Node.ANY, Node.ANY, Node.ANY);
		while( it.hasNext()) {
			
			Triple t = (Triple) it.next();
			if( ! t.getObject().isLiteral() ) {
				OntResource s = target.createResource(t.getSubject());
				OntResource p = target.createResource(t.getPredicate());
				OntResource o = target.createResource(t.getObject());
				if( ! s.isAnon() &&  s.getNameSpace().equals(namespace))
					errors |= checkSubject(s, p, o);
				if( ! o.isAnon() && o.getNameSpace().equals(namespace))
					errors |= checkObject( s, p, o);
			}
		}
		
		return errors;
	}

	/**
	 * Check that the subject is defined in the reference
	 */
	private boolean checkSubject(OntResource subj, OntResource pred, OntResource obj) {
		return check(subj, obj, null);
	}

	/**
	 * Check the object is appropriately defined in the reference.
	 */
	private boolean checkObject(OntResource subj, OntResource pred, OntResource obj) {
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
	private boolean check(OntResource refNode, OntResource targetNode, FrontsNode type) {
		if( reference.contains(refNode, RDF.type, type))
			return false;
		if( ! log.contains(refNode, LOG.hasProblems))
			note( refNode, refNode.getLocalName() + " is not defined in " + namespace);
		if( targetNode.hasRDFType())
			note(targetNode, 
					targetNode.getLocalName() + " linked with " + refNode.getLocalName() +
					" which is not defined in " + namespace);
		return true;
	}
	
	/**
	 * Create an annotation in the log model.
	 */
	private void note(OntResource subj, String mesg) {
		OntResource prob = log.createIndividual(LOG.Problem);
		prob.addProperty(RDFS.comment, mesg);
		log.add(subj, LOG.hasProblems, prob);
		
		// attach problem to ancestors that will appear in the treeview
		OntResource domain = subj.getResource(RDFS.domain);
		if( domain != null ) 
			log.add( domain, LOG.hasProblems, prob);
		
		OntResource defined = subj.getResource(RDFS.isDefinedBy);
		while( defined != null)
			log.add( defined, LOG.hasProblems, prob);
	}
}
