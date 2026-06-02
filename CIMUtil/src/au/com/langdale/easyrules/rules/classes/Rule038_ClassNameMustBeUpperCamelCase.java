/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule038", description = "Names for classes shall use the Upper Camel Case naming convention")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Class, errorTemplate = "{elementType} {name} must be named in UpperCamelCase.")
public class Rule038_ClassNameMustBeUpperCamelCase extends OntResourceBaseRule {

	public Rule038_ClassNameMustBeUpperCamelCase(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isCIMUmlClass(resource) && isNormative(resource)) {
			if (!CIMRuleUtils.isUpperCamelCase(resource.getLabel()))
				return true;
		}
		return false;
	}

}