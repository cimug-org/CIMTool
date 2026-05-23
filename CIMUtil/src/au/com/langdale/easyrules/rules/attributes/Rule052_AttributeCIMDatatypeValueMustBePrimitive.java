/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleSeverity;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule052", description = "The 'value' attribute of a «CIMDatatype» class shall be a «Primitive» data type.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, severity = RuleSeverity.WARN, errorTemplate = "The *value* attribute of a «CIMDatatype» class must have a declared type this is a «Primitive» data type. It is currently defined as a {rangeName}.")
public class Rule052_AttributeCIMDatatypeValueMustBePrimitive extends OntResourceBaseRule {

	public Rule052_AttributeCIMDatatypeValueMustBePrimitive(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && isNormative(resource) && "value".equalsIgnoreCase(resource.getLabel())
				&& (resource.getDomain() != null
						&& resource.getDomain().hasProperty(UML.hasStereotype, UML.cimdatatype))) {
			OntResource range = resource.getRange();
			if ((range != null) && !range.hasProperty(UML.hasStereotype, UML.primitive))
				return true;
		}
		return false;
	}

}