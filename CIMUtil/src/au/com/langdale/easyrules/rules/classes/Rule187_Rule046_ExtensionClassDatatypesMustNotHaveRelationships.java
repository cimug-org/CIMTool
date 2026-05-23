/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

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

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

@Rule(name = "Rule187:Rule046", description = "[Rule046] Datatype classes must not have associations or inheritance.")
@RuleMetadata(compositeRule = "Rule187", compositeSubRule = "Rule046", type = RuleType.Extension, category = RuleCategory.Class, errorTemplate = "Extension {elementTypeLowerCase} {name} has a restricted stereotype ({stereotype}) and therefore may not participate in relationships. The model should be reviewed as it is currently used in an association or generalisation.")
public class Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships extends OntResourceBaseRule {

	public Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (!isNormative(resource) && isRestrictedClass(resource) && !isShadowClass(getBaseURI(), resource)) {

			if (resource.hasProperty(UML.hasStereotype, UML.enumeration)) {
				ResIterator superClasses = resource.listSuperClasses(false);
				ResIterator subClasses = resource.listSubClasses(false);
				if (isShadowClass(getBaseURI(), resource)) {
					if (subClasses.hasNext()) {
						int subClassesCount = 0;
						int normativeClassesCount = 0;
						while (subClasses.hasNext()) {
							OntResource subClass = subClasses.nextResource();
							subClassesCount++;
							if (subClass.getURI().startsWith(getBaseURI()))
								normativeClassesCount++;
						}
						// Only child normative classes are allowed for an enumeration
						if (!((normativeClassesCount == 1 && subClassesCount == 1)
								&& isShadowClass(getBaseURI(), resource)))
							return true;
					}
					if (superClasses.hasNext())
						return true;
				} else {
					if (superClasses.hasNext() || subClasses.hasNext())
						return true;
				}
			} else {
				ResIterator superClasses = resource.listSuperClasses(false);
				ResIterator subClasses = resource.listSubClasses(false);
				if (superClasses.hasNext() || subClasses.hasNext())
					return true;
			}
			//
			ResIterator propertiesWithThisAsDomain = resource.getOntModel().listSubjectsWithProperty(RDFS.domain,
					resource);
			while (propertiesWithThisAsDomain.hasNext()) {
				OntResource property = propertiesWithThisAsDomain.nextResource();
				if (property.isObjectProperty() && property.hasProperty(OWL.inverseOf))
					return true;
			}
		}
		return false;
	}

}