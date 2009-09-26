/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

/**
 *  Implements the heart of the form plumbing logic.  
 *  
 *  Controller methods trigger Binding and Observer methods
 *  in a prescribed sequence that depends on the synchronous parameter.
 *  
 *  Loops are prevented for calls representing widget events.
 */
public class Controller implements ICanRefresh {
	
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
	 * This method is normally hooked to widget events by the 
	 * various widget templates.
	 * 
	 * Unit tests might explicitly call this if widget events 
	 * are not sufficient to monitor widget state.
	 * 
	 * This triggers validate(). If synchronous mode, 
	 * update() and refresh() are also triggered.
	 */
	public void fireWidgetEvent() {
		if( preventer > 0)
			return;
		if(synchronous)
			fireUpdate();
		else
			fireValidate();
	}

	/**
	 *  The method should be called to initialise the widget values 
	 *  and again whenever the underlying data (the model) changes.  
	 *  
	 *  This triggers refresh() and validate() while suppressing events that 
	 *  would trigger update().
	 */
	public void doRefresh() {
		preventer++;
		try {
			binding.refresh();
		}
		finally {
			preventer--;
		}
		fireValidate();
	}

	/**
	 *  Initialise the widgets with default values. 
	 *  This triggers reset() and validate(). 
	 *  
	 *  If synchronous mode, update() and refresh() are also triggered.
	 */
	public void doReset() {
		preventer++;
		try {
			binding.reset();
		}
		finally {
			preventer--;
		}
		fireWidgetEvent();
	}

	/**
	 * Commit user entries from the widgets to the underlying data (the model).
	 * This triggers update(), refresh() and validate(). 
	 */
	public void fireUpdate() {
		binding.update();
		observer.markDirty();
		doRefresh();
	}

	/**
	 * Trigger validation.
	 */
	public String fireValidate() {
		String message = binding.validate();
		if( message != null)
			observer.markInvalid(message);
		else
			observer.markValid();
		return message;
	}
}
