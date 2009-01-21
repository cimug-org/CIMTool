package au.com.langdale.kena;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;

public class ResourceFactory {
	public static Resource createResource() {
		return new Resource(Node.createAnon());
	}

	public static Resource createResource(String uri) {
		return new Resource(Node.createURI(uri));
	}

	public static Resource createResource(FrontsNode symbol) {
		return new Resource(symbol.asNode());
	}
	
	public static Property createProperty(String uri) {
		return new Property(Node.createURI(uri));
	}
	
	public static Property createProperty(FrontsNode symbol) {
		return new Property(symbol.asNode());
	}
}
