package au.com.langdale.kena;

import com.hp.hpl.jena.graph.Node;

public class Property extends Resource {
	Property(Node node) {
		super(node);
		assert node.isURI();
	}
}
