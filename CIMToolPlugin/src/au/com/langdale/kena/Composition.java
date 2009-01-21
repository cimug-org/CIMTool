/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.kena;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.reasoner.InfGraph;

/**
 * A collection of utilities for composing and decomposing models.
 *
 */
public class Composition {
	
	/**
	 * Create the union of two models and wrap a transitive inferencer around it.  
	 * 
	 * Updates will be directed to the first model.
	 * 
	 * This differs from the Jena methods in that the graph hierarchy of the new
	 * model will not grow in depth on repeated application. The underlying graphs 
	 * of the arguments are extracted and inferences are dropped.
	 *   
	 */
	public static OntModel merge(OntModel updatableModel, OntModel backgroundModel) {
		MultiUnion union = new MultiUnion();
		add(union, updatableModel.getGraph());
		add(union, backgroundModel.getGraph());
		
		return ModelFactory.createTransInf(union);
	}
	
	/**
	 * Merge an empty updatable model with a background model and add an inferencer.
	 */
	public static OntModel overlay(OntModel backgroundModel) {
		return merge(ModelFactory.createMem(), backgroundModel);
	}
	
	/**
	 * Copy a model's content as a new model.  
	 * 
	 * The new model will contain forward inferences from the given model
	 * but will not have an inferencer of its own. 
	 */
	public static OntModel copy(OntModel model) {
		OntModel clone = ModelFactory.createMem();
		clone.add(model);
		return clone;
	}
	
	/**
	 * Decompose the rhs graph and add its parts to the lhs union.
	 * Discard inferences.
	 * 
	 */
	private static void add( MultiUnion union, Graph graph) {
		if( graph instanceof MultiUnion) { 
			MultiUnion parts = (MultiUnion)graph;
			add(union, parts.getBaseGraph());
			
			Iterator it = parts.getSubGraphs().iterator();
			while( it.hasNext())
				add(union, (Graph)it.next());
		}
		else if( graph instanceof InfGraph)
			add(union, ((InfGraph)graph).getRawGraph());
		
		else if( graph != null) {
			Graph base = union.getBaseGraph();
			union.addGraph(graph);
			if( base == null)
				union.setBaseGraph(graph);
		}
	}

	/**
	 * Get the updateable sub-model of a composite model. 
	 * 
	 * Something of a hack to deal with problems in the Jena API
	 * OntModel.getBaseModel() does not return an OntModel, OK,
	 * but it does not even return the model that would be hit
	 * by a mutating operation such as createClass(), 
	 * contrary to documentation.
	 */
	public static OntModel getUpdatableModel(OntModel model) {
		Graph graph = model.getGraph();
		while( true ) {
			if( graph instanceof MultiUnion) 
				graph = ((MultiUnion)graph).getBaseGraph();
			else if( graph instanceof InfGraph)
				graph = ((InfGraph)graph).getRawGraph();
			else
				break;
		}
		
		return ModelFactory.createMem(graph);
	}

}
