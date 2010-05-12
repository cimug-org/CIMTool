/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.StructuredViewer;

import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.plumbing.Binding;
/**
 * A binding that displays an array as the checked items in a table viewer.
 */
public abstract class TableBinding implements Binding, ArrayModel, AnyModel {
	private Object[] values = new Object[0];
	private CheckboxTableViewer viewer;
	private Assembly plumbing;
	private AnyModel parent;
	
	/**
	 * Bind to the CheckboxTableViewer and a parent model.
	 * @param name: the name of the viewer
	 * @param plumbing: the event plumbing to which the viewer is connected.
	 * @param parent: a parent model that can be used to obtain the input for the viewer
	 */
	public void bind(String name, Assembly plumbing, AnyModel parent) {
		this.plumbing = plumbing;
		this.parent = parent;
		viewer = (CheckboxTableViewer) plumbing.getViewer(name);
		configureViewer(viewer);
		plumbing.addBinding(this, parent);
	}
	
	/**
	 * Bind to the CheckboxTableViewer
	 * @param name: the name of the viewer
	 * @param plumbing: the event plumbing to which the viewer is connected.
	 */
	public void bind(String name, Assembly plumbing) {
		bind(name, plumbing, null);
	}

	/**
	 * Configure the viewer, if necessary.  This may involve setting a content
	 * provider that translates the input to an array.   
	 */
	protected abstract void configureViewer(StructuredViewer viewer);
	/**
	 * @return: implementations provider the viewer input, generally an array or collection
	 */
	protected abstract Object getInput();

	/**
	 * @return: get the parent model, or null if none is bound.
	 */
	public AnyModel getParent() {
		return parent;
	}

	protected void resetInput(StructuredViewer viewer) {
		Object input = getInput();
		if( input == null || ! input.equals(viewer.getInput()))
			viewer.setInput(input);
	}


	/**
	 * @return: the values checked in the viewer.
	 */
	public Object[] getValues() {
		return values;
	}

	/**
	 * set the values to be checked, *and* refresh the whole form/assembly 
	 * FIXME: probably should not refresh here! 
	 */
	public void setValues(Object[] values) {
		this.values = values;
		if( plumbing != null)
			plumbing.doRefresh();
	}
	
	/**
	 * @return: the value checked in the viewer, if a singleton.
	 */
	public Object getValue() {
		if( values.length > 0)
			return values[0];
		else
			return null;
	}

	public void setValue(Object value) {
		setValues((value != null? new Object[] { value } : new Object[0]));
	}

	public void refresh() {
		resetInput(viewer);
		if(viewer.getInput() != null)
			viewer.setCheckedElements(values);
	}

	public void reset() {
		viewer.setAllChecked(false);
	}

	public void update() {
		resetInput(viewer);
		values = viewer.getCheckedElements();
	}

	public String validate() {
		return null;
	}
}