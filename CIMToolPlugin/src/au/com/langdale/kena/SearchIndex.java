/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.kena;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * An index of all local names found in one or more models.
 */
public abstract class SearchIndex {
	private SortedSet words = new TreeSet();

	/**
	 * Add a single word to this index.
	 */
	public void addWord(String word) {
		words.add(reverse(word));
	}
	
	/**
	 * Find all words in the index matching a prefix
	 * @param prefix: a string prefixing the last dotted substring of each match 
	 * @return a collection of <code>String</code>s
	 */
	public Collection match(String prefix, int limit) {
		return new Result(prefix, limit);
	}
	
	/**
	 * Add all words occurring in the model to this index.
	 */
	public abstract void scan(OntModel model);
	
	
	/**
	 * Find the resources in the model corresponding to this word.
	 */
	public abstract Set locate(String name, OntModel model);
	
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
	 * A collection of matches.
	 */
	private class Result extends AbstractCollection {
		private Collection matches;
		private String prefix;
		private int limit;
		
		public Result(String prefix, int limit) {
			this.prefix = prefix;
			this.matches = words.tailSet(prefix);
			this.limit = limit;
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
