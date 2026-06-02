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

@Rule(name = "Extension:Rule096", description = "[Rule096] Either all or no enumeration literals within a CIM type shall have a code. NOTE: Some of the existing normative CIM UML enumerations do not comply with this rule because changes would break existing integrations.")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule096", type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "Either all or no enumeration literals within extension {elementTypeLowerCase} {name} shall have a code.\n\nNOTE: Some existing normative CIM UML enumerations do not comply with this rule because changes would break existing integrations.")
public class Extension_Rule096_EnumerationCodeUsageMustBeConsistent extends OntResourceBaseRule {

	public Extension_Rule096_EnumerationCodeUsageMustBeConsistent(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isEnumeration(resource) && !isNormative(resource)) {
			int enumLiteralsCount = 0;
			int initialValuesCount = 0;
			int nonInitialValuesCount = 0;

			ResIterator enumLiterals = resource.getOntModel().listSubjectsBuffered(RDFS.domain, resource);
			while (enumLiterals.hasNext()) {
				OntResource enumLiteral = enumLiterals.nextResource();
				enumLiteralsCount++;
				if (enumLiteral.hasProperty(UML.hasInitialValue))
					initialValuesCount++;
				else 
					nonInitialValuesCount++;
			}
			if ((enumLiteralsCount > 0) && (initialValuesCount != 0) && (nonInitialValuesCount != 0))
				return true;
		}
		return false;
	}

}