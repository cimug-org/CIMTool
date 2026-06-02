/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleSeverity;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Extension:Rule090", description = "[Rule090] All enumeration literals shall use the stereotype «enum».")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule090", type = RuleType.Extension, category = RuleCategory.Enumeration, severity = RuleSeverity.WARN, errorTemplate = "Extension {elementTypeLowerCase} {name} must use the stereotype «enum».")
public class Extension_Rule090_EnumerationLiteralMustHaveAnEnumStereotype extends OntResourceBaseRule {

	public Extension_Rule090_EnumerationLiteralMustHaveAnEnumStereotype(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		// Here we have slightly different conditoinal logic as we're testing for
		// the absence of an <<enum>> stereotype on a enum literal attribute on an
		// enumeration.
		if (resource.hasProperty(UML.hasStereotype, UML.attribute) && !isNormative(resource) && //
				resource.getDomain() != null && //
				resource.getDomain().hasProperty(UML.hasStereotype, UML.enumeration)) {
			if (!resource.hasProperty(UML.hasStereotype, UML.enumliteral))
				return true;
		}

		return false;
	}

}