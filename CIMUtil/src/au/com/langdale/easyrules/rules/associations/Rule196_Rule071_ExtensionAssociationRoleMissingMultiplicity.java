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

@Rule(name = "Rule196:Rule071", description = "[Rule071] Extension association ends shall have specified multiplicity.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule071", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} role end {fullyQualifiedName} has no multiplicity specified.")
public class Rule196_Rule071_ExtensionAssociationRoleMissingMultiplicity extends OntResourceBaseRule {

	public Rule196_Rule071_ExtensionAssociationRoleMissingMultiplicity(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource)) {
			if (!CIMRuleUtils.hasLabel(resource)) {
				return true;
			}
		}
		return false;
	}

}