/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.plumbing.Binding;
/**
 * Bind a collection or array to a ComboViewer.    
 */
public abstract class ComboBinding implements Binding, AnyModel {
	private Object value;
	private ComboViewer viewer;
	private AnyModel parent;
	
	/**
	 * Bind to the ComboViewer and a parent model.
	 * @param name: the name of the viewer
	 * @param plumbing: the event plumbing to which the viewer is connected.
	 * @param parent: a parent model that can be used to obtain the input for the viewer
	 */
	public void bind(String name, Assembly plumbing, AnyModel parent) {
		this.parent = parent;
		viewer = (ComboViewer) plumbing.getViewer(name);
		configureViewer(viewer);
		plumbing.addBinding(this, parent);
	}
	/**
	 * Bind to the ComboViewer
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
	 * @return: the value selected from the combo viewer.
	 */
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void refresh() {
		resetInput(viewer);
		if(viewer.getInput() != null ) {
			if( value != null) {
				StructuredSelection proposed = new StructuredSelection(value);
				if( ! proposed.equals(viewer.getSelection()))
				  viewer.setSelection(proposed, true);
			}
			else
				viewer.setSelection(StructuredSelection.EMPTY);
		}
	}

	public void reset() {
		viewer.setSelection(StructuredSelection.EMPTY);
	}
	
	public void update() {
		resetInput(viewer);
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if(!selection.isEmpty())
			value = selection.getFirstElement();
		else
			value = null;
	}

	public String validate() {
		return null;
	}
}