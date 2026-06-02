/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.packages;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.hasLabel;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.isUpperCamelCase;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule178", description = "CIM extension package names shall use the Upper Camel Case naming convention.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Package, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named in UpperCamelCase.")
public class Rule178_ExtensionPackageNameMustBeUpperCamelCase extends OntResourceBaseRule {

	public Rule178_ExtensionPackageNameMustBeUpperCamelCase(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isPackage(resource) && !isNormative(resource) && hasLabel(resource)) {
			if (!isUpperCamelCase(resource.getLabel()))
				return true;
		}
		return false;
	}

}