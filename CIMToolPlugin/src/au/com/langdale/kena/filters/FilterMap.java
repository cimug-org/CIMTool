package au.com.langdale.kena.filters;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public abstract class FilterMap extends NodeLookAhead {
	private Iterator inner;
	public FilterMap(Iterator it) {
		this.inner = it;
	}
	
	protected abstract boolean test(Node item);
	protected abstract Node map(Triple item);

	@Override
	protected final boolean advance() {
		while( inner.hasNext()) {
			Node item = map((Triple)inner.next());
			if( test(item)) {
				lookahead = item;
				return true;
			}
		}
		return false;
	}
}
