package au.com.langdale.inference;

import java.io.IOException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeVisitor;
import com.hp.hpl.jena.graph.Node_Fluid;
import com.hp.hpl.jena.reasoner.TriplePattern;

public interface AsyncModel {

	public static final class Wildcard extends Node_Fluid {
	
		protected Wildcard() {
			super("*");
		}
	
		@Override
		public boolean equals(Object o) {
			return o instanceof Wildcard;
		}
	
		@Override
		public Object visitWith(NodeVisitor v) {
			return null;
		}
	}

	public static final Node WILDCARD = new AsyncModel.Wildcard();

	/**
	 * Access a submodel.
	 * @param quote: the node in the present model that represents with the submodel
	 * @return another <code>SplitReader</code> instance for the submodel
	 * @throws IOException
	 */
	public AsyncModel getQuote(Node quote) throws IOException;

	/**
	 * Execute all pending queries and all consequential queries.
	 * This method returns when no further queries are pending.
	 * A query is pending if some of its results have not yet been
	 * delivered.
	 * @throws IOException
	 */
	public void run() throws IOException;

	/**
	 * Issue a query.
	 * @param clause: a template that results will match
	 * @param results: the object to receive results
	 */
	public void find(TriplePattern clause, AsyncResult results);

}