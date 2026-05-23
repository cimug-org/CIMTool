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
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule119", description = "Inheritance shall not be used with stereotyped classes.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Inheritance, errorTemplate = "Extensions inheritance may not be used with {elementType} {name}. Please review all modelling related to this element.")
public class Rule119_ExtensionInheritanceNotAllowedForDatatypes extends OntResourceBaseRule {

	public Rule119_ExtensionInheritanceNotAllowedForDatatypes(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (!isNormative(resource) && isRestrictedClass(resource)) {
			if (resource.hasProperty(UML.hasStereotype, UML.enumeration)) {
				ResIterator superClasses = resource.listSuperClasses(false);
				ResIterator subClasses = resource.listSubClasses(false);
				if (superClasses.hasNext()) {
					int superClassesCount = 0;
					int shadowClassesCount = 0;
					while (superClasses.hasNext()) {
						OntResource superClass = superClasses.nextResource();
						superClassesCount++;
						if (isShadowClass(getBaseURI(), superClass))
							shadowClassesCount++;
					}
					// Only parent shadow classes are allowed for an enumeration
					if (shadowClassesCount == 0 || (shadowClassesCount != superClassesCount))
						return true;
				}
				if (subClasses.hasNext())
					return true;
			} else {
				ResIterator superClasses = resource.listSuperClasses(false);
				ResIterator subClasses = resource.listSubClasses(false);
				if (superClasses.hasNext() || subClasses.hasNext())
					return true;
			}
		}
		return false;
	}

}