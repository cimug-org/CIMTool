/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

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

@Rule(name = "Rule042", description = "All class names shall be singular")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Class, severity = RuleSeverity.WARN, errorTemplate = "{elementType} {name} has a name that appears to be plural but should be singular. The model should be reviewed.")
public class Rule042_ClassNameMustBeSingular extends OntResourceBaseRule {

	public Rule042_ClassNameMustBeSingular(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isCIMUmlClass(resource) && !isEnumeration(resource) && isNormative(resource)
				&& CIMRuleUtils.isPossiblyPlural(resource.getLabel())) {
			return true;
		}
		return false;
	}

}