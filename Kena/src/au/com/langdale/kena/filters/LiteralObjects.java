package au.com.langdale.kena.filters;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class LiteralObjects extends FilterMap {

	public LiteralObjects(Iterator it) {
		super(it);
	}

	@Override
	protected Node map(Triple item) {
		return item.getObject();
	}

	@Override
	protected boolean test(Node item) {
		return item.isLiteral();
	}

}
