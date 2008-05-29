/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

/**
 *  Implements the heart of the form plumbing logic.  
 *  
 *  Binding events received are passed to the delegate with additional, 
 *  consequential events.  A widget event is implemented with suppression
 *  of event loops. Finally, Observer events are generated. 
 */
public class Controller implements Binding {
	
	private Binding binding;
	private Observer observer;
	private boolean synchronous;
	private int preventer = 0;

	/**
	 * Construct with Binding and Observer delegates and the given widget
	 * synchronisation mode.
	 */
	public Controller(Binding binding, Observer observer, boolean synchronous) {
		this.binding = binding;
		this.observer = observer;
		this.synchronous = synchronous;
	}

	/**
	 * Indicates that the underlying model (data) will be kept synchronised 
	 * with the widgets by widget events.   The value is set by the ctor parameter.
	 * 
	 * If true update() is called after widget events. Otherwise update() must 
	 * be explicitly called to transfer widget values to the underlying model.
	 */
	public boolean isSynchronous() {
		return synchronous;
	}

	/**
	 * Trigger processing of a user action or entry. 
	 * 
	 * This method is normally hooked to widget events. However,
	 * it might be explicitly called in unit tests or where
	 * widget events are not sufficient to monitor widget state.
	 * 
	 */
	public void fireWidgetEvent() {
		if( preventer > 0)
			return;
		if(synchronous)
			update();
		else
			validate();
	}

	public void refresh() {
		preventer++;
		try {
			binding.refresh();
		}
		finally {
			preventer--;
		}
		validate();
	}

	public void reset() {
		preventer++;
		try {
			binding.reset();
		}
		finally {
			preventer--;
		}
		fireWidgetEvent();
	}

	public void update() {
		binding.update();
		observer.markDirty();
		refresh();
	}

	public String validate() {
		String message = binding.validate();
		if( message != null)
			observer.markInvalid(message);
		else
			observer.markValid();
		return message;
	}

}
