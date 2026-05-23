/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.hasComment;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule196:Rule070", description = "[Rule070] Extension association ends shall have descriptions.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule070", type = RuleType.Extension, category = RuleCategory.Description, errorTemplate = "Extension {elementTypeLowerCase} role end {name} is missing a description.")
public class Rule196_Rule070_ExtensionAssociationRoleMissingDescription extends OntResourceBaseRule {

	public Rule196_Rule070_ExtensionAssociationRoleMissingDescription(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource) && !hasComment(resource)) {
			return true;
		}
		return false;
	}

}