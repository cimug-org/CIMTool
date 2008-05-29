/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

/**
 * Defines events to transfer data to and from a user interface
 * and perform interactive validation.
 */
public interface Binding {
	/**
	 * The implementation should transfer default values to the widgets.  
	 */
	public void reset();
	/**
	 *  The implementation should transfer values from an underlying model to the widgets. 
	 */
	public void refresh();

	/**
	 *  The implementation should transfer values from the widgets an underlying model.
	 */
	public void update();

	/**
	 *  The implementation should examine the contents of the
	 *  model and/or the widgets' state and return an error message if user action
	 *  or data entry is required. It is invoked following both refresh() and update().
	 */
	public abstract String validate();
}
