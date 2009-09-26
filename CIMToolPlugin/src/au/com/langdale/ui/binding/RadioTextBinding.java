package au.com.langdale.ui.binding;

import org.eclipse.swt.widgets.Button;

import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.validation.Validation.Validator;

public class RadioTextBinding extends TextBinding {

	private Button[] radios = new Button[0];
	private String[] values = new String[0];
	
	public RadioTextBinding(Validator validator, String initial) {
		super(validator, initial);
	}
	
	
	public void bind(String name, String[] nameValues, Assembly plumbing) {
		bind(name, plumbing, null);
		radios = new Button[nameValues.length/2];
		values = new String[nameValues.length/2];
		for(int ix = 0; ix + 1 < nameValues.length; ix += 2) {
			radios[ix/2] = plumbing.getButton(nameValues[ix]);
			values[ix/2] = nameValues[ix+1];
		}
	}
	
	@Override
	protected String createSuggestion() {
		for(int ix = 0; ix < radios.length; ix++) {
			if( radios[ix].getSelection())
				return values[ix];
		}
		return null;
	}
	
	@Override
	public void refresh() {
		super.refresh();
		for(int ix = 0; ix < radios.length; ix++) {
			radios[ix].setSelection(values[ix].equals(getValue()));
		}
	}
}
