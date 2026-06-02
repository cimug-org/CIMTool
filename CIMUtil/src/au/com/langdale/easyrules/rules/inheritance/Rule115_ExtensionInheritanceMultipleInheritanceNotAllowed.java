/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.inheritance;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule115", description = "Multiple inheritance shall not be used.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Inheritance, errorTemplate = "Extension {elementTypeLowerCase} {name} has multiple parent classes. Only single inheritance is allowed.")
public class Rule115_ExtensionInheritanceMultipleInheritanceNotAllowed extends OntResourceBaseRule {

	public Rule115_ExtensionInheritanceMultipleInheritanceNotAllowed(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		// We intentionally exclude enumerations and leave that to Rule119...
		if (isCIMUmlClass(resource) && !isEnumeration(resource) && !isNormative(resource)
				&& !isShadowClass(getBaseURI(), resource)) {
			String uri = resource.getURI().substring(0, resource.getURI().indexOf("#"));
			ResIterator superClasses = resource.listSuperClasses(false);
			if (superClasses.hasNext()) {
				int totalSuperClassesCount = 0;
				int shadowClassesCount = 0;
				while (superClasses.hasNext()) {
					OntResource superClass = superClasses.nextResource();
					totalSuperClassesCount++;
					if (isShadowClass(uri, superClass)) {
						shadowClassesCount++;
					}
				}
				if ((totalSuperClassesCount - shadowClassesCount) != 1)
					return true;
			}
		}
		return false;
	}

}