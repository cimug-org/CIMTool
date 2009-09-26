/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;

import au.com.langdale.ui.plumbing.ICanRefresh;
/**
 * An eclipse multi page editor that is provided with event plumbing.
 */

public abstract class FurnishedMultiEditor extends MultiPageEditorPart  implements ICanRefresh  {

	/**
	 * The shared form toolkit instance.
	 */
	private FormToolkit toolkit;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setPartName(input.getName());
		toolkit = Assembly.createFormToolkit();
	}
	
	public void close() {
		getSite().getPage().closeEditor(this, false);
	}

	public int addPage(IEditorPart editor) {
		int ix;
		try {
			ix = super.addPage(editor, getEditorInput());
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
		setPageText(ix, editor.getTitle());
		return ix;
	}
	
	private boolean dirty;

	@Override
	public boolean isDirty() {
		return dirty || super.isDirty();
	}
	
	protected void markDirty() {
		if( ! dirty ) {
			dirty = true;
			firePropertyChange(PROP_DIRTY);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		for(int ix = 0; ix < getPageCount(); ix++) {
			IEditorPart page = getEditor(ix);
			if( page != null)
				page.doSave(monitor);
		}
		if( dirty) {
			dirty = false;
			firePropertyChange(PROP_DIRTY);
		}
	}
	
	public void doRefresh() {
		int ix = getActivePage();
		if( ix >= 0 ) {
			doRefresh(ix);
		}
	}

	private void doRefresh(int ix) {
		IEditorPart page = getEditor(ix);
		if( page instanceof ICanRefresh) {
			ICanRefresh can = (ICanRefresh) page;
			can.doRefresh();
		}
	}

	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		doRefresh(newPageIndex);
	}

	@Override
	public void doSaveAs() {
		// not supported
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public FormToolkit getToolkit() {
		return toolkit;
	}

}
