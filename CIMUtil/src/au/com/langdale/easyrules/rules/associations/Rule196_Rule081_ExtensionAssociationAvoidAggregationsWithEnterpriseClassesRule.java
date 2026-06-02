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
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule196:Rule081", description = "[Rule081] Extension aggregation associations involving IEC 61968 classes should be avoided.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule081", type = RuleType.Extension, category = RuleCategory.Association, severity = RuleSeverity.WARN, errorTemplate = "Extension {elementTypeLowerCase} {name} is defined as an aggregation association that includes a class from the Enterprise (formerly IEC61968) package. This should be avoided.")
public class Rule196_Rule081_ExtensionAssociationAvoidAggregationsWithEnterpriseClassesRule
		extends OntResourceBaseRule {

	public Rule196_Rule081_ExtensionAssociationAvoidAggregationsWithEnterpriseClassesRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource)) {
			OntResource domain = resource.getDomain();
			OntResource range = resource.getRange();
			if (CIMRuleUtils.isInEnterprise(domain) || CIMRuleUtils.isInEnterprise(range)) {
				if (resource.hasProperty(UML.hasStereotype, UML.aggregateOf)
						|| resource.hasProperty(UML.hasStereotype, UML.ofAggregate)) {
					return true;
				}
			}
		}
		return false;
	}

}