package au.com.langdale.kena.filters;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class Subjects extends Nodes {
	public Subjects(Iterator inner) {
		super(inner);
	}

	protected Node map(Triple triple) {
		return triple.getSubject();
	}

}
