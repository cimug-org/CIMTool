/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

/**
 * Defines overall form status events.
 */
public interface Observer {
	/**
	 * Indicates to the receiver that values have been transferred from the
	 * widgets to the model or other underlying data structures.
	 */
	public void markDirty();
	
	/**
	 * Indicates to the receiver that the widget values and/or model data
	 * are now valid.
	 */
	public void markValid();

	/**
	 * Indicates to the receiver that the widget values and/or model data
	 * are now invalid.  The message parameter provides a human readable
	 * reason.
	 */
	public void markInvalid(String message);
}
