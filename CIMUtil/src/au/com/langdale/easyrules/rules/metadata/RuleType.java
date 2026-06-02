/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.metadata;

/**
 * A RuleType represents the type of modeling in the CIM that a specific Rule is
 * applicable for. Currently, there are two types or categories of rules and
 * they are defined in the guide as recommendations applicable to either
 * "standard CIM" (i.e. Standard) or to extensions modeled in the CIM (i.e.
 * Extension).
 */
public enum RuleType {

	Normative("CIM UML Modeling Rules and Recommendations [Section 5]"), //
	Extension("CIM UML Extension Rules and Recommendations [Section 6]"); //

	private final String sectionTitle;

	RuleType(String sectionTitle) {
		this.sectionTitle = sectionTitle;
	}

	public String getSectionTitle() {
		return sectionTitle;
	}

}