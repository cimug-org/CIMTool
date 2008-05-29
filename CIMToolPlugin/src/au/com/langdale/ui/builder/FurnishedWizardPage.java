/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * A wizard page provided with a ContentBuilder and event plumbing.
 */
public abstract class FurnishedWizardPage extends WizardPage {

	public FurnishedWizardPage(String pageName) {
		super(pageName);
	}

	public FurnishedWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	public abstract class Content extends ContentBuilder {
		public Content(FormToolkit toolkit) {
			super(toolkit, true);
		}

		public Content() {
			super(createDialogToolkit(), true);
		}

		@Override
		public void markInvalid(String message) {
			message = message.length()>0?message:null;
			super.markInvalid(message);
			setErrorMessage(message);
			setPageComplete(false);
		}

		@Override
		public void markValid() {
			super.markValid();
			setErrorMessage(null);
			setPageComplete(true);

			IWizardPage next = getNextPage();
			if( next instanceof FurnishedWizardPage) {
				Content nextContent = ((FurnishedWizardPage)next).getContent();
				if( nextContent != null)
					nextContent.fireValidate();
			}

		}
	}
	
	private Content content;
	
	protected abstract Content createContent();
	
	public Content getContent() {
		return content;
	}

	public final void createControl(Composite parent) {
		content = createContent();
		setControl(content.realise(parent));
		content.doRefresh(); 
		ScrolledForm form = content.getForm();
		if( form != null && form.getText().length() == 0)
			form.setText(getTitle());
	}
}
