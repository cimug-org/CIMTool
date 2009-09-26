package au.com.langdale.kena.filters;

import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.graph.Node;

public class ListResourceIterator extends ListIterator {

	public ListResourceIterator(OntResource cell) {
		super(cell);
	}

	@Override
	protected boolean test(Node item) {
		return ! item.isLiteral();
	}

}
