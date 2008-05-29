/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import au.com.langdale.validation.Validation.Validator;
/**
 * A preference or property page provided with a ContentBuilder and
 * a set of bound templates for various property and preference types.
 */
public abstract class FurnishedPropertyPage extends PreferencePage 
			implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
	
	private Content content;
	private Control body;
	private IResource resource;

	public FurnishedPropertyPage(String title, ImageDescriptor image) {
		super(title, image);
	}

	public void init(IWorkbench workbench) {

	}

	public FurnishedPropertyPage(String title) {
		super(title);
	}

	public FurnishedPropertyPage() {
	}
	
	protected abstract class Content extends ContentBuilder {
		protected abstract class TextBinding extends BoundTemplate {
			public final QualifiedName symbol;
			public final Validator validator;

			public TextBinding(QualifiedName symbol, Validator validator) {
				this.symbol = symbol;
				this.validator = validator;
			}

			protected String getValue() {
				return getText(symbol.getLocalName()).getText().trim();
			}

			protected void setValue(String value) {
				setTextValue(symbol.getLocalName(), value);
			}

			public String validate() {
				return validator.validate(getValue());
			}

			public Control realise(Composite parent) {
				return Field(symbol.getLocalName()).realise(parent);
			}
		}
		
		protected abstract class OptionBinding extends BoundTemplate {
			public final QualifiedName symbol;
			public final String label;

			public OptionBinding(QualifiedName symbol, String label) {
				this.symbol = symbol;
				this.label = label;
			}

			protected boolean getValue() {
				return getButton(symbol.getLocalName()).getSelection();
			}

			protected void setValue(boolean value) {
				getButton(symbol.getLocalName()).setSelection(value);
			}

			public String validate() {
				return null;
			}

			public Control realise(Composite parent) {
				return CheckBox(symbol.getLocalName(), label).realise(parent);
			}
		}
		
		protected class Property extends TextBinding {
			public Property(QualifiedName symbol, Validator validator) {
				super( symbol, validator );
			}

			public void refresh() {
				try {
					setValue(getResource().getPersistentProperty(symbol));
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void update() {
				try {
					getResource().setPersistentProperty(symbol, getValue());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void reset() {
				setValue(getPreferenceStore().getString(symbol.getLocalName()));				
			}
		}
		
		protected class Preference extends TextBinding {
			public Preference(QualifiedName symbol, Validator validator) {
				super(symbol, validator);
			}

			public void refresh() {
				setValue(getPreferenceStore().getString(symbol.getLocalName()));		
			}

			public void reset() {
				setValue(getPreferenceStore().getDefaultString(symbol.getLocalName()));		
			}

			public void update() {
				getPreferenceStore().setValue(symbol.getLocalName(), getValue());
			}
		}
		
		protected class PreferenceOption extends OptionBinding {
			public PreferenceOption(QualifiedName symbol, String label) {
				super(symbol, label);
			}

			public void refresh() {
				setValue(getPreferenceStore().getBoolean(symbol.getLocalName()));		
			}

			public void reset() {
				setValue(getPreferenceStore().getDefaultBoolean(symbol.getLocalName()));		
			}

			public void update() {
				getPreferenceStore().setValue(symbol.getLocalName(), getValue());
			}
		}

		public Content() {
			super(createDialogToolkit(), false);
		}

		@Override
		public void markInvalid(String message) {
			super.markInvalid(message);
			setErrorMessage(message);
			setValid(false);
		}

		@Override
		public void markValid() {
			super.markValid();
			setErrorMessage(null);
			setValid(true);
		}
		
	}
	
	protected abstract Content createContent();

	@Override
	protected final Control createContents(Composite parent) {
		content = createContent();
		body = content.realise(parent);
		content.doRefresh(); 
		ScrolledForm form = content.getForm();
		if( form != null && form.getText().length() == 0)
			form.setText(getTitle());
		return body;
	}
	
	@Override
	public boolean performOk() {
		content.fireUpdate();
		return true;
	}

	@Override
	protected void performDefaults() {
		content.doReset();
		super.performDefaults();
	}

	public IAdaptable getElement() {
		return resource;
	}

	public IResource getResource() {
		return resource;
	}

	public void setElement(IAdaptable element) {
		resource = (IResource) element;
		
	}
}
