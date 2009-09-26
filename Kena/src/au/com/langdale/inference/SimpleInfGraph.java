/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.FGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.BasicForwardRuleInfGraph;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.impl.RETEEngine;

/**
 * An inference graph based on the RETE forward chaining engine.
 * 
 * Unlike the RETERuleInfGraph, SimpleInfGraph will collect any
 * deduced rules for use in later inference stages.  It will apply   
 * all rules it is given, including those marked backward generated in
 * an earlier stage.
 *  
 */
public class SimpleInfGraph extends BasicForwardRuleInfGraph {
	private Set derived = new HashSet();
	
	/**
	 * Directly construct an instance (see also SimpleReasoner which is a factory).
	 * 
	 * The reasoner is a reference to the parent reasoner, or null.
	 * 
	 * The rules is a List of Rule objects and is required.
	 * 
	 * The schema is a Graph whose contents that will be added to the 
	 * internal deductions graph without directly triggering any rules. It
	 * may be null.
	 * 
	 * The data is a Graph to which the rules will be applied.
	 * 
	 * The logging flag controls logging of deductions.  
	 */
	public SimpleInfGraph(Reasoner reasoner, List rules, Graph schema, Graph data, boolean logging) {
		super(reasoner, rules, schema, data);
		setDerivationLogging(logging);
        Graph deduct = createDeductionsGraph();
        if( schema != null ) {
            for (Iterator it = schema.find(Triple.ANY); it.hasNext(); ) 
                deduct.add((Triple)it.next());
        }
		fdeductions = new FGraph( deduct);
	}
	
	@Override
	protected void instantiateRuleEngine(List rules) {
       engine = new RETEEngine(this, rules);
    }
	
	/**
	 * Compute all inferences.  If this method is not called explicity,
	 * it is called implicitly when the graph is first queried.
	 * 
	 * The RETE forward inference engine is used and all input rules,
	 * including those marked Backward, are used.
	 */
	@Override
    public synchronized void prepare() {
        if (isPrepared) 
        	return;

        isPrepared = true;
        engine.init(false, fdata);
    }

	@Override
	public void addBRule(Rule brule) {
		derived.add(brule);
	}

	@Override
	public void deleteBRule(Rule brule) {
		derived.remove(brule);
	}
	
	/**
	 * Retrieve a List or Rule objects deduced from the data and the input rules.
	 * Such rules might be input to another Reasoner/InfGraph. 
	 * 
	 * The deduced rules will have their Backward property set. 
	 */
	public List getBRules() {
		if( ! isPrepared)
			prepare();
		return new ArrayList(derived);
	}
}
