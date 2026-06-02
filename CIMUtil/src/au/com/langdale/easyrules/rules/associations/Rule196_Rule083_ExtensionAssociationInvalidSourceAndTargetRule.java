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

@Rule(name = "Rule196:Rule083", description = "[Rule083] Associations should be drawn from the dependent (source) class to the target class in Enterprise Architect to facilitate correct dependency processing.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule083", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {name} should be drawn from the dependent (source) class (i.e. {correctSourceClass}) to the target class (i.e. {correctTargetClass}) in Enterprise Architect to facilitate correct dependency processing. Source and target ends are currently swapped.")
public class Rule196_Rule083_ExtensionAssociationInvalidSourceAndTargetRule extends OntResourceBaseRule {

	public Rule196_Rule083_ExtensionAssociationInvalidSourceAndTargetRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource)) {
			OntResource domain = resource.getDomain();
			if (isShadowClass(getBaseURI(), domain)) {
				domain = getClassBeingShadowed(getBaseURI(), domain);
			}
			OntResource range = resource.getRange();
			if (isShadowClass(getBaseURI(), range)) {
				range = getClassBeingShadowed(getBaseURI(), range);
			}
			if (domain != null && range != null) {
				if (CIMRuleUtils.isSourceSide(resource)) {
					// If the association is a source-side association it means that the priority of
					// the domain is expected to be less than or equal to the range. If it isn't we
					// know that the source and target are not correct for the association.
					if (CIMRuleUtils.getDependencyPriority(domain) > CIMRuleUtils.getDependencyPriority(range)) {
						return true;
					}
				} else {
					// If the association is a target-side association it means that the priority of
					// the domain is expected to be greater than or equal to that of the range.
					// Again,
					// if it isn't we know that the source and target are not correct.
					if (CIMRuleUtils.getDependencyPriority(domain) < CIMRuleUtils.getDependencyPriority(range)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}