package au.com.langdale.kena.filters;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import au.com.langdale.kena.NodeIterator;

public abstract class Nodes implements NodeIterator {
	private Iterator inner;

	public Nodes(Iterator inner) {
		this.inner = inner;
	}
	
	protected abstract Node map(Triple item);
	
	public boolean hasNext() {
		return inner.hasNext();
	}

	public Object next() {
		return nextNode();
	}
	
	public Node nextNode() {
		return map((Triple) inner.next());
	}

	public void remove() {
		inner.remove();
	}

}
