/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;
/**
 * A binding that has no behaviour.
 *  
 * This can be used as a placemarker in a bindings list or
 * as a base class for implementations. 
 */
public class SentinelBinding implements Binding {

	/**
	 * Default implementation provided.
	 */
	public void reset() {}


	/**
	 * Default implementation provided.
	 */
	public void refresh() {}


	/**
	 * Default implementation provided.
	 */
	public void update() {}


	/**
	 * Default implementation provided.
	 */
	public String validate() {
		return null;
	}
}