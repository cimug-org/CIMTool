package au.com.langdale.profiles;

import java.util.EnumSet;

/**
 * A class representing the set of end-user selections to be applies 
 * when selecting classes in the UI and adding them to the profile.
 * Representing them in this manner allows for easier extensibility
 * and method signatures throughout the application do not need to
 * be modified.
 */
public class SelectionOptions {
	
	private int selectionOptions; 

	public SelectionOptions() {
		// All selection flags are false...
		selectionOptions = 0; 
	}
	
	public SelectionOptions(int selectionOptions) {
		this.selectionOptions = selectionOptions;
	}

	public SelectionOptions(SelectionOption... options) {
		this.selectionOptions = SelectionOption.encodeOptions(options);
	}

	public boolean isConcrete() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.Concrete);
	}
	
	public boolean isByReference() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.ByReference);
	}

	public boolean isPropertyRequired() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.PropertyRequired);
	}

	public boolean useSchemaCardinality() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.UseSchemaCardinality);
	}
	
	public boolean useProfileCardinality() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.UseProfileCardinality);
	}
	
	public boolean includeOnlySourceSideAssociations() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.IncludeOnlySourceSideAssociations);
	}
	
	public boolean includeAllAssociations() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.IncludeAllAssociations);
	}

	public SelectionOptions cloneAndRemove(SelectionOption... optionsToRemove) {
		// We initialze a "clone" of the selectionOptions and
		// remove the options passed in...
		int result = selectionOptions;
		for (SelectionOption optionToRemove : optionsToRemove) {
			result = result & ~optionToRemove.getValue();
		}
		return new SelectionOptions(result);
	}

	@Override
	public String toString() {
		EnumSet<SelectionOption> options = SelectionOption.decodeOptions(selectionOptions);
		StringBuffer s = new StringBuffer();
		s.append("SelectionOptions [selectionOptions=");
		s.append(options.toString());
		s.append("]");
		return s.toString();
	}

}
