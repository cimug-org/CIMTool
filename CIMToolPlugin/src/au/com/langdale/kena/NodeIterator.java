package au.com.langdale.kena;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;

public interface NodeIterator extends Iterator {
	public Node nextNode();
}
