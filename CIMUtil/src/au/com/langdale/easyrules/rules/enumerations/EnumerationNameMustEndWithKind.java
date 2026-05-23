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

@Rule(name = "Enumeration Naming", description = "CIM enumeration names shall end with the suffix 'Kind'.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Enumeration, errorTemplate = "{elementType} {name} must be named with a suffix of 'Kind'. NOTE: There are some existing standard CIM classes that do not comply with this naming convention. This is an instance where backwards compatibility supersedes a modeling rule.")
public class EnumerationNameMustEndWithKind extends OntResourceBaseRule {

	public EnumerationNameMustEndWithKind(String baseURI) {
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