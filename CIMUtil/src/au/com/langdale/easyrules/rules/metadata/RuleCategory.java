/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.metadata;

/**
 * Represents the specific category of UML model element that a rule applies to.
 */
public enum RuleCategory {

	Package("Package Rules [Section 5.3]", 1), //
	Class("Class Rules [Section 5.4]", 2), //
	Attribute("Attribute Rules [Section 5.5]", 3), //
	Association("Association Rules [Section 5.6]", 4), //
	Enumeration("Enumeration Rules [Section 5.7]", 5), //
	Description("Element Description Rules [Section 5.9]", 6), //
	Inheritance("Inheritance Rules [Section 5.10]", 7), //
	Stereotype("Stereotype Rules [Section 5.11]", 8), //
	Namespace("Namespace Rules [Section 5.12]", 9), //
	ShadowClass("Shadow Class Rules", 10);

	private final String sectionTitle;
	private final int order;

	RuleCategory(String sectionTitle, int order) {
		this.sectionTitle = sectionTitle;
		this.order = order;
	}

	public String getSectionTitle() {
		return sectionTitle;
	}

	public int getOrder() {
		return order;
	}
}