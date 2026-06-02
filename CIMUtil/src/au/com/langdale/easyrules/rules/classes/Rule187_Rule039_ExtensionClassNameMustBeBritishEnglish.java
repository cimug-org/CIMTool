/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import static au.com.langdale.easyrules.rules.utils.BritishSpellingUtils.doesUpperCamelCaseNameContainAmericanEnglish;
import static au.com.langdale.easyrules.rules.utils.BritishSpellingUtils.getUpperCamelCaseNameInBritishEnglish;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule187:Rule039", description = "[Rule039] Names for classes shall be British English names")
@RuleMetadata(compositeRule = "Rule187", compositeSubRule = "Rule039", type = RuleType.Extension, category = RuleCategory.Class, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using British English. Suggested {elementTypeLowerCase} renaming: {correctedValue}.")
public class Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish extends OntResourceBaseRule {

	public Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isCIMUmlClass(resource) && !isNormative(resource)) {
			if (doesUpperCamelCaseNameContainAmericanEnglish(resource.getLabel()))
				return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		values.put(correctedValue,
				applyAsciidocBoldStyling(getUpperCamelCaseNameInBritishEnglish(resource.getLabel())));
		return values;
	}

}