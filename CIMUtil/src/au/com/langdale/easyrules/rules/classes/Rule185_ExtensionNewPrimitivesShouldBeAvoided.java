/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleSeverity;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule185", description = "The creation of new primitive extension data types should be avoided. As a semantic data model, the CIM is intended to represent utility domain concepts, not extensive data typing available in implementation technologies.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Class, severity = RuleSeverity.INFO, errorTemplate = "The creation of new CIM {stereotype} extension classes should be avoided. Consider alternatives to the extension class {name}. . As a semantic data model, the CIM is intended to represent utility domain concepts, not extensive data typing available in implementation technologies. If you feel a new {stereotype} data type is needed contact the CIM Model Mangagement Team at mailto:CIM.MM.Team@UCATF.org")
public class Rule185_ExtensionNewPrimitivesShouldBeAvoided extends OntResourceBaseRule {

	public Rule185_ExtensionNewPrimitivesShouldBeAvoided(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isPrimitive(resource) && !isNormative(resource)) {
			return true;
		}
		return false;
	}

}