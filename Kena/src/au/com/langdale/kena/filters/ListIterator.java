package au.com.langdale.kena.filters;

import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.graph.Node;

public class ListIterator extends NodeLookAhead {
	private OntResource cell;
	
	public ListIterator(OntResource cell) {
		this.cell = cell;
	}
	
	protected boolean test(Node item) {
		return true;
	}
	
	@Override
	protected final boolean advance() {
		while( cell != null && ! cell.isEmpty()) {
			lookahead = cell.getFirst();
			cell = cell.getRest();
			if( lookahead != null && test(lookahead)) {
				return true;
			}
		}
		return false;
	}
}
