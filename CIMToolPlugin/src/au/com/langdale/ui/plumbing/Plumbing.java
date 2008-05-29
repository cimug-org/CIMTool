/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.plumbing;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 *  A base class to supply form construction, refresh and update logic.  
 *  
 *  Subclasses implement define() to return a Template instance.
 *  These are canned layout and widget specifications. 
 *  (See ContentBuilder for an inventory of templates.)
 *  
 *  Clients call realise() to build a widget hierarchy conforming to 
 *  the Template returned by define(). realise() should only be called once.
 *  It can be extended to further initialise the widgets.  
 *  
 *  During realise(), most templates will register a widget or viewer instance 
 *  with putControl() or putViewer(). This enables the control to be accessed in 
 *  form update/refresh logic and unit tests via getControl() and getViewer().
 *  
 *  Most templates will also hook their widgets to the fireWidgetEvent()
 *  method via widget-specific event listeners.  This is the main entry point
 *  into the form event plumbing described below.
 *  
 *  Event plumbing is mediated by two interfaces.  The Binding interface is
 *  implemented by classes that transfer data to and from widgets and 
 *  interactively validate that data.   See Binding for details.
 *  
 *  Binding methods for the form as a whole are implemented by stubs in
 *  this class and may be overridden by subclasses.  
 *  
 *  Binding methods for a specific widget or group of widgets may be implemented 
 *  by their template, which may be a subclass of BoundTemplate. 
 *  
 *  The second plumbing interface is Observer, which is implemented by classes
 *  that track form validation status and clean/dirty status. Observer methods
 *  stubs are defined by this class and may be overridden by subclasses.
 *  See interface Observer for details.
 *  
 *  When Binding and Observer methods are implemented as above they are 
 *  automatically hooked to the form event plumbing.  (There is no need
 *  to explicitly register the receiving objects with the event sources.)  
 *  
 *  The form plumbing is responsible for delegating events to Binding 
 *  and Observer methods. The template Binding methods are called first, 
 *  then the whole-form Binding methods, and the Observer methods last. 
 *  The plumbing also triggers any necessary consequential events and 
 *  blocks widget event loops. 
 *  
 *  Events are injected into the form plumbing by fireWidgetEvent() and its
 *  counterpart doRefresh(). doRefresh() should be called following realise() 
 *  to initialise the widget values and again whenever the underlying data (the model)
 *  changes.  
 *  
 *  The event flow is influenced by the constructor's synchronous parameter.  
 *  In synchronous mode, the underlying data (the model) is kept synchronised 
 *  with the widgets by widget events. 
 *  
 *  An additional method, fireUpdate() is used if synchronous mode 
 *  is not in effect. fireUpdate() explicitly commits user entries from 
 *  the widgets to the underlying data (the model). 
 *  
 */
public abstract class Plumbing implements Template, Binding, Observer, ICanRefresh {

	public abstract class BoundTemplate implements Template, Binding {
		public BoundTemplate() {
			addBinding(this);
		}

		protected Plumbing getPlumbing() {
			return Plumbing.this;
		}
	}
	
	private Controller controller;
	private Bindings bindings;
	private Map subjects;

	/**
	 * Construct a new set of form plumbing.
	 * 
	 * The synchronous parameter indicates that the underlying model (data) 
	 * will be kept synchronised with the widgets by widget events. 
	 * If false, then fireUpdate() must be explicitly called 
	 * to transfer widget values to the underlying model.
	 * 
	 */
	public Plumbing(boolean synchronous) {
		bindings = new Bindings();
		bindings.push(this);
		controller = new Controller(bindings, this, synchronous);
		subjects = new HashMap();
	}
	/**
	 *  Returns an implementation of Binding through which events may be 
	 *  injected into the form plumbing.   
	 */
	public Binding getController() {
		return controller;
	}
	
	/**
	 * Default implementation of Binding.
	 */
	public void reset() {}


	/**
	 * Default implementation of Binding.
	 */
	public void refresh() {}


	/**
	 * Default implementation of Binding.
	 */
	public void update() {}


	/**
	 * Default implementation of Binding.
	 */
	public String validate() {
		return null;
	}
	
	/**
	 * Default implementation of Observer.
	 */
	public void markDirty() {}

	/**
	 * Default implementation of Observer.
	 */
	public void markInvalid(String message) {}

	/**
	 * Default implementation of Observer.
	 */
	public void markValid() {}

	/**
	 *  The method should be called following realise() to initialise 
	 *  the widget values and again whenever the underlying data (the model)
	 *  changes.  
	 *  
	 *  This triggers refresh() and validate() while suppressing events that 
	 *  would trigger update().
	 */
	public final void doRefresh() {
		getController().refresh();
	}

	/**
	 *  Initialise the widgets with default values. 
	 *  This triggers reset() and validate(). 
	 *  
	 *  If synchronous mode, update() and refresh() are also triggered.
	 */
	public final void doReset() {
		getController().reset();
	}

	/**
	 * Commit user entries from the widgets to the underlying data (the model).
	 * This triggers update(), refresh() and validate(). 
	 */
	public final void fireUpdate() {
		getController().update();
	}

	/**
	 * Trigger processing of a user action or entry. 
	 * 
	 * This method is normally hooked to widget events by the 
	 * various widget templates.
	 * 
	 * Subclasses and unit tests might explicitly call this 
	 * if widget events are not sufficient to monitor widget state.
	 * 
	 * This triggers validate(). If synchronous mode, 
	 * update() and refresh() are also triggered.
	 */
	public final void fireWidgetEvent() {
		controller.fireWidgetEvent();
	}

	/**
	 * Trigger validation.
	 */
	public final void fireValidate() {
		getController().validate();
	}
	
	/**
	 *  Build a widget hierarchy conforming to 
	 *  the Template returned by define(). 
	 *  
	 *  This method should only be called once as the
	 *  Template will hook widget events and register 
	 *  them against their names. 
	 */
	public Control realise(Composite parent) {
		return define().realise(parent);
	}

	/**
	 *  The implementation should construct and return a Template instance.
	 *  These are canned layout and widget specifications. 
	 *  They are created with the template 
	 *  methods such as Row(), Column() and Label().
	 */
	protected abstract Template define();

	/**
	 * Get a generic control.
	 */
	public Control getControl(String name) {
		return (Control) subjects.get(name);
	}

	/**
	 * Get a generic Viewer.
	 */
	public Viewer getViewer(String name) {
		return (Viewer) subjects.get(name);
	}

	public void addBinding(Binding bind) {
		bindings.push(bind);
	}

	public void addBinding(Binding bind, Object after) {
		bindings.push(bind, after);
	}

	/**
	 * Register a control under its name.
	 */
	public void putControl(String name, Control control) {
		subjects.put(name, control);
	}

	/**
	 * Register a viewer under its name.
	 */
	public void putViewer(String name, Viewer viewer) {
		subjects.put(name, viewer);
	}
}