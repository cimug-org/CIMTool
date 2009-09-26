package au.com.langdale.ui.binding;

/**
 * Validators implement this template.
 * FIXME: should be an interface.
 */
public abstract class Validator {
	/**
	 * Validate a value.
	 * 
	 * This method has the same contract as Binding.validate().
	 * @param value: the value to be validated
	 * @return: the validation error message or null if value is valid.
	 */
	public abstract String validate(String value);
}