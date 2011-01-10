/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import au.com.langdale.ui.plumbing.ICanRefresh;
import au.com.langdale.ui.plumbing.Observer;
/**
 * An eclipse editor that is provided with an <code>Assembly</code> and event plumbing.
 */
public abstract class FurnishedEditor extends EditorPart implements Observer, ICanRefresh {
	private boolean dirty;

	public FurnishedEditor() {
		super();
	}

	public FurnishedEditor(String name) {
		super();
		setPartName(name);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if( dirty) {
			dirty = false;
			firePropertyChange(PROP_DIRTY);
		}
	}

	@Override
	public void doSaveAs() {
		// not used
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public final void createPartControl(Composite parent) {
		content = createContent();
		content.realise(parent, content.define());
		content.addBindings();
		content.doRefresh(); 
		
		ScrolledForm form = content.getForm();
		if( form != null && form.getText().length() == 0)
			form.setText(getTitle());
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	public void markDirty() {
		if( ! dirty ) {
			dirty = true;
			firePropertyChange(PROP_DIRTY);
		}
	}
	
	public void markValid() {}
	
	public void markInvalid(String message) {}
	
	public void doRefresh() {
		if( content != null) 
			content.doRefresh();
	}
	
	protected abstract class Content extends Assembly {
		public Content(FormToolkit toolkit, boolean trackDirtyState) {
			super(toolkit, trackDirtyState? FurnishedEditor.this: null, true);
		}
		
		public Content(FormToolkit toolkit) {
			this(toolkit, true);
		}
		
		protected abstract Template define();
		protected void addBindings() {}
	}
	
	private Content content;
	
	protected abstract Content createContent();
}
