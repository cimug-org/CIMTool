/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.util.List;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerException;
import com.hp.hpl.jena.reasoner.rulesys.RuleReasoner;

/**
 * SimpleReasoner is a factory for SimpleInfGraph objects. 
 * 
 * The supported way to create a a SimpleInfGraph is to create a SimpleReasoner,
 * initialise its parameters, and call its bind() method. 
 * 
 * (But calling the SimpleInfGraph constructor is simpler, 
 * providing clients don't call getReasoner.)
 * 
 * Unlike other Reasoners the schema is passed uninterpreted to the 
 * resulting inference graph.  Inference will only be triggered by data
 * passed to bind().  
 */
public class SimpleReasoner implements RuleReasoner {

	private boolean logging;
	private List rules;
	private Graph schema;
	
	/** 
	 * The default constructor assumes that at least setRules() will be called
	 * before bind(). 
	 */
	public SimpleReasoner() {}
	
	/** 
	 * The minimal constructor requires a rule set. 
	 */
	public SimpleReasoner(List rules) {
		this.rules = rules;
	}
	
	/**
	 * The maximal constructor takes a list of Rule objects,
	 * an schema graph that will be added to the inference graph
	 * and a flag to control logging of inferences.
	 */
	public SimpleReasoner(List rules, Graph schema, boolean logging) {
		this.rules = rules;
		this.schema = schema;
		this.logging = logging;
	}

	public void addDescription(Model configSpec, Resource base) {
		// ignored
	}

	/**
	 * Create a SimpleInfGraph with the given data graph and the
	 * current rules, schema graph and logging flag.
	 */
	public InfGraph bind(Graph data) throws ReasonerException {
		return new SimpleInfGraph(this, rules, schema, data, logging);
	}

	/**
	 * Clone this instance replacing its schema (if any) with the given schema.
	 * The schema will be passed to any SimpleInfGraph created by a bind() 
	 * on the resulting Reasoner.
	 * 
	 * (This is in place of a conventional setter for the schema graph.) 
	 */
	public Reasoner bindSchema(Graph schema) throws ReasonerException {
		return new SimpleReasoner(rules, schema, logging);
	}

	/**
	 * Convenience method, equivalent to binSchema(Graph) but accepts 
	 * a Model.
	 */
	public Reasoner bindSchema(Model schema) throws ReasonerException {
		return bindSchema(schema.getGraph());
	}

	public Capabilities getGraphCapabilities() {
		// no capabilities
		return null;
	}

	public Model getReasonerCapabilities() {
		// no capabilities
		return null;
	}

	/**
	 * Set the logging flag that will be passed to any 
	 * SimpleInfGraph created by bind(). 
	 */
	public void setDerivationLogging(boolean logging) {
		this.logging = logging;
	}

	public void setParameter(Property parameterUri, Object value) {
		// ignored
	}

	public boolean supportsProperty(Property property) {
		// no properties
		return false;
	}

	/**
	 * The rules  that will be passed to any 
	 * SimpleInfGraph created by bind(). 
	 */
	public List getRules() {
		return rules;
	}

	/**
	 * Set or replace the rules that will be passed to any 
	 * SimpleInfGraph created by bind(). 
	 */
	public void setRules(List rules) {
		this.rules = rules;
	}
}
