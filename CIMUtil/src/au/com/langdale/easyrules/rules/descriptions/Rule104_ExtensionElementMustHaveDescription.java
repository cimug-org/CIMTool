/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.descriptions;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.RDF;

@Rule(name = "Rule104", description = "All CIM packages, classes, and attributes shall contain a description in the 'Element Notes' field in Enterprise Architect (i.e. the 'Element Notes' field shall not be blank).")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Description, errorTemplate = "Extension {elementTypeLowerCase} {name} is missing a description.")
public class Rule104_ExtensionElementMustHaveDescription extends OntResourceBaseRule {

	public Rule104_ExtensionElementMustHaveDescription(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (!isNormative(resource) && resource.hasProperty(RDF.type) && !CIMRuleUtils.hasComment(resource)) {
			return true;
		}
		return false;
	}

}