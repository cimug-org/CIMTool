/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.DisplayField;
import static au.com.langdale.ui.builder.Templates.Field;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

import au.com.langdale.ui.binding.AnyModel;
import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.ColorUtils;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.builder.Templates.DefaultContentProvider;
import au.com.langdale.ui.plumbing.Binding;
import au.com.langdale.ui.plumbing.Observer;

/**
 * A preference or property page provided with a Assembly and a set of templates
 * for various property and preference types.
 */
public abstract class FurnishedPropertyPage extends PreferencePage
		implements Observer, IWorkbenchPreferencePage, IWorkbenchPropertyPage {

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

	public static abstract class TextBinding implements Template, Binding {
		public final QualifiedName symbol;
		public final Validator validator;
		private Assembly assembly;

		public TextBinding(QualifiedName symbol, Validator validator) {
			this.symbol = symbol;
			this.validator = validator;
		}

		protected String getValue() {
			return assembly.getText(symbol.getLocalName()).getText().trim();
		}

		protected void setValue(String value) {
			assembly.setTextValue(symbol.getLocalName(), value);
		}

		public String validate() {
			return validator.validate(getValue());
		}

		public Control realise(Composite parent, Assembly assembly) {
			this.assembly = assembly;
			assembly.addBinding(this);
			return Field(symbol.getLocalName()).realise(parent, assembly);
		}
	}
	
	public static abstract class ReadOnlyComboBinding implements Template, Binding, AnyModel {
		public final QualifiedName symbol;
		private ComboViewer viewer;
		public final Validator validator;
		private Assembly assembly;
		private Object data;
		private Object initialSelection;

		public ReadOnlyComboBinding(QualifiedName symbol, Validator validator) {
			this(symbol, validator, (Object) null, null);
		}
		
		public ReadOnlyComboBinding(QualifiedName symbol, Validator validator, Object data) {
			this(symbol, validator, data, null);
		}
		
		public ReadOnlyComboBinding(QualifiedName symbol, Validator validator, Object data, Object initialSelection) {
			super();
			this.symbol = symbol;
			this.validator = validator;
			this.data = data;
			this.initialSelection = initialSelection;
		}

		public String getValue() {
			String selection = (String) this.viewer.getStructuredSelection().getFirstElement();
			return selection;
		}

		public void setValue(Object value) {
			if (value != null && !"".equals(value) ) {
				this.viewer.setSelection(new StructuredSelection(value));
			}
		}
		
		public String validate() {
			return this.validator.validate(getValue());
		}

		public Control realise(Composite parent, Assembly assembly) {
			assembly.addBinding(this);
			
			Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			combo.setLayoutData(gd);

			assembly.getToolkit().adapt(combo, true, true);
			this.viewer = new ComboViewer(combo);
			assembly.putViewer(this.symbol.getLocalName(), this.viewer);
			this.viewer.addSelectionChangedListener(assembly.selectionChangedlistener);
			this.viewer.setContentProvider(new DefaultContentProvider());
			if (this.data != null) {
				this.viewer.setInput(this.data);
				if (this.initialSelection != null) {
					this.viewer.setSelection(new StructuredSelection(this.initialSelection));
				}
			}
			
			combo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					System.err.println(event.toString());
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent event) {
					System.err.println(event.toString());
				}
			});

			return combo;
		}
	}
	
	private static class MyColorFieldEditor extends ColorFieldEditor {
		private Button button;

		public MyColorFieldEditor(String name, String text, Composite area) {
			super(name, text, area);
		}

		protected Button getChangeControl(Composite parent) {
			if (button == null)
				button = super.getChangeControl(parent);
			return button;
		}

	}

	public static abstract class ColorDisplayBinding implements Template, Binding {
		public final QualifiedName symbol;
		private String text;
		private String initialColor;
		private MyColorFieldEditor editor;

		public ColorDisplayBinding(QualifiedName symbol, String text, String initialColor) {
			this.symbol = symbol;
			this.text = text;
			this.initialColor = initialColor;
		}

		protected String getValue() {
			return ColorUtils.parseRGBColor(editor.getColorSelector().getColorValue());
		}
		
		protected void setValue(String color) {
			if (color != null)
				editor.getColorSelector().setColorValue(ColorUtils.parseHexColor(color));
		}
		
		public String validate() {
			return null;
		}

		public Control realise(Composite parent, Assembly assembly) {
			assembly.addBinding(this);

			Composite area = new Composite(parent, SWT.NONE);
			this.editor = new MyColorFieldEditor(symbol.getLocalName() + "-editor", text, area);
			editor.getColorSelector().setColorValue(ColorUtils.parseHexColor(initialColor));

			Button button = editor.getChangeControl(area);
			assembly.putControl(symbol.getLocalName() + "-button", button); // register button with the assembly

			button.addListener(SWT.Selection, event -> {
				String selectedColor = initialColor;
				
				if (editor.getColorSelector().getColorValue() != null)
					selectedColor = ColorUtils.parseRGBColor(editor.getColorSelector().getColorValue());

				if (selectedColor != null) {
					Label label = assembly.getLabel(symbol.getLocalName() + "-label");
					if (label != null)
						label.setText(selectedColor);
				}
			});

			return area;
		}
	}

	public static abstract class DisplayTextBinding implements Template, Binding {
		public final QualifiedName symbol;
		private Assembly assembly;

		public DisplayTextBinding(QualifiedName symbol) {
			this.symbol = symbol;
		}

		protected String getValue() {
			return assembly.getText(symbol.getLocalName()).getText().trim();
		}

		protected void setValue(String value) {
			assembly.setTextValue(symbol.getLocalName(), value);
		}

		public String validate() {
			return null;
		}

		public Control realise(Composite parent, Assembly assembly) {
			this.assembly = assembly;
			assembly.addBinding(this);
			return DisplayField(symbol.getLocalName()).realise(parent, assembly);
		}
	}

	public static abstract class OptionBinding implements Template, Binding {
		public final QualifiedName symbol;
		public final String label;
		private Assembly assembly;

		public OptionBinding(QualifiedName symbol, String label) {
			this.symbol = symbol;
			this.label = label;
		}

		protected boolean getValue() {
			return assembly.getButton(symbol.getLocalName()).getSelection();
		}

		protected void setValue(boolean value) {
			assembly.getButton(symbol.getLocalName()).setSelection(value);
		}

		public String validate() {
			return null;
		}

		public Control realise(Composite parent, Assembly assembly) {
			this.assembly = assembly;
			assembly.addBinding(this);
			return CheckBox(symbol.getLocalName(), label).realise(parent, assembly);
		}
	}
	
	protected class PropertyOption extends OptionBinding {
		public PropertyOption(QualifiedName symbol, String label) {
			super(symbol, label);
		}

		public void refresh() {
			setValue(Boolean.valueOf(Info.getPropertyNoException(getResource(), symbol)));
		}

		public void update() {
			Info.putProperty(getResource(), symbol, Boolean.valueOf(getValue()).toString());
		}
		
		public void reset() {
			setValue(Boolean.valueOf(Info.getPreference(symbol)));
		}
		
	}
	
	protected class ColorProperty extends ColorDisplayBinding {

		public ColorProperty(String text, QualifiedName symbol) {
			super(symbol, text, Info.getPropertyNoException(getResource(), symbol));
		}

		public void refresh() {
			setValue(Info.getPropertyNoException(getResource(), symbol));
		}

		public void update() {
			Info.putProperty(getResource(), symbol, getValue());
		}

		public void reset() {
			setValue(Info.getPreference(symbol));
		}
		
	}
	
	protected class PropertyReadOnlyCombo extends ReadOnlyComboBinding {

		public PropertyReadOnlyCombo(QualifiedName symbol, Validator validator) {
			super(symbol, validator, null, Info.getPropertyNoException(getResource(), symbol));
		}
		
		public PropertyReadOnlyCombo(QualifiedName symbol, Validator validator, Object data) {
			super(symbol, validator, data, Info.getPropertyNoException(getResource(), symbol));
		}

		public void refresh() {
			setValue(Info.getPropertyNoException(getResource(), symbol));
		}

		public void update() {
			Info.putProperty(getResource(), symbol, getValue());
		}

		public void reset() {
			setValue(Info.getPreference(symbol));
		}
		
	}
	
	protected class Property extends TextBinding {
		public Property(QualifiedName symbol, Validator validator) {
			super(symbol, validator);
		}

		public void refresh() {
			try {
				setValue(Info.getProperty(getResource(), symbol));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		public void update() {
			Info.putProperty(getResource(), symbol, getValue());
		}

		public void reset() {
			setValue(Info.getPreference(symbol));
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
	
	protected class PreferenceReadOnlyCombo extends ReadOnlyComboBinding {

		public PreferenceReadOnlyCombo(QualifiedName symbol, Validator validator) {
			super(symbol, validator, null, getPreferenceStore().getString(symbol.getLocalName()));
		}
		
		public PreferenceReadOnlyCombo(QualifiedName symbol, Validator validator, Object data) {
			super(symbol, validator, data, getPreferenceStore().getString(symbol.getLocalName()));
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
	
	protected class ColorPreference extends ColorDisplayBinding {

		public ColorPreference(String text, QualifiedName symbol) {
			super(symbol, text, getPreferenceStore().getString(symbol.getLocalName()));
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

	protected class DisplayPreference extends DisplayTextBinding {
		public DisplayPreference(QualifiedName symbol) {
			super(symbol);
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

	protected abstract class Content extends Assembly {
		public Content() {
			super(createDialogToolkit(), FurnishedPropertyPage.this, false);
		}

		protected abstract Template define();

		protected void addBindings() {
		}
	}

	protected abstract Content createContent();

	@Override
	protected final Control createContents(Composite parent) {
		content = createContent();
		body = content.realise(parent, content.define());
		content.addBindings();
		content.doRefresh();
		return body;
	}

	public void markInvalid(String message) {
		setErrorMessage(message);
		setValid(false);
	}

	public void markValid() {
		setErrorMessage(null);
		setValid(true);
	}

	public void markDirty() {

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
