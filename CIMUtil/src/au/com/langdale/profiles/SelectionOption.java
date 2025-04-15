
package au.com.langdale.profiles;

/**
 * Enumeration used to represent the options to apply when selecting classes in
 * the UI and adding them to the profile. It is intended to reflect the end
 * users selections within the UI and is used to pass those. Using a designation
 * class to represent the set of user selections allows for much more easily
 * adding additional selection options in the UI.
 */
public enum SelectionOption {

	// Do not change ordering, but append new options at the end of the list.
	NoOp(0 << 0), // 00000 (initialized to 0)
	Concrete(1 << 0), // 00001 (initialized to 1)
	PropertyRequired(1 << 1), // 00010 (initialized to 2)
	UseSchemaCardinality(1 << 2), // 00100 (initialized to 4)
	UseProfileCardinality(1 << 4), // 01000 (initialized to 8)
	IncludeAllAssociations(1 << 8);  // 10000 (initialized to 16)
	
	private final int value;

	SelectionOption(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	/**
	 * Accepts one or more selection options and returns the combined result as an
	 * integer.
	 * 
	 * @param options
	 * @return
	 */
	public static int encodeOptions(SelectionOption... options) {
		int result = 0;
		for (SelectionOption option : options) {
			result |= option.getValue(); // Combine options using bitwise OR
		}
		return result;
	}

	public static boolean hasOption(int encodedOptions, SelectionOption flag) {
		if (flag == null)
			return false;
		return (encodedOptions & flag.getValue()) != 0;
	}
}
