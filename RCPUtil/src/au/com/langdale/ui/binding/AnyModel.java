/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;
/**
 * A data model whose single value is an untyped object.  
 * 
 * This boils down to a reference to an Object.
 */
public interface AnyModel {
	/**
	 * @return: the value of the model.
	 */
	public Object getValue();
	public void setValue(Object value);
}