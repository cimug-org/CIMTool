/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;
/**
 * A data model whose value is an array.
 */
public interface ArrayModel {
	/**
	 * @return: the value of the model
	 */
	public Object[] getValues();
	public void setValues(Object[] values);
}