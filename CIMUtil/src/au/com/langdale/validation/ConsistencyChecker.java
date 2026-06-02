/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;


import java.util.Iterator;

import au.com.langdale.inference.LOG;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.profiles.MESSAGE;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Check a profile for consistency with a base schema.
 * 
 */
public class ConsistencyChecker {
	public static String BUILDLET_NS = "http://langdale.com.au/2007/Buildlet#";

	private int errors;
	private OntModel log, target, reference;
	private String namespace;

	/**
	 * Construct validator from models.
	 * 
	 * @param target the profile to be checked
	 * @param ref the base schema
	 * @param namespace the namespace of the profile
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

	public int errorCount() {
		return errors;
	}

	private static class Terms {
		FrontsNode[] terms;
		
		Terms( FrontsNode[] terms) { this.terms = terms;}
		
		boolean includes(FrontsNode cand) {
			for(int ix = 0; ix < terms.length; ix++)
				if( terms[ix].equals(cand))
					return true;
			return false; 
		}
		
		boolean hasSubject(OntResource subj, FrontsNode pred) {
			for(int ix = 0; ix < terms.length; ix++)
				if( subj.hasProperty(pred, terms[ix]))
					return true; 
			return false; 
		}
		
		boolean hasInstance(OntResource subj) { 
			return hasSubject(subj, RDF.type); 
		}
	}
	
	private Terms Terms( FrontsNode t1 ) { return new Terms( new FrontsNode[] { t1 }); }
	private Terms Terms( FrontsNode t1, FrontsNode t2) { return new Terms( new FrontsNode[] { t1, t2 }); }
	private Terms Terms( FrontsNode t1, FrontsNode t2, FrontsNode t3) {	return new Terms( new FrontsNode[] { t1, t2, t3 }); }

	/**
	 * Check a target model for inconsistent use of a namespace.
	 * Any resource in the given namespace that is used in the target
	 * must be defined in the reference. Errors are recorded in the log
	 * model.
	 * 
	 * @return true if there are validation errors
	 */
	public void run() {
		
		Iterator it = target.getGraph().find(Node.ANY, Node.ANY, Node.ANY);
		while( it.hasNext()) {
			
			Triple t = (Triple) it.next();
			if( ! t.getObject().isLiteral() ) {
				Resource s = ResourceFactory.createResource(t.getSubject());
				Property p = ResourceFactory.createProperty(t.getPredicate());
				Resource o = ResourceFactory.createResource(t.getObject());
				if( isRefNode(s))
					checkSubject(s.inModel(reference), p, o.inModel(target));
				if( isRefNode(o))
					checkObject( s.inModel(target), p, o.inModel(reference));
			}
		}
	}
	
	/**
	 * True if the resource belongs to the reference schema (as opposed to the profile).
	 */
	private boolean isRefNode(Resource s) {
		if( s.isAnon())
			return false;
		
		String ns = s.getNameSpace();
		return ! (ns.equals(namespace) 
				|| ns.equals(OWL.NS) 
				|| ns.equals(RDFS.getURI()) 
				|| ns.equals(RDF.getURI())
				|| ns.equals(MESSAGE.NS)
				|| ns.equals(BUILDLET_NS));
	}

	/**
	 * Check that the subject is defined in the reference
	 */
	private void checkSubject(OntResource subj, Property pred, OntResource obj) {
		if( ! obj.hasRDFType()) 
			noteUndefined(subj, obj);
	}

	/**
	 * Check the object is appropriately defined in the reference.
	 */
	private void checkObject(OntResource subj, Property pred, OntResource obj) {
		if( ! obj.hasRDFType() ) 
			noteUndefined(obj, subj);
		
		else if( Terms( RDFS.subClassOf, RDFS.domain, RDF.type).includes(pred) 
				&& ! Terms(OWL.Class).hasInstance(obj)) 
			noteExpectedClass(obj, subj);
		
		else if ( Terms(RDFS.subPropertyOf, OWL.onProperty).includes(pred) 
				&& ! Terms(RDF.Property, OWL.ObjectProperty, OWL.DatatypeProperty).hasInstance(obj)) 
			noteExpectedProperty(obj, subj);
	}
	
	private void noteUndefined(Resource refNode, Resource targNode) {
		note(refNode, "is undefined in the schema", targNode);
	}
	
	private void noteExpectedClass(Resource refNode, Resource targNode) {
		note(refNode, "is not defined as a class in the schema", targNode);
	}
	
	private void noteExpectedProperty(Resource refNode, Resource targNode) {
		note(refNode, "is not defined as a property in the schema", targNode);
	}
	
	private Resource findNamedTarget(Resource targNode) {
		if( targNode.isAnon()) {
			ResIterator it = target.listSubjectsWithProperty(RDFS.subClassOf, targNode);
			OntResource parentNode;
			while( it.hasNext()) {
				parentNode = it.nextResource();
				if( ! parentNode.isAnon())
					return parentNode;
			}
			return null;
		}
		return targNode;
	}
	
	/**
	 * Create an annotation in the log model.
	 */
	private void note(Resource refNode, String mesg, Resource targNode) {
		targNode = findNamedTarget(targNode);
		OntResource prob = log.createIndividual(LOG.Problem);
		prob.addProperty( RDFS.comment, refNode.getLocalName() + " " + mesg);
		prob.addProperty( LOG.problemReference, refNode);
		prob.addProperty( LOG.problemDetail, refNode.getURI() + " " + mesg + (targNode == null? "" : ", is linked to profile " + targNode.getURI()));
		
		if( targNode != null)
			log.add( targNode, LOG.hasProblems, prob);
		errors += 1;
	}
}
