/**
 * 
 */
package au.com.langdale.ui.binding;

import org.eclipse.swt.widgets.Button;

import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.plumbing.Binding;

/**
 * Bind a Boolean to a CheckBox.
 */
public class CheckBoxBinding implements Binding {
	
	private Button checkbox;
	private boolean initialState = Boolean.FALSE;
	private boolean checked;
	
	public CheckBoxBinding() {
	}
	
	public CheckBoxBinding(boolean initialState) {
		this.initialState = initialState;
		this.checked = this.initialState;
	}
	
	public boolean getChecked() {
		return checked;
	}

	public void bind(String name, Assembly plumbing) {
		checkbox = plumbing.getButton(name);
		plumbing.addBinding(this);
	}

	public void reset() {
		System.out.println("CheckBoxBinding.reset() ");
		checkbox.setSelection(initialState);
	}

	public void refresh() {
		System.out.println("CheckBoxBinding.refresh() ");
		if(checkbox.getSelection() != checked)
			checkbox.setSelection(checked);
	}

	public void update() {
		System.out.println("CheckBoxBinding.update() ");
		checked = checkbox.getSelection();
	}

	@Override
	public String validate() {
		System.out.println("CheckBoxBinding.validate() ");
		return null;
	}

}
