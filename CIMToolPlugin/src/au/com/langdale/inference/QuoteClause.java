/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
/**
 * Represents a rule class that applies to a quote (or submodel).
 */
public class QuoteClause implements ClauseEntry {
	private ClauseEntry clause;
	private Node quote;
	
	public QuoteClause(Node quote, ClauseEntry clause) {
		super();
		this.quote = quote;
		this.clause = clause;
	}

	public ClauseEntry getClause() {
		return clause;
	}

	public Node getQuote() {
		return quote;
	}

	public boolean sameAs(Object other) {
		if (other instanceof QuoteClause) {
			QuoteClause cand = (QuoteClause) other;
			return Node_RuleVariable.sameNodeAs(quote, cand.quote) 
					&& clause.sameAs(cand.clause);
		}
		return false;
	}

}
