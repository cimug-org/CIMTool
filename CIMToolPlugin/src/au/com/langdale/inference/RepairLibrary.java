package au.com.langdale.inference;

import static au.com.langdale.inference.StandardFunctorActions.check;

import java.util.Map;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.NodeIterator;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResourceFactory;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

public class RepairLibrary extends RepairFunctors {
	
	public static class Remove extends RepairFunctor {

		@Override
		protected void checkArgs(Node[] args) {
			check(args.length >= 1 || args.length <= 3, ARGS);
		}

		@Override
		protected void repair(Node[] args, Graph graph, Map renames) {
			OntResource subj = ModelFactory.createMem(graph).createResource(args[0]);
			if( args.length == 1)
				subj.removeRecursive();
			else {
				Property prop = ResourceFactory.createProperty(args[1]);
				if(args.length == 2) 
					remove(subj, prop);
				else
					remove(subj, prop, args[2]);
			}
		}

		@Override
		public String getComment() {
			return "remove this item";
		}

		@Override
		public String getDescription(Node[] args) {
			if( args.length == 1)
				return  "remove all references to " + args[0];
			else if( args.length == 2) 
				return "remove property " + args[1] + " from " + args[0];
			else
				return "remove property ("  + args[0] + " " + args[1] + " " + args[2] + ")";
				                                                      
		}
		
		@Override
		public int getPriority() {
			return 9;
		}
	}
	
	public static class SetProperty extends RepairFunctor {

		@Override
		protected void checkArgs(Node[] args) {
			check(args.length == 3, ARGS);
		}

		@Override
		protected void repair(Node[] args, Graph graph, Map renames) {
			OntResource subj = ModelFactory.createMem(graph).createResource(args[0]);
			Property prop = ResourceFactory.createProperty(args[1]);
			Node value = args[2];
			remove(subj, prop, value);
			subj.addProperty(prop, value);
		}

		@Override
		public String getComment() {
			return "set property";
		}

		@Override
		public String getDescription(Node[] args) {
			return "set property ("  + args[0] + " " + args[1] + " " + args[2] + ")";
		}
		
		@Override
		public int getPriority() {
			return 3;
		}
	}
	
	public static class Rename extends RepairFunctor {

		@Override
		protected void checkArgs(Node[] args) {
			check(args.length == 3, ARGS);
		}

		@Override
		protected void repair(Node[] args, Graph graph, Map renames) {
			renames.put(args[0], args[1]);
		}

		@Override
		public String getComment() {
			return "rename this item";
		}

		@Override
		public String getDescription(Node[] args) {
			return  "rename all references to " + args[0] + " as " + args[1];
		}
		
		@Override
		public int getPriority() {
			return 1;
		}
	}

	protected static void remove(OntResource subj, Property prop, Node value) {
		subj.removeProperty(prop, value);
		if( value.isBlank()) {
			subj.getOntModel().createResource(value).removeRecursive();
		}
	}

	protected static void remove(OntResource subj, Property prop) {
		for(  NodeIterator it = subj.listObjects(prop); it.hasNext();)
			remove(subj, prop, it.nextNode());
	}

	
	static void addAll() {
		add(new Remove());
		add(new Rename());
		add(new SetProperty());
	}
}
