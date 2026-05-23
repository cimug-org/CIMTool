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

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Extension:Rule091", description = "[Rule091] Enumeration literals shall have no datatype (by UML definition).")
@RuleMetadata(compositeRule = "Extension", compositeSubRule = "Rule091", type = RuleType.Extension, category = RuleCategory.Enumeration, errorTemplate = "Extension {elementTypeLowerCase} {name} must not have a datatype assigned.")
public class Extension_Rule091_EnumerationLiteralShallNotHaveADatatype extends OntResourceBaseRule {

	public Extension_Rule091_EnumerationLiteralShallNotHaveADatatype(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isEnumLiteral(resource) && !isNormative(resource) && resource.getRange() != null) {
			return true;
		}
		return false;
	}

}