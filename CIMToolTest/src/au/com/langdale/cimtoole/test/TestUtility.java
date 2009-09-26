package au.com.langdale.cimtoole.test;

import au.com.langdale.inference.AsyncModel;
import au.com.langdale.inference.AsyncResult;
import au.com.langdale.kena.OntModel;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import junit.framework.TestCase;

public class TestUtility extends TestCase {

	protected static TriplePattern pattern(String s, String p, String o) {
		return new TriplePattern(uri(s), uri(p), literal(o));
	}

	protected static TriplePattern pattern(String s, String p, Node o) {
		return new TriplePattern(uri(s), uri(p), o);
	}

	protected static Node uri(String s) {
		if( s == null )
			return Node.ANY;
		else
			return Node.createURI(s);
	}

	protected static Node literal(String s) {
		if( s == null )
			return Node.ANY;
		else
			return Node.createLiteral(s);
	}
	
	protected static final String ANY = null;
	protected static final String A = RDF.type.getURI();
	
	protected static Triple triple(String s, String p, String o) {
		return new Triple(Node.createURI(s), Node.createURI(p), Node.createLiteral(o));
	}

	protected static Triple triple(String s, String p, Node o) {
		return new Triple(Node.createURI(s), Node.createURI(p), o);
	}

	public static class Deferred implements AsyncResult {
		private boolean complete;
		private int count = 0;
		private Triple result;
		
		public boolean add(Triple result) {
			this.result = result;
			count ++;
			return true;
		}

		public void close() {
			complete = true;
		}

		public boolean isComplete() {
			return complete;
		}

		public int getCount() {
			return count;
		}

		public Triple getResult() {
			return result;
		}
	}

	public static abstract class Chain implements AsyncResult {
		private TriplePattern pattern;
		private AsyncResult delegate;
		private int pending = 1;
		private boolean more = true;
		
		public Chain(TriplePattern pattern, AsyncResult delegate) {
			this.pattern = pattern;
			this.delegate = delegate;
		}
		
		private class Link implements AsyncResult {
			public boolean add(Triple result) {
				if(more) 
					more = delegate.add(result);
				if( ! more )
					Chain.this.close();
				return more;
			}

			public void close() {
				Chain.this.close();
			}
		}
		
		public boolean add(Triple result) {
			pending ++;
			find(new TriplePattern(result.getObject(), pattern.getPredicate(), pattern.getObject()), new Link());
			return more;
		}
		
		protected abstract void find(TriplePattern pattern, AsyncResult result);

		public void close() {
			pending --;
			if( pending == 0)
				delegate.close();
		}
	}
	
	public static class ChainGraph extends Chain {
		private Graph graph;

		public ChainGraph(Graph graph, TriplePattern pattern, AsyncResult delegate) {
			super(pattern, delegate);
			this.graph = graph;
		}

		@Override
		protected void find(TriplePattern pattern, AsyncResult result) {
			TestUtility.find(graph, pattern, result);
		}
	}
	
	public static class ChainReader extends Chain {
		private AsyncModel reader;

		public ChainReader(AsyncModel reader, TriplePattern pattern, AsyncResult delegate) {
			super(pattern, delegate);
			this.reader = reader;
		}

		@Override
		protected void find(TriplePattern pattern, AsyncResult result) {
			reader.find(pattern, result);
		}
	}

	protected static Deferred find(AsyncModel reader, TriplePattern pattern) {
		Deferred deferred = new Deferred();
		reader.find(pattern, deferred);
		return deferred;
	}
	
	protected static Deferred find(OntModel model, TriplePattern pattern) {
		return find(model.getGraph(), pattern);
	}
	
	protected static Deferred find(OntModel model, TriplePattern pattern1, TriplePattern pattern2) {
		return find(model.getGraph(), pattern1, pattern2);
	}

	protected static Deferred find(final Graph graph, TriplePattern pattern1, TriplePattern pattern2) {
		Deferred deferred = new Deferred();
		find(graph, pattern1, new ChainGraph( graph, pattern2, deferred));
		return deferred;
	}
	
	protected static void find(OntModel model, TriplePattern pattern, AsyncResult deferred) {
		find(model.getGraph(), pattern, deferred);
	}

	protected static Deferred find(Graph graph, TriplePattern pattern) {
		Deferred deferred = new Deferred();
		find(graph, pattern, deferred);
		return deferred;
	}

	protected static void find(Graph graph, TriplePattern pattern, AsyncResult deferred) {
		ExtendedIterator it = graph.find(pattern.asTripleMatch());
		while (it.hasNext()) {
			Triple triple = (Triple) it.next();
			if( ! deferred.add(triple))
				return;
		}
		deferred.close();
	}
}
