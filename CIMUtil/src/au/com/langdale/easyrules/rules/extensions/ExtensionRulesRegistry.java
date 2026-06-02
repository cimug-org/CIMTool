/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.extensions;

import au.com.langdale.easyrules.rules.classes.Rule187_Rule038_ExtensionClassNameMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule040_ExtensionClassNameMustBeUnique;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule042_ExtensionClassNameMustBeSingular;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes;

import org.jeasy.rules.api.Rules;

public class ExtensionRulesRegistry {

	public static Rules getShadowExtensionRules(String baseURI) {
		Rules rules = new Rules();
		rules.register(new Rule187_Rule038_ExtensionClassNameMustBeUpperCamelCase(baseURI));
		rules.register(new Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish(baseURI));
		rules.register(new Rule187_Rule040_ExtensionClassNameMustBeUnique(baseURI));
		rules.register(new Rule187_Rule042_ExtensionClassNameMustBeSingular(baseURI));
		rules.register(new Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes(baseURI));
		rules.register(new Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships(baseURI));
		rules.register(new Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes(baseURI));
		return rules;
	}

}