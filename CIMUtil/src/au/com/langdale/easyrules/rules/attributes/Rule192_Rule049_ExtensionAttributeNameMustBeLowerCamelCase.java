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

@Rule(name = "Rule192:Rule049", description = "[Rule049] Names for attributes shall use the Lower Camel Case naming convention")
@RuleMetadata(compositeRule = "Rule192", compositeSubRule = "Rule049", type = RuleType.Extension, category = RuleCategory.Attribute, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using lowerCamelCase.")
public class Rule192_Rule049_ExtensionAttributeNameMustBeLowerCamelCase extends OntResourceBaseRule {

	public Rule192_Rule049_ExtensionAttributeNameMustBeLowerCamelCase(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && !isNormative(resource)) {
			if (!CIMRuleUtils.isLowerCamelCase(resource.getLabel()))
				return true;
		}
		return false;
	}

}