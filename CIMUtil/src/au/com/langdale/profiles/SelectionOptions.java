
package au.com.langdale.profiles;

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

	public boolean isPropertyRequired() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.PropertyRequired);
	}

	public boolean useSchemaCardinality() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.UseSchemaCardinality);
	}
	
	public boolean useProfileCardinality() {
		return SelectionOption.hasOption(selectionOptions, SelectionOption.UseProfileCardinality);
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



}
