package au.com.langdale.inference;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayDeque;

import com.hp.hpl.jena.graph.Factory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;

public class GraphAdapter implements AsyncModel {
	private Graph graph;
	private Deque work;

	public GraphAdapter(Graph graph) {
		this.graph = graph;
		work = new ArrayDeque();
	}

	public void find(TriplePattern clause, AsyncResult results) {
		work.add(new Query(clause, results));
	}

	public AsyncModel getQuote(Node quote) throws IOException {
		return new GraphAdapter(Factory.createDefaultGraph());
	}

	public void run() throws IOException {
		while( ! work.isEmpty()) {
			Query query = (Query) work.remove();
			query.run();
		}
	}
	
	private class Query {
		TriplePattern clause;
		AsyncResult results;
		
		public Query(TriplePattern clause, AsyncResult results) {
			this.clause = clause;
			this.results = results;
		}

		void run() {
			boolean more = true;
			Iterator it = graph.find(clause.asTripleMatch());
			while( more && it.hasNext()) {
				Triple t = (Triple) it.next();
				more = results.add(t);
			}
			if( more )
				results.close();
		}
	}
}
