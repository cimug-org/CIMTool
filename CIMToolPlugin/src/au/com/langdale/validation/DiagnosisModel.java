/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import au.com.langdale.jena.JenaTreeModelBase;

import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This is a model that represents a diagnosis graph as a tree.  
 * A diagnosis graph is an RDF document containing diagnostic
 * messages.
 */
public class DiagnosisModel extends JenaTreeModelBase {

	public static Resource DIAGNOSIS_ROOT = ResourceFactory.createResource("http://langdale.com.au/2007/Diagnosis#root");
	public static Resource DIAGNOSIS_GENERAL = ResourceFactory.createResource("http://langdale.com.au/2007/Diagnosis#general");
	/**
	 * The root of the diagnosis tree.
	 */
	public class RootNode extends Node {

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return asOntResource(DIAGNOSIS_ROOT);
		}

		@Override
		protected void populate() {
			ResIterator jt = getOntModel().listSubjectsWithProperty(LOG.hasProblems);
			while(jt.hasNext()) {
				Resource subject = jt.nextResource();
				add( new KeyNode(asOntResource(subject)));
			}
			add( new GeneralNode());
		}

	}
	/**
	 * A node to group diagnostics that are not attached to any specific subject.
	 */
	public class GeneralNode extends ModelNode {

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return asOntResource(DIAGNOSIS_GENERAL);
		}

		@Override
		protected void populate() {
			ResIterator it = getOntModel().listSubjectsWithProperty(RDF.type, LOG.Problem);
			while (it.hasNext()) {
				Resource problem = it.nextResource();
				ResIterator jt = getOntModel().listSubjectsWithProperty(LOG.hasProblems, problem);
				if( ! jt.hasNext())
					add( new DetailNode(getSubject(), asOntResource(problem)));
				jt.close();
			}
		}

		@Override
		protected String collation() {
			return "~";
		}

		@Override
		public String toString() {
			return "General Diagnostics";
		}
		
	}
	
	/**
	 * A node that groups diagnostics attached to a specific subject.  
	 */
	public class KeyNode extends ModelNode {
		
		private OntResource subject;
		
		public KeyNode(OntResource subject) {
			this.subject = subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		protected void populate() {
			StmtIterator it = subject.listProperties(LOG.hasProblems);
			while (it.hasNext()) {
				RDFNode node = it.nextStatement().getObject();
				if( node.isResource()) {
					add( new DetailNode(subject, asOntResource(node)));
				}
			}
		}
	}
	
	/**
	 * A node representing a single diagnostic message.
	 */
	public class DetailNode extends ModelNode {
		private OntResource subject, problem;
		
		public DetailNode(OntResource subject, OntResource problem) {
			this.subject = subject;
			this.problem = problem;
		}

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return problem;
		}

		@Override
		public OntResource getBase() {
			return subject;
		}

		@Override
		protected void populate() {
			// no children
		}

		@Override
		public boolean getAllowsChildren() {
			return false;
		}

		@Override
		public String toString() {
			return problem.getComment(null);
		}

		public String getDescription() {
			return problem.getProperty(LOG.problemDetail).getString();
		}
	}
	
	@Override
	protected Node classify(OntResource root) throws ConversionException {
		if( root.equals(DIAGNOSIS_ROOT))
			return new RootNode();
		else
			return null;
	}
}
