/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.namespaces;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Missing Extensions Namespace", description = "Missing namespace assignment for an extension class.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Namespace, errorTemplate = "Missing namespace assignment for an extension class. Class {name} is located in the non-standard CIM package {packageHierarchy} requiring that an extension namespace be assigned.")
public class MissingExtensionNamespaceRule extends OntResourceBaseRule {

	public MissingExtensionNamespaceRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isCIMUmlClass(resource) && CIMRuleUtils.isInExtensionPackage(resource)
				&& resource.getURI().equals(getBaseURI())) {
			return true;
		}
		return false;
	}

}