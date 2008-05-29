/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import org.eclipse.swt.widgets.Text;

import au.com.langdale.ui.plumbing.Binding;
import au.com.langdale.ui.plumbing.Plumbing;
import au.com.langdale.validation.Validation;
import au.com.langdale.validation.Validation.Validator;
/**
 * A text model and binding to a text control.
 */
public class TextBinding implements Binding, TextModel, AnyModel {
	
	protected String value = "";
	private Plumbing plumbing;
	private Validator validator = Validation.NONE;
	private Text control;
	private AnyModel parent;
	private String lastSuggestion = "";

	public TextBinding(Validator validator, String initial) {
		this.validator = validator;
		value = initial;
	}
	
	public TextBinding(Validator validator) {
		this(validator, "");
	}

	public void bind(String name, Plumbing plumbing, AnyModel parent) {
		this.plumbing = plumbing;
		this.parent = parent;
		control = (Text) plumbing.getControl(name);
		plumbing.addBinding(this, parent);
	}
	
	public void bind(String name, Plumbing plumbing) {
		bind(name, plumbing, null);
	}

	private void applySuggestion() {
		String suggestion = createSuggestion();
		if( suggestion != null && ! suggestion.equals(lastSuggestion)) {
			lastSuggestion = suggestion;
			value = suggestion;
		}
	}

	protected String createSuggestion() {
		return lastSuggestion;
	}

	public String parentText() {
		AnyModel parent = getParent();
		Object parentRawValue = parent != null? parent.getValue(): "";
		return parentRawValue != null? parentRawValue.toString(): "";
	}

	public AnyModel getParent() {
		return parent;
	}
	
	public void refresh() {
		if( !control.getText().equals(value))
			control.setText(value);
	}

	public void reset() {
		control.setText(createSuggestion());
	}

	public void update() {
		value = control.getText().trim();
		applySuggestion();
	}

	public String validate() {
		if(control.isEnabled())
			return validator.validate(value);
		return null;
	}

	public String getText() {
		return value;
	}

	public void setText(String value) {
		this.value = value != null? value.trim(): "";
		if( plumbing != null)
			plumbing.doRefresh();
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		setText(value != null? value.toString(): null);
	}
}
