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
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule051", description = "Attribute names shall be singular form concepts.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, severity = RuleSeverity.INFO, errorTemplate = "{elementType} {name} has a name that appears to be plural but should be singular. Please review and correct if necessary.")
public class Rule051_AttributeNameMustBeSingular extends OntResourceBaseRule {

	public Rule051_AttributeNameMustBeSingular(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && isNormative(resource)) {
			if (CIMRuleUtils.isPossiblyPlural(resource.getLabel()))
				return true;
		}
		return false;
	}

}