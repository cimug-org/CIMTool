/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.splitmodel;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

/**
 * An index of all local names found in one or more models.
 */
public class SearchIndex {
	private SortedSet words = new TreeSet();
	private Set spaces = new HashSet();
	private int limit;
	
	/**
	 * Construct with result limit parameter.
	 * @param limit: the maximum number of results returned by <code>match()</code>
	 */
	public SearchIndex(int limit) {
		this.limit = limit;
	}
	
	/**
	 * Index resources 
	 * @param model: the model containing the resources 
	 */
	public void scan(OntModel model) {
		ResIterator it = model.listSubjects();
		while(it.hasNext()) {
			scan(it.nextResource());
		}
	}
	
	/**
	 * Find resources by local name.
	 * @param name: the local name
	 * @param model: the model containing the resources
	 * @return a set of <code>Resource</code>
	 */
	public Set locate(String name, OntModel model) {
		Set result = Collections.EMPTY_SET;
		for (Iterator it = spaces.iterator(); it.hasNext();) {
			String space = (String) it.next();
			OntResource res = model.createResource(space + name);
			if(res.hasRDFType()) {
				if( result.size() == 0)
					result = Collections.singleton(res);
				else { 
					if( result.size() == 1) 
						result = new HashSet(result);
					result.add(res);
				}
			}
		}
		return result;
	}

	private void scan(OntResource res) {
		if( res.isURIResource() ) {
			words.add(reverse(res.getLocalName()));
			spaces.add(res.getNameSpace());
		}
	}
	
	private static String reverse(String path) {
		StringBuffer result = new StringBuffer();
		int ix = path.length();
		while(ix > 0) {
			int iy = path.lastIndexOf('.', ix - 1) + 1;
			result.append(path, iy, ix);
			if( iy > 0 )
				result.append('.');
			ix = iy - 1;
		}
		return result.toString();
	}
	
	/**
	 * Find all names in the index matching a prefix
	 * @param prefix: a string prefixing the last dotted substring of each match 
	 * @return a collection of <code>String</code>s
	 */
	public Collection match(String prefix) {
		return new Result(prefix);
	}
	
	/**
	 * A collection of matches.
	 */
	private class Result extends AbstractCollection {
		private Collection matches;
		private String prefix;
		
		public Result(String prefix) {
			this.prefix = prefix;
			this.matches = words.tailSet(prefix);
		}
		
		@Override
		public Iterator iterator() {
			return new Iterator() {
				Iterator inner = matches.iterator();
				int count;
				Object lookahead = step();
				
				private Object step() {
					if( count >= limit || ! inner.hasNext())
						return null;
					String cand = (String) inner.next();
					if( ! cand.startsWith(prefix))
						return null;
					return reverse(cand);
				}
				
				public boolean hasNext() {
					return lookahead != null;
				}

				public Object next() {
					count++;
					Object value = lookahead;
					lookahead = step();
					return value;
				}

				public void remove() {
					
				}
			};
		}		

		@Override
		public int size() {
			Iterator inner = matches.iterator();
			int count = 0;
			while( count < limit ) {
				if( ! inner.hasNext())
					break;
				String cand = (String) inner.next();
				if( ! cand.startsWith(prefix))
					break;
				count++;
			}
			return count;
		}
	}
}
