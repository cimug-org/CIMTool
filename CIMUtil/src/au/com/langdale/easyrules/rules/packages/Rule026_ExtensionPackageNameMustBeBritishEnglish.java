/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.packages;

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

@Rule(name = "Rule026", description = "Names for extension packages shall be British English names")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Package, errorTemplate = "Extension {elementTypeLowerCase} {name} must be named using British English. Suggested class renaming: {correctedValue}.")
public class Rule026_ExtensionPackageNameMustBeBritishEnglish extends OntResourceBaseRule {

	public Rule026_ExtensionPackageNameMustBeBritishEnglish(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isPackage(resource) && !isNormative(resource)) {
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