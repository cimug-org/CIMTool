package au.com.langdale.kena.filters;

import java.util.ArrayDeque;

import java.util.HashSet;
import java.util.Set;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

public abstract class TransitiveIterator implements ResIterator {

	private HashSet seen = new HashSet();
	private ArrayDeque queue = new ArrayDeque();
	
	public TransitiveIterator(OntResource seed) {
		queue.add(seed);
		seen.add(seed);
	}

	public boolean hasNext() {
		return ! queue.isEmpty();
	}

	public OntResource nextResource() {
		OntResource r = (OntResource) queue.removeFirst();
		ResIterator it = traverse(r);
		while( it.hasNext()) {
			OntResource s = it.nextResource();
			if( ! seen.contains(s)) {
				queue.add(s);
				seen.add(s);
			}
		}
		return r;
	}
	
	protected abstract ResIterator traverse(OntResource subject); 
	
	public Object next() {
		return nextResource();
	}

	public Set toSet() {
		while( hasNext())
			next();
		return seen;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
