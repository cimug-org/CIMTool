/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

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

@Rule(name = "Rule194", description = "User-defined CIM extension attributes that are part of a user-defined CIM extension class shall not be stereotyped.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Attribute, errorTemplate = "User-defined CIM extension attribute {name} that is part of user-defined CIM extension class shall not be stereotyped. Attribute currently has stereotype {stereotype}.")
public class Rule194_ExtensionNoStereotypesOnAttributeOfUserDefinedExtensionClass extends OntResourceBaseRule {

	public Rule194_ExtensionNoStereotypesOnAttributeOfUserDefinedExtensionClass(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isAttribute(resource) && !isNormative(resource)) {
			OntResource domain = resource.getDomain();
			if (domain != null && !isNormative(domain) && resource.hasProperty(UML.hasStereotype)) {
				ResIterator stereotypes = resource.listProperties(UML.hasStereotype);
				while (stereotypes.hasNext()) {
					OntResource stereotype = stereotypes.nextResource();
					if (!stereotype.equals(UML.attribute) && !stereotype.getLabel().toLowerCase().equals("deprecated"))
						return true;
				}
			}
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		ResIterator stereotypes = resource.listProperties(UML.hasStereotype);
		while (stereotypes.hasNext()) {
			OntResource aStereotype = stereotypes.nextResource();
			if (!aStereotype.equals(UML.attribute) && !aStereotype.getLabel().toLowerCase().equals("deprecated")) {
				values.put(stereotype, aStereotype.getLabel());
				break;
			}
		}
		//
		return values;
	}

}