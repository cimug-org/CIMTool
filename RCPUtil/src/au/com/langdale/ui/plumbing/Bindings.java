/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Delegates Binding events to Binding implementations.  
 * 
 * Bindings are invoked in the reverse of the order they 
 * are added by the push() method.
 * 
 */
public class Bindings implements Binding {

	public static class GuardedList  {
	    private int guard = 0;
	    private ArrayList contents = new ArrayList();
	    
	    public List get() {
	    	return contents;
	    }

	    public GuardedList use() {
	    	guard++;
	    	return this;
	    }
	    
	    public void release() {
	    	guard--;
	    }
	    
	    public GuardedList copy() {
	    	if(guard > 0) {
	    		GuardedList clone = new GuardedList();
	    		clone.contents.addAll(contents);
	    		return clone;
	    	}
	    	else
	    		return this;
	    }
	}
	
	private GuardedList delegates = new GuardedList();
	
	public void refresh() {
		GuardedList stable = delegates.use();
		try {
			Iterator it = stable.get().iterator();
			while(it.hasNext()) {
				Binding binding = ((Binding)it.next());
				binding.refresh();
			}
		}
		finally {
			stable.release();
		}
	}

	public void update() {
		GuardedList stable = delegates.use();
		try {
			Iterator it = stable.get().iterator();
			while(it.hasNext()) {
				Binding binding = ((Binding)it.next());
				binding.update();
			}
		}
		finally {
			stable.release();
		}
	}

	public String validate() {
		GuardedList stable = delegates.use();
		try {
			Iterator it = stable.get().iterator();
			while(it.hasNext()) {
				String result = ((Binding)it.next()).validate();
				if( result != null )
					return result;
			}
			return null;
		}
		finally {
			stable.release();
		}
	}

	public void reset() {
		GuardedList stable = delegates.use();
		try {
			Iterator it = stable.get().iterator();
			while(it.hasNext()) {
				((Binding)it.next()).reset();
			}
		}
		finally {
			stable.release();
		}
	}

	/**
	 * Add a delegate Binding to the top of the binding stack.
	 * It will receive events before all previously added delegates.
	 */
	public void push(Binding bind) {
		delegates = delegates.copy();
		delegates.get().add(0, bind);
	}
	

	/**
	 * Add a delegate Binding at a specific position in the stack.
	 * It will receive events immediately after the given reference. 
	 */
	public void push(Binding bind, Object after) {
		delegates = delegates.copy();
		int ix = after != null? delegates.get().indexOf(after) + 1: 0;
		delegates.get().add(ix, bind);
	}
	

	/**
	 * Remove a delegate Binding.
	 */
	public void remove(Binding bind) {
		delegates = delegates.copy();
		delegates.get().remove(bind);
	}
}
