/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
/**
 * A Template contains a specification for a single control and its children.  
 * The control is created within its parent by the realise() method.
 */
public interface Template {

	/**
	 *  Build and return a control with the given parent. The control
	 *  may itself be the root of a hierarchy. 
	 *  
	 *  This method should hook widget events and register 
	 *  one or more widget or viewers against their names. 
	 */
	public Control realise(Composite parent);

}