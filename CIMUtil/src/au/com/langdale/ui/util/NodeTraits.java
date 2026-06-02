package au.com.langdale.ui.util;
/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */


/**
 * An interface that tree nodes may implement to 
 * gain more control over rendering.
 */
public interface NodeTraits {
	/**
	 * 
	 * Should the node be rendered with an error indicator?
	 */
	public boolean getErrorIndicator();
	
	/**
	 * A marker class that will determine the icon used.
	 */
	public Class getIconClass();
}