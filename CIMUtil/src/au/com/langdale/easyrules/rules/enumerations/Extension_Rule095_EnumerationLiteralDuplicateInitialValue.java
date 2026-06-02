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

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.RDFS;

@Rule(name = "Extension:Rule095", description = "[Rule095] In instances where an initial value for extension enumeration literals is used, the code must be unique among all the codes for enumeration literals within the containing CIM class.")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule095", type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "In instances where an initial value for extension enumeration literals is used, the code must be unique among all the codes for enumeration literals within the containing extension enumeration. The initial value of {elementTypeLowerCase} {name} should be corrected in the model.")
public class Extension_Rule095_EnumerationLiteralDuplicateInitialValue extends OntResourceBaseRule {

	public Extension_Rule095_EnumerationLiteralDuplicateInitialValue(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isEnumLiteral(resource) && !isNormative(resource) && resource.hasProperty(UML.hasInitialValue)) {
			String initialValue = resource.getString(UML.hasInitialValue);
			OntResource enumeration = resource.getDomain();
			if (initialValue != null && enumeration != null) {
				ResIterator enumLiterals = enumeration.getOntModel().listSubjectsBuffered(RDFS.domain, enumeration);
				while (enumLiterals.hasNext()) {
					OntResource enumLiteral = enumLiterals.nextResource();
					if (enumLiteral.hasProperty(UML.hasInitialValue)) {
						String value = enumLiteral.getString(UML.hasInitialValue);
						if (value.equals(initialValue))
							return true;
					}
				}
			}
		}
		return false;
	}

}