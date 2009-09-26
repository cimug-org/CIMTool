/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		public void match(Node[] nodes, AsyncModel model, Graph axioms, RuleState state);
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
	/**
	 * Track the progress of rule.
	 */
	public static class RuleMonitor {
		private int entered = 1;
		private int exited = 0;
		private int fired = 0;
		
		public void enterState() {
			entered ++;
		}
		
		public void leaveState() {
			exited ++;
			if( entered == exited )
				completed();
		}
		
		public void fire() {
			fired ++;
		}
		
		public int getFired() {
			return fired;
		}
		
		public int getTotalStates() {
			return entered;
		}
		
		public int getPendingStates() {
			return entered - exited;
		}

		protected void completed() {
			
		}
	}
	
	private class Guard extends RuleMonitor {
		private final Functor block;
		
		Guard( Functor block ) {
			this.block = block;
			blocks.add(block);
		}
		
		@Override
		protected void completed() {
			blocks.remove(block);
		}
	}

	private AsyncModel reader;
	private List rules;
	private Map functors;
	private Map functions;
	private Set blocks;
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
	public Extractor(AsyncModel reader, Graph axioms, List rules, Map functors) {
		this.reader = reader;
		this.rules = rules;
		this.functors = functors;
		this.axioms = axioms;
		this.functions = new HashMap();
		this.blocks = new HashSet();
		
		registerFunctions();
	}
	
	private void registerFunctions() {
		for (Iterator it = rules.iterator(); it.hasNext();) {
			CompoundRule rule = (CompoundRule) it.next();
			if( rule.isFunction() ) {
				ClauseEntry key = rule.getBodyElement(0);
				if(functions.put(key, rule) != null)
					System.out.println("Multiple definitions for " + key);
				if(functors.containsKey(((Functor)key).getName()))
					System.out.println("Builtin conflicts with function definition " + key);
						
			}
		}
	}
	/**
	 * Execute the transformation.
	 * @throws IOException
	 */
	public void run() throws IOException {
		result = new GraphMem();
		
		for (Iterator it = rules.iterator(); it.hasNext();) {
			CompoundRule rule = (CompoundRule) it.next();
			invoke(rule);
		}
		
		try {
			reader.run();
		} catch (TerminateExtractor e) {
			// enough already
		}
	}

	private void invoke(CompoundRule rule) {
		if( rule.isFunction() )
			return;

		RuleState state = new RuleState(rule);
		state.dispatch();
	}

	private void invoke(CompoundRule rule, Node[] args) {
		if( ! rule.isFunction() ) 
			return;
			
		Functor func = (Functor) rule.getBodyElement(0);
		Node[] formals = func.getArgs();
		
		Functor block = new Functor(func.getName(), args);
		if( blocks.contains(block))
			return;

		PartialBinding bindings = bind(formals, args, rule.getNumVars());
		if( bindings == null )
			return;
			
		RuleState state = new RuleState(rule, bindings, new Guard(block));
		state.dispatch();
	}
	
	private PartialBinding bind(Node[] formals, Node[] args, int numVars) {
		if(formals.length != args.length )
			return null;

		PartialBinding bindings = new PartialBinding(numVars);
		for( int ix = 0; ix < args.length; ix++ ) {
			if(args[ix].isConcrete()) {
				bindings.bind(formals[ix], args[ix]);
			}
			else {
				return null;
			}
		}
		return bindings;
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
		private final RuleMonitor monitor;
		private final int clause;
		
		private RuleState(CompoundRule rule, PartialBinding bindings, RuleMonitor monitor) {
			this.rule = rule;
			this.bindings = bindings;
			this.clause = 1;
			this.monitor = monitor;
		}
		
		private RuleState(CompoundRule rule) {
			this.rule = rule;
			this.bindings = new PartialBinding(rule.getNumVars());
			this.clause = 0;
			this.monitor = new Alternative();
		}
		
		private RuleState(RuleState parent, PartialBinding bindings) {
			this.rule = parent.rule;
			this.bindings = bindings;
			this.clause = parent.clause + 1;
			this.monitor = parent.monitor;
			monitor.enterState();
		}
		
		private RuleState(RuleState parent, CompoundRule rule) {
			this.rule = rule;
			this.bindings = new PartialBinding(parent.bindings, rule.getNumVars());
			this.clause = 0;
			this.monitor = new Alternative();
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
		 * Bind a variable
		 */
		public boolean bind(Node var, Node value) {
			return bindings.bind(var, value);
		}
		
		/**
		 * Indicate that the current clause matches.
		 */
		public void dispatch() {
			if(clause >= rule.bodyLength()) 
				fire();
			else			
				match(reader, rule.getBodyElement(clause));
		}
		
		/**
		 * Indicates that the current clause does not match anything.
		 */
		public void cancel() {
			monitor.leaveState();
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
			monitor.fire();
			monitor.leaveState();
		}

		private void match(AsyncModel context, ClauseEntry entry) {
			if( entry instanceof TriplePattern)
				match(context, (TriplePattern)entry);
			else if( entry instanceof Functor)
				match(context, (Functor)entry);
			else if( entry instanceof QuoteClause)
				match(context, (QuoteClause)entry);
		}

		private void match(AsyncModel context, Functor functor) {
			FunctorActions actions = (FunctorActions) functors.get(functor.getName());
			if( actions != null )
				actions.match(functor.getBoundArgs(bindings), context, axioms, new RuleState(this));
			monitor.leaveState();
		}

		private void match(AsyncModel context, TriplePattern pattern) {
			context.find(bindings.partInstantiate(pattern), new Matcher(pattern));
		}

		private void match(AsyncModel outer, QuoteClause entry) {
			Node quote = bindings.getGroundVersion(entry.getQuote());
			AsyncModel context;
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
			if( actions == null) {
				CompoundRule target = (CompoundRule) functions.get(functor);
				if( target != null ) {
					invoke( target, functor.getBoundArgs(bindings));
				}
			}
			else
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
		
		private class Alternative extends RuleMonitor {
			
			@Override
			protected void completed() {
				if( getFired() == 0 && rule.getAlternative() != null) {
					RuleState state = new RuleState(RuleState.this, rule.getAlternative());
					state.dispatch();
				}
			}
		}
		

		private class Matcher implements AsyncResult {
			private TriplePattern pattern;
//			private int count = 0;

			public Matcher(TriplePattern pattern) {
				this.pattern = pattern;
			}
			public boolean add(Triple result) {
//				count ++;
				PartialBinding revised = new PartialBinding(bindings);
				revised.bind(pattern.getSubject(), result.getSubject());
				revised.bind(pattern.getPredicate(), result.getPredicate());
				revised.bind(pattern.getObject(), result.getObject());
				RuleState state = new RuleState(RuleState.this, revised);
				state.dispatch();
				return true;
			}

			public void close() {
				monitor.leaveState();
			}
		}
	}
}

