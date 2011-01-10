/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import au.com.langdale.ui.plumbing.Observer;

/**
 * A wizard page provided with a widget Assembly and event plumbing.
 */
public abstract class FurnishedWizardPage extends WizardPage implements Observer {

	public FurnishedWizardPage(String pageName) {
		super(pageName);
	}

	public FurnishedWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	public abstract class Content extends Assembly {
		public Content() {
			super(createDialogToolkit(), FurnishedWizardPage.this, true);
		}
		
		protected abstract Template define();
		protected void addBindings() {}
	}
	
	private Content content;

	protected abstract Content createContent();
	
	public Content getContent() {
		return content;
	}

	public final void createControl(Composite parent) {
		content = createContent();
		setControl(content.realise(parent, content.define()));
		content.addBindings();
		content.doRefresh(); 
	}

	public void markInvalid(String message) {
		setErrorMessage(message.length()>0?message:null);
		setPageComplete(false);
	}

	public void markValid() {
		setErrorMessage(null);
		setPageComplete(true);

		IWizardPage next = getNextPage();
		if( next instanceof FurnishedWizardPage) {
			Content nextContent = ((FurnishedWizardPage)next).getContent();
			if( nextContent != null)
				nextContent.doRefresh();
		}
	}
	
	public void markDirty() {
		
	}
}
