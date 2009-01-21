package au.com.langdale.kena;

import com.hp.hpl.jena.graph.Factory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.reasoner.transitiveReasoner.TransitiveReasoner;

public class ModelFactory {
	public static OntModel createMem(Graph graph) {
		return new OntModel(graph);
	}

	public static OntModel createMem() {
		return new OntModel(Factory.createDefaultGraph());
	}

	public static OntModel createTransInf(Graph graph) {
		TransitiveReasoner reasoner = new TransitiveReasoner();
		return new OntModel(reasoner.bind(graph));
	}
	
	public static OntModel createTransInf() {
		return createTransInf(Factory.createDefaultGraph());
	}
}
