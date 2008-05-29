/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Delegates Binding events to Binding implementations.  
 * 
 * Bindings are invoked in the reverse of the order they 
 * are added by the push() method.
 * 
 */
public class Bindings implements Binding {

	private List properties = new LinkedList();

	public void refresh() {
		// tell each property to copy from store to widget
		Iterator it = properties.iterator();
		while(it.hasNext()) {
			((Binding)it.next()).refresh();
		}
	}

	public void update() {
		Iterator it = properties.iterator();
		while(it.hasNext()) {
			((Binding)it.next()).update();
		}
	}

	public String validate() {
		Iterator it = properties.iterator();
		while(it.hasNext()) {
			String result = ((Binding)it.next()).validate();
			if( result != null )
				return result;
		}
		return null;
	}

	public void reset() {
		Iterator it = properties.iterator();
		while(it.hasNext()) {
			((Binding)it.next()).reset();
		}
	}

	public void push(Binding bind) {
		properties.add(0, bind);
	}
	

	public void push(Binding bind, Object after) {
		int ix = after != null? properties.indexOf(after) + 1: 0;
		properties.add(ix, bind);
	}
}