/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;
/**
 * Tag for components that can refresh.
 */
public interface ICanRefresh {
	/**
	 * Trigger a refresh operation, transferring model values to a user interface.
	 */
	void doRefresh();
}
