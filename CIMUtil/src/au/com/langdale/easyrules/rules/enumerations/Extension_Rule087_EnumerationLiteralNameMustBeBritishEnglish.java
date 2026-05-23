/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

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

@Rule(name = "Extension:Rule087", description = "[Rule087] Names for enumeration literals shall be British English names")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule087", type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using British English. Suggested {elementTypeLowerCase} renaming: {correctedValue}.")
public class Extension_Rule087_EnumerationLiteralNameMustBeBritishEnglish extends OntResourceBaseRule {

	public Extension_Rule087_EnumerationLiteralNameMustBeBritishEnglish(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isEnumLiteral(resource) && !isNormative(resource)) {
			if (doesLowerCamelCaseNameContainAmericanEnglish(resource.getLabel()))
				return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		values.put(correctedValue,
				applyAsciidocBoldStyling(getLowerCamelCaseNameInBritishEnglish(resource.getLabel())));
		return values;
	}

}