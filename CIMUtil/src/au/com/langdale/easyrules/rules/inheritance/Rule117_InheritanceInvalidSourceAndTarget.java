/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.inheritance;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getDependencyPriority;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule117", description = "In case the classes from two top-level packages are involved, the subclass must be in the dependent package, and inherit from the class in a package upon which it depends.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Inheritance, errorTemplate = "Generalisations between two normative CIM classes must honor CIM top-level package dependencies. The generalisation between subclass {subClassName} to parent class {superClassName} is invalid.")
public class Rule117_InheritanceInvalidSourceAndTarget extends OntResourceBaseRule {

	public Rule117_InheritanceInvalidSourceAndTarget(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isClass(resource) && isNormative(resource)) {
			ResIterator superClasses = resource.listSuperClasses(false);
			while (superClasses.hasNext()) {
				OntResource superClass = superClasses.nextResource();
				if (isNormative(superClass)) {
					// If the generalization is a source-side association it means that the priority
					// of the domain is expected to be less than or equal to the range. If it isn't
					// we know that the source and target are backwards for the association.
					if (getDependencyPriority(resource) > getDependencyPriority(superClass)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		values.put("subClassName", getPackageHierarchy(resource) + "::" + resource.getLabel());
		ResIterator superClasses = resource.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			if (isNormative(superClass)) {
				if (getDependencyPriority(resource) > getDependencyPriority(superClass)) {
					values.put("superClassName", getPackageHierarchy(superClass) + "::" + superClass.getLabel());
					return values;
				}
			}
		}
		//
		return values;
	}

}