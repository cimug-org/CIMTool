/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.common;

import java.util.Map;

public interface RulePlaceholderValuesSupport {

	public static final String ELEM_TYPE_PACKAGE = "Package";
	public static final String ELEM_TYPE_CLASS = "Class";
	public static final String ELEM_TYPE_ENUMERATION = "Enumeration";
	public static final String ELEM_TYPE_ATTRIBUTE = "Attribute";
	public static final String ELEM_TYPE_ASSOCIATION = "Association";
	public static final String ELEM_TYPE_DEFAULT = "Element";
	//
	public static final String uri = "uri"; // The URI of the resource
	public static final String stereotype = "stereotype"; // The name of the class
	public static final String elementType = "elementType"; // The element type (in proper case)
	public static final String elementTypeLowerCase = "elementTypeLowerCase"; // The element type (in lower case)
	public static final String name = "name"; // The name of the class
	public static final String domainName = "domainName"; // The name of the domain
	public static final String domainStereotype = "domainStereotype"; // If relevant, the restricted stereotype of the
																		// domain
	public static final String rangeName = "rangeName"; // The name of the range
	public static final String rangeStereotype = "rangeStereotype"; // If relevant, the restricted stereotype of the
																	// range
	public static final String attributeCard = "attributeCard"; // The cardinality of the attribute
	public static final String enumLiteralCard = "enumLiteralCard";
	public static final String sourceRoleEnd = "sourceRoleEndName"; // The source side role end name
	public static final String targetRoleEnd = "targetRoleEndName"; // The target side role end name
	public static final String sourceCard = "sourceCard"; // The source side cardinality
	public static final String targetCard = "targetCard"; // The target side cardinality
	public static final String sourceClass = "sourceClass"; // The name of the currently modeled source side class
	public static final String targetClass = "targetClass"; // The name of the currently modeled target side class
	public static final String correctSourceClass = "correctSourceClass"; // The name of the correct source side class
	public static final String correctTargetClass = "correctTargetClass"; // The name of the correct target side class
	public static final String packageHierarchy = "packageHierarchy"; // The full package hierarchy of the class
	public static final String correctedValue = "correctedValue"; // A placeholder for any corrected value
	public static final String scope = "scope"; // The scope (e.g. Public/Private) of a class or attribute
	public static final String fullyQualifiedName = "fullyQualifiedName"; // The fully qualified of the class or element
	public static final String roleEndName = "roleEndName"; // The role end name for an association

	public default Map<String, String> getPlaceholderValues() {
		return Map.of();
	}

	public default String getPlaceholderValue(String placeholderKey) {
		if (getPlaceholderValues().containsKey(placeholderKey))
			return getPlaceholderValues().get(placeholderKey);
		return null;
	}

	public default String getPlaceholderValueElementType() {
		return getPlaceholderValue(elementType);
	}

	public default String getPlaceholderValueElementTypeLowerCase() {
		return getPlaceholderValue(elementTypeLowerCase);
	}

	public default String getPlaceholderValueName() {
		return getPlaceholderValue(name);
	}

	public default String getPlaceholderValueDomainName() {
		return getPlaceholderValue(domainName);
	}

	public default String getPlaceholderValueRangeName() {
		return getPlaceholderValue(rangeName);
	}

	public default String getPlaceholderValueAttributeCard() {
		return getPlaceholderValue(attributeCard);
	}

	public default String getPlaceholderValueSourceRoleEnd() {
		return getPlaceholderValue(sourceRoleEnd);
	}

	public default String getPlaceholderValueTargetRoleEnd() {
		return getPlaceholderValue(targetRoleEnd);
	}

	public default String getPlaceholderValueSourceCard() {
		return getPlaceholderValue(sourceCard);
	}

	public default String getPlaceholderValueTargetCard() {
		return getPlaceholderValue(targetCard);
	}

	public default String getPlaceholderValuePackageHierarchy() {
		return getPlaceholderValue(packageHierarchy);
	}

	public default String getPlaceholderValueCorrectedValue() {
		return getPlaceholderValue(correctedValue);
	}

	public default String getFullyQualifiedName() {
		return getPlaceholderValue(fullyQualifiedName);
	}

	public default String getRoleEndName() {
		return getPlaceholderValue(fullyQualifiedName);
	}

	public default String getSoureClassName() {
		return getPlaceholderValue(sourceClass);
	}

	public default String getTargetClassName() {
		return getPlaceholderValue(targetClass);
	}

}