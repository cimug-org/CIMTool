package au.com.langdale.kena.filters;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class SubjectsExcluding extends FilterMap {

	private Node excluded;
	
	public SubjectsExcluding(Node excluded, Iterator it) {
		super(it);
		this.excluded = excluded;
	}

	@Override
	protected Node map(Triple item) {
		return item.getSubject();
	}

	@Override
	protected boolean test(Node item) {
		return  ! item.equals(excluded);
	}

}
