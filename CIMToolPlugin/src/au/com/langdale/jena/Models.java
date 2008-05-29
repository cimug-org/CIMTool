package au.com.langdale.jena;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.InfGraph;

/**
 * A collection of utilities for composing and decomposing models.
 *
 */
public class Models {
	
	/**
	 * Create the union of two models.  Updates will be directed to the first.
	 * This differs from the Jena methods in that the graph hierarchy of the new
	 * model will not grow in depth on repeated application. The underlying graphs 
	 * of the arguments are extracted and inferences are dropped.
	 *   
	 */
	public static OntModel merge(Model updatableModel, Model backgroundModel) {
		MultiUnion union = new MultiUnion();
		add(union, updatableModel.getGraph());
		add(union, backgroundModel.getGraph());
		
		Model basic = ModelFactory.createModelForGraph(union);
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF, basic);
	}
	
	/**
	 * Merge an empty updatable model with a background model.
	 */
	public static OntModel overlay(Model backgroundModel) {
		return merge(ModelFactory.createDefaultModel(), backgroundModel);
	}
	
	/**
	 * Copy a model.
	 */
	public static OntModel copy(Model model) {
		OntModel clone = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		clone.add(model, true);
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
	 * something of a hack to deal with problems in the Jena API
	 * OntModel.getBaseModel() does not return an OntModel, OK,
	 * but it does not even return the model that would be hit
	 * by a mutating operation such as createClass(), 
	 * contrary to documentation.
	 * 
	 * @param model
	 * @return
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
		
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, 
				ModelFactory.createModelForGraph(graph));
	}

}
