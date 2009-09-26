package au.com.langdale.kena.filters;

import java.util.HashSet;
import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;

public abstract class Unique extends FilterMap {

	protected HashSet seen = new HashSet();

	public Unique(Iterator it) {
		super(it);
	}

	protected boolean test(Node item) {
		if( seen.contains(item))
			return false;

		seen.add(item);
		return true;
	}
}
