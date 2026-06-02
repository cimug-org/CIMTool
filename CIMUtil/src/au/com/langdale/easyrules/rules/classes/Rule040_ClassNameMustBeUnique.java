/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getLabelDefaultUnknown;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

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

@Rule(name = "Rule040", description = "All class names shall be unique")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Class, errorTemplate = "{elementType} {fullyQualifiedName} is not globally unique. Duplicate(s) detected:\n {fullyQualifiedClasses}")
public class Rule040_ClassNameMustBeUnique extends OntResourceBaseRule {

	public Rule040_ClassNameMustBeUnique(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource, @Fact("namesMap") Map<String, List<String>> namesMap) {
		String className = resource.getLabel();
		if (isCIMUmlClass(resource) && isNormative(resource) && (className != null && !className.isBlank()
				&& namesMap.containsKey(className) && namesMap.get(className).size() > 1)) {
			return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		String fullyQualifiedResourceName = getPackageHierarchy(resource) + "::" + getLabelDefaultUnknown(resource);
		//
		int duplicateFullyQualifiedClassCount = 0;
		StringBuffer fullyQualifiedClasses = new StringBuffer();
		List<String> duplicates = namesMap.get(resource.getLabel());
		for (String fullyQualifiedClassName : duplicates) {
			if (!fullyQualifiedClassName.equals(fullyQualifiedResourceName)) {
				fullyQualifiedClasses.append("\n").append("- ").append(applyAsciidocStyling(fullyQualifiedClassName));
			} else {
				duplicateFullyQualifiedClassCount++;
			}
		}
		if (duplicateFullyQualifiedClassCount > 1) {
			fullyQualifiedClasses.append("\n").append("- ").append(applyAsciidocStyling(fullyQualifiedResourceName));
		}
		//
		values.put("fullyQualifiedClasses", fullyQualifiedClasses.toString());
		//
		return values;
	}

}