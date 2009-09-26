package au.com.langdale.kena.filters;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class Objects extends Nodes {
	protected Iterator inner;
	
	public Objects(Iterator inner) {
		super(inner);
	}
	
	public Node map(Triple triple) {
		return triple.getObject();
	}

}
