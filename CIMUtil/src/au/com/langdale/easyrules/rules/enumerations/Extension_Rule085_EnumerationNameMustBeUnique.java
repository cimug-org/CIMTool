/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Extension:Rule085", description = "[Rule085] All enumeration classes shall have unique names")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule085", type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "Extension {elementTypeLowerCase} {name} is not globally unique (duplicate detected).")
public class Extension_Rule085_EnumerationNameMustBeUnique extends OntResourceBaseRule {

	public Extension_Rule085_EnumerationNameMustBeUnique(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource, @Fact("namesMap") Map<String, List<String>> namesMap) {
		String className = resource.getLabel();
		if (isEnumeration(resource) && !isNormative(resource) && className != null && namesMap.containsKey(className)
				&& namesMap.get(className).size() > 1) {
			if (resource.hasProperty(UML.hasStereotype, UML.shadowextension)) {
				ResIterator subClasses = resource.listSubClasses(false);
				while (subClasses.hasNext()) {
					OntResource subClass = subClasses.nextResource();
					if (subClass.getLabel().equals(resource.getLabel()) && subClass.getURI().equals(getBaseURI())) {
						return false; // This is allowed for shadow classes so we return false
					}
				}
			} else if (!resource.getURI().startsWith(getBaseURI())) {
				// We know at this point that resource is an extension class
				// so we check to see if there exists a subclass that is
				// a normative CIM class with the same name.
				ResIterator subClasses = resource.listSubClasses(false);
				while (subClasses.hasNext()) {
					OntResource subClass = subClasses.nextResource();
					if (subClass.getLabel().equals(resource.getLabel()) && subClass.getURI().startsWith(getBaseURI())) {
						return false; // This is allowed for shadow classes so we return false
					}
				}
			} else if (resource.getURI().startsWith(getBaseURI())) {
				// We know at this point that res is a normative CIM class
				// so we now check to see if there exists a superclass that
				// is a shadow class with the same name.
				List<String> urisList = namesMap.get(className);
				int count = 0;
				for (String uri : urisList) {
					if (uri.equals(resource.getURI()))
						count++;
				}
				if (count == 1)
					return false;
			}
			return true; // duplicate class name
		}
		return false;
	}

}