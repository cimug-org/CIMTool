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

@Rule(name = "Rule205", description = "CIM extension enumeration names shall comply with class extension rules specified in section 6.4 and shall end with the suffix 'Kind'.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named with a suffix of 'Kind'.")
public class Rule205_ExtensionEnumerationNameMustEndWithKind extends OntResourceBaseRule {

	public Rule205_ExtensionEnumerationNameMustEndWithKind(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isEnumeration(resource) && !isNormative(resource) && resource.getLabel() != null
				&& !resource.getLabel().endsWith("Kind")) {
			if (!CIMRuleUtils.isLowerCamelCase(resource.getLabel()))
				return true;
		}
		return false;
	}

}