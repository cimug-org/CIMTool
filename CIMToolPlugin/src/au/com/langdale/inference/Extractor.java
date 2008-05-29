/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import au.com.langdale.splitmodel.SplitReader;
import au.com.langdale.splitmodel.SplitReader.SplitResult;
import au.com.langdale.util.Profiler.TimeSpan;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
/**
 * A rule-directed graph transformer.  
 * 
 * The primary input to the transformation is a split model (which can be very large).
 * The transformation is defined by a schema graph, rule list, and a set of functors.
 * The result is another Graph.   When used as a validator, the result may be a 
 * diagnostic graph.
 * 
 */
public class Extractor {
	/**
	 * Defines the effect of a functor when it appears as a rule clause.
	 */
	public interface FunctorActions {
		/**
		 * A functor appearing as a body clause tests its arguments.
		 * 
		 * @param nodes: the functor argument list
		 * @param model: the input model
		 * @param axioms: the schema, which should be treated as immutable
		 * @param state: a positive result is reported by calling dispatch() on this object  
		 */
		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state);
		/**
		 * A functor appearing as a head clause can contribute to the result graph.
		 * 
		 * @param nodes: the functor argument list
		 * @param model: the result graph, to which functor effects should be applied.
		 * @param axioms: the schema, which should be treated as immutable
		 * @param state: the final state of the rule. this is provided for information only.
		 */
		public void apply(Node[] nodes, Graph model, Graph axioms, RuleState state);
	}
	/**
	 * Throwable used internally to unwind and stop execution.
	 */
	public static class TerminateExtractor extends Error {
		private static final long serialVersionUID = 9117510621543001428L;
	}

	private SplitReader reader;
	private List rules;
	private Map functors;
	private Graph result;
	private Graph axioms;
	/**
	 * Instantiate.
	 * 
	 * @param reader: the (large) model to be transformed
	 * @param axioms: the schema, which is treated as immutable
	 * @param rules: the rules to be applied
	 * @param functors: the a map of functor names to FunctorAction instances 
	 */
	public Extractor(SplitReader reader, Graph axioms, List rules, Map functors) {
		this.reader = reader;
		this.rules = rules;
		this.functors = functors;
		this.axioms = axioms;
	}
	/**
	 * Execute the transformation.
	 * @throws IOException
	 */
	public void run() throws IOException {
		TimeSpan span = new TimeSpan("Extractor Run");
		result = new GraphMem();
		
		for (Iterator it = rules.iterator(); it.hasNext();) {
			CompoundRule rule = (CompoundRule) it.next();
			RuleState state = new RuleState(rule);
			state.dispatch();
		}
		
		try {
			reader.run();
		} catch (TerminateExtractor e) {
			// enough already
		}
		span.stop();
	}
	/**
	 * 
	 * @return: the result graph
	 */
	public Graph getResult() {
		return result;
	}
	/**
	 * Represents a partially matched rule.  
	 */
	public class RuleState {
		private final CompoundRule rule;
		private final PartialBinding bindings;
		private final int clause;
		
		private RuleState(CompoundRule rule) {
			this.rule = rule;
			this.bindings = new PartialBinding(rule.getNumVars());
			this.clause = 0;
		}
		
		private RuleState(RuleState parent, PartialBinding bindings) {
			this.rule = parent.rule;
			this.bindings = bindings;
			this.clause = parent.clause + 1; 
		}
		
		private RuleState(RuleState parent, CompoundRule rule) {
			this.rule = rule;
			this.bindings = new PartialBinding(parent.bindings, rule.getNumVars());
			this.clause = 0; 
		}
		
		private RuleState(RuleState parent) {
			this(parent, parent.bindings);
		}
		/**
		 * 
		 * @return: the Rule being processed.
		 */
		public CompoundRule getRule() {
			return rule;
		}
		/**
		 * 
		 * @return: the body clause number being processed 
		 */
		public int getClause() {
			return clause;
		}
		/**
		 * Indicate that this clause matches.
		 */
		public void dispatch() {
			if(clause >= rule.bodyLength()) 
				fire();
			else			
				match(reader, rule.getBodyElement(clause));
		}

		private void fire() {
			ClauseEntry[] entries = rule.getHead();
			for (int i = 0; i < entries.length; i++) {
				ClauseEntry entry = entries[i];
				if( entry instanceof TriplePattern) {
					if( rule.bodyLength() == 0)
						apply((TriplePattern) entry, axioms);
					else
						apply((TriplePattern) entry, result);
				}
				else if( entry instanceof Functor)
					apply((Functor)entry);
				else if( entry instanceof CompoundRule)
					apply((CompoundRule)entry);
			}
		}

		private void match(SplitReader context, ClauseEntry entry) {
			if( entry instanceof TriplePattern)
				match(context, (TriplePattern)entry);
			else if( entry instanceof Functor)
				match(context, (Functor)entry);
			else if( entry instanceof QuoteClause)
				match(context, (QuoteClause)entry);
		}

		private void match(SplitReader context, Functor functor) {
			FunctorActions actions = (FunctorActions) functors.get(functor.getName());
			if( actions != null )
				actions.match(functor.getBoundArgs(bindings), context, axioms, new RuleState(this));
		}

		private void match(SplitReader context, TriplePattern pattern) {
			context.find(bindings.partInstantiate(pattern), new Matcher(pattern));
		}

		private void match(SplitReader outer, QuoteClause entry) {
			Node quote = bindings.getGroundVersion(entry.getQuote());
			SplitReader context;
			try {
				context = outer.getQuote(quote);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if( context != null ) {
				match(context, entry.getClause());
			}
		}

		private void apply(CompoundRule rule) {
			RuleState state = new RuleState(this, rule);
			state.dispatch();
		}
		
		private void apply(Functor functor) {
			FunctorActions actions = (FunctorActions) functors.get(functor.getName());
			if( actions == null)
				return;
			actions.apply(functor.getBoundArgs(bindings), result, axioms, this);
		}

		private void apply(TriplePattern pattern, Graph target) {
			target.add(bindings.instantiate(pattern));
		}
		
//		public RuleState bind(Node variable, Node value) {
//			PartialBinding revised = new PartialBinding(bindings);
//			revised.bind(variable, value);
//			return new RuleState(this, revised);
//		}


		private class Matcher implements SplitResult {
			private TriplePattern pattern;

			public Matcher(TriplePattern pattern) {
				this.pattern = pattern;
			}
			public boolean add(Triple result) {
				PartialBinding revised = new PartialBinding(bindings);
				revised.bind(pattern.getSubject(), result.getSubject());
				revised.bind(pattern.getPredicate(), result.getPredicate());
				revised.bind(pattern.getObject(), result.getObject());
				RuleState state = new RuleState(RuleState.this, revised);
				state.dispatch();
				return true;
			}

			public void close() {
			}
		}
	}
}

