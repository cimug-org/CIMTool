package au.com.langdale.kena.filters;

import com.hp.hpl.jena.graph.Node;

import au.com.langdale.kena.NodeIterator;

public abstract class NodeLookAhead implements NodeIterator {
	protected Node lookahead;
	private boolean filled;

	protected abstract boolean advance();

	public boolean hasNext() {
		if( ! filled ) 
			filled = advance();
		return filled;
	}

	public Node nextNode() {
		if( ! filled ) 
			advance();
		filled = false;
		return lookahead;
	}

	public Object next() {
		return nextNode();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
