/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.inference.Extractor.FunctorActions;
import au.com.langdale.inference.Extractor.RuleState;
import au.com.langdale.inference.Extractor.TerminateExtractor;
import au.com.langdale.splitmodel.SplitReader;
import au.com.langdale.splitmodel.SplitReader.SplitResult;
import au.com.langdale.validation.LOG;
import au.com.langdale.validation.ValidatorUtil;

/**
 * Functor definitions for use by <code>Extractor</code>. 
 * 
 * These are different to those declared in <code>ValidationBuiltins</code> because
 * they implement <code>FunctorActions</code> and are compatible with the 
 * asynchronous design of <code>Extractor</code> and <code>SplitReader</code>. 
 */
public class StandardFunctorActions extends ValidatorUtil {
	public static final String ARGS = "incorrect number of arguments";
	public static final String INT_ARG = "first argument must be an integer";
	public static final String HEAD = "not allowed in rule head";
	public static final String BODY = "not allowed in rule body";
	public static final String NS = "http://langdale.com.au/2007/Functor#";
	
	public static final Node SUB_CLASS_OF = RDFS.subClassOf.asNode();
	public static final Node RDF_TYPE = RDF.type.asNode();
	public static final Node HAS_PROBLEMS = LOG.hasProblems.asNode();
	public static final Node COMMENT = RDFS.comment.asNode();
	public static final Node PROBLEM_PER_SUBJECT = Node.createURI(NS + "problem_per_subject");
	public static final Node OPTION = Node.createURI(NS + "Option");
	
	public static void setOption(Graph axioms, Node option, boolean state) {
		Triple flag = Triple.create(option, RDF_TYPE, OPTION);
		if( state ) 
			axioms.add(flag);
		else
			axioms.delete(flag);
	}
	
	public static boolean getOption(Graph axioms, Node option) {
		return axioms.contains(Triple.create(option, RDF_TYPE, OPTION));
	}
	
	public static void check(boolean assertion, String message) {
		
	}
	
	public static Node var2Any(Node node) {
		return SplitReader.var2Any(node);
	}
	
	public static Integer getInteger(Node node) {
		if( node.isLiteral()) {
			Object value = node.getLiteralValue();
			if( value instanceof Integer)
				return (Integer) value;
		}
		return null;
	}

	public static abstract class Test implements FunctorActions {
		public void apply(Node[] nodes, Graph model, Graph axioms, RuleState state) {
			check(false, HEAD);
		}
	}	
	
	public static abstract class SimpleTest extends Test {
		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state) {
			if( eval(nodes, axioms))
				state.dispatch();
		}

		protected abstract boolean eval(Node[] nodes, Graph axioms);
	}
	
	public static class Axiom extends SimpleTest {
		@Override
		protected boolean eval(Node[] nodes, Graph axioms) {
			check(nodes.length == 3, ARGS);
			return axioms.contains(var2Any(nodes[0]), var2Any(nodes[1]), var2Any(nodes[2]));
		}
	}

	public static class Same extends SimpleTest {
		@Override
		protected boolean eval(Node[] nodes, Graph axioms) {
			check(nodes.length == 2, ARGS);
			return nodes[0].equals(nodes[1]);
		}
	}
	
	public static class DatatypeTest extends SimpleTest {
		@Override
		protected boolean eval(Node[] nodes, Graph axioms) {
			check(nodes.length == 2, ARGS);
			return isLexicalForm(nodes[0], nodes[1]);
		}
	}
	
	public static class LiteralTest extends SimpleTest {
		@Override
		protected boolean eval(Node[] nodes, Graph axioms) {
			check(nodes.length == 1, ARGS);
			return nodes[0].isLiteral();
		}
	}
	
	public static class Not extends SimpleTest {
		private SimpleTest delegate;
		
		public Not(SimpleTest delegate) {
			this.delegate = delegate;
		}

		@Override
		protected boolean eval(Node[] nodes, Graph axioms) {
			return ! delegate.eval(nodes, axioms);
		}
	}
	
	public static class Any extends Test {
		private boolean sense;

		public Any(boolean sense) {
			this.sense = sense;
		}
		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state) {
			check(nodes.length == 3, ARGS);
			boolean axiom = axioms.contains(var2Any(nodes[0]), var2Any(nodes[1]), var2Any(nodes[2]));
			if(sense && axiom) {
				state.dispatch();
				return;
			}
			else if( (! sense) && axiom ) {
				return;
			}
			model.find(new TriplePattern(nodes[0], nodes[1], nodes[2]), new CountResult(state, sense, 1));
		}
	}
	
	public static class TypeTest extends Test {
		private boolean sense;

		public TypeTest(boolean sense) {
			this.sense = sense;
		}
		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state) {
			check(nodes.length == 2, ARGS);
			model.find(new TriplePattern(nodes[0], RDF_TYPE, Node.ANY), 
					new TypeResult(axioms, nodes[1], state, sense));
		}
	}
	
	public static class Count extends Test {
		private boolean greater, equal;

		public Count(boolean greater, boolean equal) {
			this.greater = greater;
			this.equal = equal;
		}
		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state) {
			check(nodes.length == 4, ARGS);
			Integer arg = getInteger(nodes[0]);
			check(arg != null, INT_ARG);
			int limit = (greater^equal)? arg.intValue()+1: arg.intValue();
			model.find(new TriplePattern(nodes[1], nodes[2], nodes[3]), 
					new CountResult(state, greater, limit));
		}
	}
	
	public static class CountResult implements SplitResult {
		private RuleState state;
		private boolean sense;
		private int limit;
		private int count;
		
		public CountResult(RuleState state, boolean sense, int limit) {
			this.state = state;
			this.sense = sense;
			this.limit = limit;
		}
		public boolean add(Triple result) {
			count++;
			if(count >= limit) {
				if( sense )
					state.dispatch();
				return false; // do not continue
			}
			return true;
		}
		public void close() {
			if( ! sense )
				state.dispatch();
		}
	}
	
	public static class TypeResult extends CountResult {
		private Graph axioms;
		private Node clss;

		public TypeResult(Graph axioms, Node clss, RuleState state, boolean sense) {
			super(state, sense, 1);
			this.axioms = axioms;
			this.clss = clss;
		}

		@Override
		public boolean add(Triple result) {
			if(axioms.contains(result.getObject(), SUB_CLASS_OF, clss))
				return super.add(result);
			else
				return true;
		}
	}
	
	public static class Problem implements FunctorActions {
		public static final int MAX_REPORT_SIZE = 1000;
		private static final Node FINAL_MESSAGE = Node.createLiteral("Too many problems found. First " + MAX_REPORT_SIZE + " are shown.");
		private static final int STATEMENTS_PER_PROBLEM = 5;

		public void apply(Node[] nodes, Graph model, Graph axioms, RuleState state) {
			Node subject = getReportSubject(nodes, 0, nodes.length);
			Node phrase = getReportPhrase("error", nodes, 0, nodes.length);
			
			if(subject != null && getOption(axioms, PROBLEM_PER_SUBJECT)) {
				ExtendedIterator it = model.find(subject, HAS_PROBLEMS, Node.ANY);
				while (it.hasNext()) {
					Triple report = (Triple) it.next();
					if(model.contains(report.getObject(), COMMENT, phrase))
						return;
				}
			}

			createReport(model, subject, phrase, nodes, 0, nodes.length);
			
			int size = model.size()/STATEMENTS_PER_PROBLEM;
			if( size % 100 == 0)
				System.out.println("Result count: " + size);
			
			if( size >= MAX_REPORT_SIZE) {
				createReport(model, null, FINAL_MESSAGE, new Node[0], 0, 0);
				throw new TerminateExtractor();
			}
		}

		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state) {
			check(false, BODY);
		}
	}
	
	public static class Debug implements FunctorActions {

		public void apply(Node[] nodes, Graph model, Graph axioms, RuleState state) {
			System.out.print("fired:");
			String name = state.getRule().getName();
			if(name != null) {
				System.out.print( name + ":" );
			}
			print(nodes);
		}

		public void match(Node[] nodes, SplitReader model, Graph axioms, RuleState state) {
			System.out.print("matched:");
			String name = state.getRule().getName();
			if(name != null) {
				System.out.print( name + ":" + state.getClause());
			}
			print(nodes);
			state.dispatch();
		}

		private void print(Node[] nodes) {
			for (int ix = 0; ix < nodes.length; ix++) {
				System.out.print(" " + PrintUtil.print(nodes[ix]));
			}
			System.out.println();
		}
	}
	
	public static Map create() {
		Map map = new HashMap();
		map.put("axiom", new Axiom());		
		map.put("notAxiom", new Not(new Axiom()));
		map.put("same", new Same());
		map.put("notSame", new Not(new Same()));
		map.put("datatype", new DatatypeTest());
		map.put("notDatatype", new Not( new DatatypeTest()));
		map.put("literal", new LiteralTest());
		map.put("notLiteral", new Not( new LiteralTest()));
		map.put("any", new Any(true));
		map.put("notAny", new Any(false));
		map.put("type", new TypeTest(true));
		map.put("notType", new TypeTest(false));
		map.put("countMoreThan", new Count(true, false));
		map.put("countLessThan", new Count(false, false));
		map.put("countAtLeast", new Count(true, true));
		map.put("countAtMost", new Count(false, true));
		map.put("problem", new Problem());
		map.put("debug", new Debug());
		return map;
	}
}
