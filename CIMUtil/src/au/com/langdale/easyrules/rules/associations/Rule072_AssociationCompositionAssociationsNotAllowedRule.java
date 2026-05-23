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

@Rule(name = "Rule072", description = "The CIM shall not include composition associations between classes.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "{elementType} {name} is defined as a composition assocation. This should be avoided.")
public class Rule072_AssociationCompositionAssociationsNotAllowedRule extends OntResourceBaseRule {

	public Rule072_AssociationCompositionAssociationsNotAllowedRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && isNormative(resource) && CIMRuleUtils.isSourceSide(resource)) {
			if (resource.hasProperty(UML.hasStereotype, UML.compositeOf)
					|| resource.hasProperty(UML.hasStereotype, UML.ofComposite)) {
				return true;
			}
		}
		return false;
	}

}