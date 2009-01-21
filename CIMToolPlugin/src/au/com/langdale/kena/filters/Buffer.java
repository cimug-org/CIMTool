package au.com.langdale.kena.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;

import au.com.langdale.kena.NodeIterator;

public class Buffer implements NodeIterator {
	private Iterator inner;
	
	public Buffer(NodeIterator it) {
		Collection seen = new ArrayList();
		while(it.hasNext())
			seen.add(it.nextNode());
		inner = seen.iterator();
	}
	
	public Node nextNode() {
		return (Node) inner.next();
	}

	public boolean hasNext() {
		return inner.hasNext();
	}

	public Object next() {
		return inner.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
