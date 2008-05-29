/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ViewerComparator;

import au.com.langdale.ui.plumbing.Binding;
import au.com.langdale.ui.plumbing.Plumbing;
/**
 * Bind an array of <code>BooleanModel</code>s to a CheckboxTableViewer.
 */
public abstract class BooleanBinding implements Binding {
	private CheckboxTableViewer viewer;
	private BooleanModel[] flags = BooleanModel.EMPTY_FLAGS;

	/**
	 * Called to provide the model on every refresh.
	 * @return: the model to be displayed.
	 */
	protected abstract BooleanModel[] getFlags();

	public BooleanBinding() {
		super();
	}

	public void bind(String name, Plumbing plumbing) {
		viewer = (CheckboxTableViewer) plumbing.getViewer(name);
		viewer.setComparator(new ViewerComparator());
		plumbing.addBinding(this);
	}

	public void reset() {
		for( int ix = 0; ix < flags.length; ix++) {
			viewer.setChecked(flags[ix], false);
		}
	}

	public void refresh() {
		flags = getFlags();
		viewer.setInput(flags);
		for( int ix = 0; ix < flags.length; ix++) {
			viewer.setChecked(flags[ix], flags[ix].isTrue());
		}
	}

	public void update() {
		for( int ix = 0; ix < flags.length; ix++) 
			flags[ix].setTrue(viewer.getChecked(flags[ix]));
	}

	public String validate() {
		return null;
	}

}