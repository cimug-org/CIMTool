package au.com.langdale.kena.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

public class Wrapper implements ResIterator {
	protected Iterator inner;
	protected OntModel model;
	
	public Wrapper(OntModel model, Iterator it) {
		this.inner = it;
		this.model = model;
	}

	public boolean hasNext() {
		return inner.hasNext();
	}

	public Object next() {
		return nextResource();
	}
	
	public OntResource nextResource() {
		return model.createResource(((Node)inner.next()));
	}
	
	public Set toSet() {
		Set result = new HashSet();
		while( hasNext())
			result.add(nextResource());
		return result;
	}

	public void remove() {
		inner.remove();
	}
}
