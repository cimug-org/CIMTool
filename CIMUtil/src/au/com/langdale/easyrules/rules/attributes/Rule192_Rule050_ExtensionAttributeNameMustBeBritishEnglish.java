/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

import static au.com.langdale.easyrules.rules.utils.BritishSpellingUtils.doesLowerCamelCaseNameContainAmericanEnglish;
import static au.com.langdale.easyrules.rules.utils.BritishSpellingUtils.getLowerCamelCaseNameInBritishEnglish;

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

@Rule(name = "Rule192:Rule050", description = "[Rule050] Names for attributes shall be British English names.")
@RuleMetadata(compositeRule = "Rule192", compositeSubRule = "Rule050", type = RuleType.Extension, category = RuleCategory.Attribute, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using British English. Suggested {elementTypeLowerCase} renaming: {correctedValue}.")
public class Rule192_Rule050_ExtensionAttributeNameMustBeBritishEnglish extends OntResourceBaseRule {

	public Rule192_Rule050_ExtensionAttributeNameMustBeBritishEnglish(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && !isNormative(resource)) {
			if (doesLowerCamelCaseNameContainAmericanEnglish(resource.getLabel()))
				return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		values.put(correctedValue, applyAsciidocBoldStyling(getLowerCamelCaseNameInBritishEnglish(resource.getLabel())));
		return values;
	}

}