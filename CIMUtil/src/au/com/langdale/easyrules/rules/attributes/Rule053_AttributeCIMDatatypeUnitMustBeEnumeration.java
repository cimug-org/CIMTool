/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule053", description = "The 'unit' attribute of a «CIMDatatype» class shall be an enumeration data type.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, errorTemplate = "The *unit* attribute of a «CIMDatatype» class must have a declared type this is a «enumeration» data type. It is currently defined as a {rangeName}.")
public class Rule053_AttributeCIMDatatypeUnitMustBeEnumeration extends OntResourceBaseRule {

	public Rule053_AttributeCIMDatatypeUnitMustBeEnumeration(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && isNormative(resource) && "unit".equalsIgnoreCase(resource.getLabel())
				&& (resource.getDomain() != null
						&& resource.getDomain().hasProperty(UML.hasStereotype, UML.cimdatatype))) {
			OntResource range = resource.getRange();
			if ((range != null) && !range.hasProperty(UML.hasStereotype, UML.enumeration))
				return true;
		}
		return false;
	}

}