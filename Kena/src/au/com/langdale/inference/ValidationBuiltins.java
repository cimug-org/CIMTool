/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
/**
 * Custom builtins for the Jena rule based inference engine.
 */
public class ValidationBuiltins extends Reporting {
    
	public static abstract class VarArgsBuiltin extends BaseBuiltin {
		
		protected abstract int getMinArgs();
		
		@Override
		public void checkArgs(int length, RuleContext context) {
	        if (length < getMinArgs()) {
	            throw new BuiltinException(this, context, "requires at least " + getMinArgs() + " arguments but has " + length);
	        }
		}
	}

	public static abstract class ValidationBuiltin extends VarArgsBuiltin {
		@Override
		public void headAction(Node[] args, int length, RuleContext context) {
			if( bodyCall(args, length, context))
					return;
			
			int offset1 = getMinArgs();
			Node subject = getReportSubject(args, offset1, length);
			Node phrase = getReportPhrase(getName(), args, offset1, length);
			Graph graph = context.getGraph().getDeductionsGraph();
			createReport(graph, subject, phrase, args, offset1, length);
		}
	}
	
	public static class Problem extends ValidationBuiltin {

		@Override
		protected int getMinArgs() {
			return 0;
		}

		public String getName() {
			return "problem";
		}
		
	    @Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
	    	return false;
	    }
	}

	public static class DatatypeTest extends ValidationBuiltin {
	
		public String getName() {
			return "datatype";
		}
		
		@Override
		protected int getMinArgs() {
			return 2;
		}
		
		@Override
	    public boolean bodyCall(Node[] args, int length, RuleContext context) {
	        checkArgs(length, context);
	        return isLexicalForm(args[0], args[1]);
	    }
	}

	public static class NotDatatypeTest extends ValidationBuiltin {
	
		public String getName() {
			return "notDatatype";
		}
		
		@Override
		protected int getMinArgs() {
			return 2;
		}
		
		@Override
	    public boolean bodyCall(Node[] args, int length, RuleContext context) {
	        checkArgs(length, context);
	        return ! isLexicalForm(args[0], args[1]);
	    }
	}

	public static class FactTest extends ValidationBuiltin {
	
		public String getName() {
			return "axiom";
		}
		
		@Override
		protected int getMinArgs() {
			return 3;
		}
	
		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
	        checkArgs(length, context);
	        return context.contains(args[0], args[1], args[2]);
		}
	}

	public static class NotFactTest extends ValidationBuiltin {
	
		public String getName() {
			return "notAxiom";
		}
		
		@Override
		protected int getMinArgs() {
			return 3;
		}

		@Override
		public boolean isMonotonic() {
			return false;
		}

		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
	        checkArgs(length, context);
	        return ! context.contains(args[0], args[1], args[2]);
		}
	}

	public static class SameTest extends ValidationBuiltin {
	
		public String getName() {
			return "same";
		}
		
		@Override
		protected int getMinArgs() {
			return 2;
		}
	
		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
	        checkArgs(length, context);
	        return args[0].sameValueAs(args[1]);
		}
	}

	public static class NotSameTest extends ValidationBuiltin {
	
		public String getName() {
			return "notSame";
		}
		
		@Override
		protected int getMinArgs() {
			return 2;
		}
	
		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
	        checkArgs(length, context);
	        return ! args[0].sameValueAs(args[1]);
		}
	}

	public static abstract class PathTest extends VarArgsBuiltin {
		static boolean any(Node start, Node[] path, int at, int length, Graph graph) {
			if( at == length - 2)
				return graph.contains(start, path[at], path[at+1]);
			
			ExtendedIterator it = graph.find(start, path[at], Node.ANY);
			while(it.hasNext()) {
				Triple t = (Triple) it.next();
				if( any(t.getObject(), path, at + 1, length, graph) )
					return true;
			}
			return false;
		}
		
		static boolean all(Node start, Node[] path, int at, int length, Graph graph) {
			if( at == length - 1)
				return start.sameValueAs(path[at]);
			
			ExtendedIterator it = graph.find(start, path[at], Node.ANY);
			while(it.hasNext()) {
				Triple t = (Triple) it.next();
				if(! all(t.getObject(), path, at + 1, length, graph))
					return false;
			}
			return true;
		}

		@Override
		protected int getMinArgs() {
			return 3;
		}
	}
	
	public static class AnyTest extends PathTest {
		public String getName() {
			return "any";
		}

		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
			checkArgs(length, context);
			return any(args[0], args, 1, length, context.getGraph());
		}
	}
	
	public static class AllTest extends PathTest {
		public String getName() {
			return "all";
		}

		@Override
		public boolean isMonotonic() {
			return false;
		}

		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
			checkArgs(length, context);
			return all(args[0], args, 1, length, context.getGraph());
		}
	}
	
	public static class NotAnyTest extends PathTest {
		public String getName() {
			return "notAny";
		}

		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
			checkArgs(length, context);
			return ! any(args[0], args, 1, length, context.getGraph());
		}

		@Override
		public boolean isMonotonic() {
			return false;
		}
	}
	
	public static class NotAllTest extends PathTest {
		public String getName() {
			return "notAll";
		}

		@Override
		public boolean bodyCall(Node[] args, int length, RuleContext context) {
			checkArgs(length, context);
			return ! all(args[0], args, 1, length, context.getGraph());
		}
	}
	
	public static void registerAll() {
		registerAll(BuiltinRegistry.theRegistry);
	}

	public static void registerAll(BuiltinRegistry r) {
		r.register(new Problem());
		r.register(new DatatypeTest());
		r.register(new DatatypeTest());
		r.register(new NotDatatypeTest());
		r.register(new SameTest());
		r.register(new NotSameTest());
		r.register(new FactTest());
		r.register(new NotFactTest());
		r.register(new AnyTest());
		r.register(new NotAnyTest());
		r.register(new AllTest());
	}

}
