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

@Rule(name = "Rule067", description = "Association ends shall have role names.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "{elementType} end {name} shall have role names.")
public class Rule067_AssociationMissingRole extends OntResourceBaseRule {

	public Rule067_AssociationMissingRole(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && isNormative(resource)) {
			if (!CIMRuleUtils.hasLabel(resource))
				return true;
		}
		return false;
	}

}