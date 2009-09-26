package au.com.langdale.inference;

import com.hp.hpl.jena.graph.Triple;

/**
 * Clients implement this interface to receive query results.
 */
public interface AsyncResult {
	/**
	 * Invoked to deliver a single statement. 
	 * 
	 * If true is returned then further statements will be delivered 
	 * or close() will be invoked if none are pending.
	 *  
	 * If false is returned then no further statements will
	 * be delivered and close() will not be invoked.   
	 */
	public boolean add(Triple result);
	
	/**
	 * Invoked after all statements have been delivered. 
	 */
	public void close();
}
