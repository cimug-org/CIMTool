/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule049", description = "Names for attributes shall use the Lower Camel Case naming convention")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, errorTemplate = "{elementType} {name} must be named using lowerCamelCase.")
public class Rule049_AttributeNameMustBeLowerCamelCase extends OntResourceBaseRule {

	public Rule049_AttributeNameMustBeLowerCamelCase(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && isNormative(resource)) {
			if (!CIMRuleUtils.isLowerCamelCase(resource.getLabel()))
				return true;
		}
		return false;
	}

}