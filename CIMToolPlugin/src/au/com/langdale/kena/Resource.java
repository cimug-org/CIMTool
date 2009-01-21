package au.com.langdale.kena;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;

public class Resource implements FrontsNode {

	protected final Node node;

	Resource(Node node) {
		this.node = node;
		assert ! node.isLiteral();
	}

	@Override
	public boolean equals(Object obj) {
		if( this == obj )
			return true;
		if( obj instanceof FrontsNode ) {
			FrontsNode other = (FrontsNode)obj;
			return node.equals(other.asNode());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	@Override
	public String toString() {
		return node.toString();
	}

	public Node asNode() {
		return node;
	}

	public String getLocalName() {
		return node.getLocalName();
	}

	public String getNameSpace() {
		return node.getNameSpace();
	}
	
	public String getURI() {
		return node.getURI();
	}
	
	public boolean isAnon() {
		return node.isBlank();
	}
	
	public boolean isURIResource() {
		return node.isURI();
	}
	
	public OntResource inModel(OntModel model) {
		return new OntResource(node, model);
	}

}
