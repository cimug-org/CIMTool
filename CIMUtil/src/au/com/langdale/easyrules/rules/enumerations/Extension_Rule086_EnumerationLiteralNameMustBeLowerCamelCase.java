/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Extension:Rule086", description = "[Rule086] Names for enumeration literals shall use the Lower Camel Case naming convention.")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule086", type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using lowerCamelCase.\n\nNOTE: Some existing normative CIM UML enumerations do not comply with this rule because changes would break existing integrations.")
public class Extension_Rule086_EnumerationLiteralNameMustBeLowerCamelCase extends OntResourceBaseRule {

	public Extension_Rule086_EnumerationLiteralNameMustBeLowerCamelCase(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isEnumLiteral(resource) && !isNormative(resource)) {
			if (!CIMRuleUtils.isLowerCamelCase(resource.getLabel()))
				return true;
		}
		return false;
	}

}