/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.inheritance;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule115", description = "Multiple inheritance shall not be used.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Inheritance, errorTemplate = "{elementType} {name} has multiple parent classes. Only single inheritance is allowed.")
public class Rule115_InheritanceMultipleInheritanceNotAllowed extends OntResourceBaseRule {

	public Rule115_InheritanceMultipleInheritanceNotAllowed(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isCIMUmlClass(resource) && !isEnumeration(resource) && isNormative(resource)) {
			ResIterator superClasses = resource.listSuperClasses(false);
			if (superClasses.hasNext()) {
				int normativeClassesCount = 0;
				while (superClasses.hasNext()) {
					OntResource superClass = superClasses.nextResource();
					if (!isShadowClass(getBaseURI(), superClass)) {
						normativeClassesCount++;
					}
				}
				if (normativeClassesCount > 1)
					return true;
			}
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		values.put("subClassName", resource.getLabel());
		ResIterator superClasses = resource.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			if (isNormative(superClass)) {
				// The below check indicates that the
				if (CIMRuleUtils.getDependencyPriority(resource) > CIMRuleUtils.getDependencyPriority(superClass)) {
					values.put("superClassName", superClass.getLabel());
					return values;
				}
			}
		}
		//
		return values;
	}

}