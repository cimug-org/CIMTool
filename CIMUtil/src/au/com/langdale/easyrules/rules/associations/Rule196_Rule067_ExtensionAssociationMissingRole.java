/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule196:Rule067", description = "[Rule067] Extension association ends shall have role names.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule067", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {packageHierarchy}::{domainName}.<MissingRoleName>' on the {rangeName} end has missing role name. Please correct the model.")
public class Rule196_Rule067_ExtensionAssociationMissingRole extends OntResourceBaseRule {

	public Rule196_Rule067_ExtensionAssociationMissingRole(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource)) {
			if (!CIMRuleUtils.hasLabel(resource))
				return true;
		}
		return false;
	}

}