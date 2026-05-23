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

@Rule(name = "Rule081", description = "Aggregation associations involving IEC 61968 classes should be avoided.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, severity = RuleSeverity.WARN, errorTemplate = "{elementType} {name} is defined as an aggregation association that includes a class from the Enterprise (formerly IEC61968) package. This should be avoided.")
public class Rule081_AssociationAvoidAggregationsWithEnterpriseClassesRule extends OntResourceBaseRule {

	public Rule081_AssociationAvoidAggregationsWithEnterpriseClassesRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && isNormative(resource)) {
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