/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.StructuredViewer;

import au.com.langdale.ui.plumbing.Binding;
import au.com.langdale.ui.plumbing.Plumbing;
/**
 * A binding that displays an array as the checked items in a table viewer.
 */
public abstract class TableBinding implements Binding, ArrayModel, AnyModel {
	private Object[] values = new Object[0];
	private CheckboxTableViewer viewer;
	private Plumbing plumbing;
	private AnyModel parent;
	
	public void bind(String name, Plumbing plumbing, AnyModel parent) {
		this.plumbing = plumbing;
		this.parent = parent;
		viewer = (CheckboxTableViewer) plumbing.getViewer(name);
		configureViewer(viewer);
		plumbing.addBinding(this, parent);
	}
	
	public void bind(String name, Plumbing plumbing) {
		bind(name, plumbing, null);
	}

	protected abstract void configureViewer(StructuredViewer viewer);
	protected abstract Object getInput();

	public AnyModel getParent() {
		return parent;
	}

	protected void resetInput(StructuredViewer viewer) {
		Object input = getInput();
		if( input == null || ! input.equals(viewer.getInput()))
			viewer.setInput(input);
	}

	public Object[] getValues() {
		return values;
	}

	public void setValues(Object[] values) {
		this.values = values;
		if( plumbing != null)
			plumbing.doRefresh();
	}
	
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