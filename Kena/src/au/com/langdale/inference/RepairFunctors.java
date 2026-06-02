package au.com.langdale.inference;

import static au.com.langdale.inference.StandardFunctorActions.check;

import java.util.HashMap;
import java.util.Map;

import au.com.langdale.inference.Extractor.FunctorActions;
import au.com.langdale.inference.Extractor.RuleState;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

public class RepairFunctors {
	public static final String NS = "http://langdale.com.au/2009/Repair#";
	public static final String ARGS = "incorrect number of arguments";
	public static final String SUBJECT = "no subject supplied";
	public static final String BODY = "not allowed in rule body";

	public static abstract class RepairFunctor implements FunctorActions {

		public String getLabel() {
			String name = getClass().getSimpleName();
			int ix = name.lastIndexOf("$");
			if( ix >= 0 && ix < name.length() - 1)
				name = name.substring(ix + 1);
			return name.substring(0, 1).toLowerCase() + name.substring(1);
		}
		
		public void apply(Node[] nodes, Graph graph, Graph axioms,	RuleState state) {
			check(nodes.length > 0, ARGS);
			
			Node parent = nodes[0];
			Node[] args = new Node[nodes.length - 1];
			for(int ix = 1; ix < nodes.length; ix ++ )
				args[ix-1] = nodes[ix];
			
			checkArgs(args);
			
			OntModel model = ModelFactory.createMem(graph);
			OntResource repair = model.createIndividual(LOG.Repair);
			OntResource problem = model.createResource(parent);
			problem.addProperty(LOG.hasRepairs, repair);
			repair.addRDFType(model.createResource(NS + getLabel()));
			repair.addComment(getComment(), null);
			repair.addProperty(LOG.repairArgs, model.createList(args));
		}

		public void match(Node[] nodes, AsyncModel model, Graph axioms,	RuleState state) {
			check(false, BODY);
		}
		
		protected abstract void checkArgs(Node[] args);
		
		protected abstract void repair(Node[] args, Graph graph, Map renames);
		
		public abstract String getDescription(Node[] args);
		
		public abstract String getComment();

		public abstract int getPriority();
	}
	
	public static class RepairAction {
		private RepairFunctor func;
		private Node problem;
		private Node ref;
		private Node[] args;
		
		public RepairAction(RepairFunctor func, Node problem, Node ref, Node[] args) {
			this.func = func;
			this.problem = problem;
			this.ref = ref;
			this.args = args;
			func.checkArgs(args);
		}
		
		public Node getProblem() {
			return problem;
		}

		public Node getRef() { 
			return ref; 
		}
		
		public String getDescription() {
			return func.getDescription(args);
		}
		
		public void repair(Graph graph, Map renames) {
			func.repair(args, graph, renames);
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof RepairAction) && ((RepairAction)obj).getRef().equals(ref);
		}
		
		@Override
		public int hashCode() {
			return ref.hashCode();
		}

		public int getPriority() {
			return func.getPriority();
		}
		
		public String getComment() {
			return func.getComment();
		}
	}
	
//	private static Node getSubject(Node repair, Graph graph) {
//		OntModel model = ModelFactory.createMem(graph);
//		OntResource ref = model.createResource(repair);
//		return getSubject(ref);
//	}
//
//	private static Node getSubject(OntResource ref) {
//		OntResource problem = ref.getSubject(LOG.hasRepairs);
//		check( problem != null, SUBJECT);
//		
//		OntResource subject = problem.getSubject(LOG.hasProblems);
//		check( subject != null, SUBJECT);
//		return subject.asNode();
//	}
//	
//	private static Node[] getArgs(Node repair, Graph graph) {
//		OntModel model = ModelFactory.createMem(graph);
//		OntResource ref = model.createResource(repair);
//		return getArgs(ref);
//	}
//	
//	private static RepairFunctor getFunctor(Node repair, Graph graph) {
//		OntModel model = ModelFactory.createMem(graph);
//		OntResource ref = model.createResource(repair);
//		return getFunctor(ref);
//	}
//
//	private static RepairAction getAction(Node repair, Graph graph) {
//		OntModel model = ModelFactory.createMem(graph);
//		OntResource ref = model.createResource(repair);
//		return getAction(ref);
//	}

	private static Node[] getArgs(OntResource ref) {
		OntResource args = ref.getResource(LOG.repairArgs);
		if( args != null )
			return args.toElementArray();
		else
			return new Node[0];
	}

	private static RepairFunctor getFunctor(OntResource ref) {
		ResIterator it = ref.listRDFTypes(false);
		while( it.hasNext()) {
			OntResource kind = it.nextResource();
			if( kind.getNameSpace().equals(NS)) {
				RepairFunctor func = (RepairFunctor) map.get(kind.getLocalName());
				if( func != null)
					return func;
			}
		}
		return null;
	}
	
	public static RepairAction getAction(OntResource ref) {
		RepairFunctor func = getFunctor(ref);
		OntResource problem = ref.getSubject(LOG.hasRepairs);
		return new RepairAction(func, problem.asNode(), ref.asNode(), getArgs(ref));
	}

	private static Map map = new HashMap();
	
	public static void add(RepairFunctor func) {
		map.put(func.getLabel(), func);
	}
	
	public static Map getFunctorMap() {
//		System.out.println("----");
//		for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
//			System.out.println( "Profile Functor " + it.next());
//		}
//		System.out.println("----");

		return map;
	}
	
	static {
		RepairLibrary.addAll();
	}
}
