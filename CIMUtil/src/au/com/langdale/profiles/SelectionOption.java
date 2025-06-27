
package au.com.langdale.profiles;

import java.util.EnumSet;

/**
 * Enumeration used to represent the options to apply when selecting classes in
 * the UI and adding them to the profile. It is intended to reflect the full set
 * of possible end user selections within the UI that can be chosen.
 */
public enum SelectionOption {

	// Do not change ordering, but append new options at the end of the list.
	NoOp(0 << 0), // 0000000 (initialized to 0)
	Concrete(1 << 0), // 0000001 (initialized to 1)
	PropertyRequired(1 << 1), // 0000010 (initialized to 2)
	UseSchemaCardinality(1 << 2), // 0000100 (initialized to 4)
	UseProfileCardinality(1 << 3), // 0001000 (initialized to 8)
	IncludeOnlySourceSideAssociations(1 << 4),  // 0010000 (initialized to 16)
	IncludeAllAssociations(1 << 5),  // 0100000 (initialized to 32)
	ByReference(1 << 6);  // 1000000 (initialized to 64)
	
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
	
	public static EnumSet<SelectionOption> decodeOptions(int encodedOptions) {
	    EnumSet<SelectionOption> result = EnumSet.noneOf(SelectionOption.class);
	    for (SelectionOption option : values()) {
	        if ((encodedOptions & option.value) != 0) {
	            result.add(option);
	        }
	    }
	    return result;
	}

	public static boolean hasOption(int encodedOptions, SelectionOption flag) {
		if (flag == null)
			return false;
		return (encodedOptions & flag.getValue()) != 0;
	}

	@Override
	public String toString() {
	    return name() + "(" + value + ")";
	}
	
}
