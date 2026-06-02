/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import static au.com.langdale.easyrules.rules.utils.BritishSpellingUtils.doesUpperCamelCaseNameContainAmericanEnglish;
import static au.com.langdale.easyrules.rules.utils.BritishSpellingUtils.getUpperCamelCaseNameInBritishEnglish;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule196:Rule069", description = "[Rule069] Names for extension association roles shall be British English names.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule069", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using British English. Suggested {elementTypeLowerCase} renaming: {correctedValue}.")
public class Rule196_Rule069_ExtensionAssociationRoleMustBeBritishEnglish extends OntResourceBaseRule {

	public Rule196_Rule069_ExtensionAssociationRoleMustBeBritishEnglish(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAssociation(resource) && !isNormative(resource) && CIMRuleUtils.isSourceSide(resource)) {
			if (doesUpperCamelCaseNameContainAmericanEnglish(resource.getLabel()))
				return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		values.put(correctedValue, getUpperCamelCaseNameInBritishEnglish(resource.getLabel()));
		return values;
	}

}