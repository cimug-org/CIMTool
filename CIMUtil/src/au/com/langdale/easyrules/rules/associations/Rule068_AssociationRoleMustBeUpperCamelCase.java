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

@Rule(name = "Rule068", description = "Names for association roles shall use the Upper Camel Case naming convention.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "{elementType} role {name} must be named in UpperCamelCase.")
public class Rule068_AssociationRoleMustBeUpperCamelCase extends OntResourceBaseRule {

	public Rule068_AssociationRoleMustBeUpperCamelCase(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && isNormative(resource) && CIMRuleUtils.isSourceSide(resource)) {
			if (!CIMRuleUtils.isUpperCamelCase(resource.getLabel())) {
				return true;
			}
		}
		return false;
	}

}