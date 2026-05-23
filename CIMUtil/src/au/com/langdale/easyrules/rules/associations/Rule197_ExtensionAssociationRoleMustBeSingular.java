/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

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

@Rule(name = "Rule197", description = "For all new associations it is recommended that end role names be expressed in the singular.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Association, severity = RuleSeverity.WARN, errorTemplate = "Extension association {name} end role name must be expressed in the singular.")
public class Rule197_ExtensionAssociationRoleMustBeSingular extends OntResourceBaseRule {

	public Rule197_ExtensionAssociationRoleMustBeSingular(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource)) {
			if (CIMRuleUtils.isPossiblyPlural(resource.getLabel()))
				return true;
		}
		return false;
	}

}