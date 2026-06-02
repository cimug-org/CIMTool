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
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule196:Rule072", description = "[Rule072] CIM extensions shall not include composition associations between classes.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule072", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {name} is defined as a composition assocation. This should be avoided.")
public class Rule196_Rule072_ExtensionAssociationCompositionAssociationsNotAllowedRule extends OntResourceBaseRule {

	public Rule196_Rule072_ExtensionAssociationCompositionAssociationsNotAllowedRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource) && CIMRuleUtils.isSourceSide(resource)) {
			if (resource.hasProperty(UML.hasStereotype, UML.compositeOf)
					|| resource.hasProperty(UML.hasStereotype, UML.ofComposite)) {
				return true;
			}
		}
		return false;
	}

}