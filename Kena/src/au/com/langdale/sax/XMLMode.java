/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.sax;

/**
 * 	An XML Processing Mode.
 * 
 * 	A mode receives elements (XMLElement objects) one at a time for
 * 	interpretation.    
 */
public interface XMLMode {
	/**
	 * Interpret an element and return a mode object 
	 * that should interpret its child elements
	 * and text nodes.  Returning null means that children
	 * should be ignored.
	 */
	public XMLMode visit( XMLElement element);

	/**
	 * Interpret a text node in the context of its parent element.
	 */
	public void visit(XMLElement element, String text);
	
	/**
	 * Indicates the end of the mode.  No further methods will
	 * be called.
	 */
	public void leave();
}
