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

@Rule(name = "Rule145", description = "The value of the top-level CIM (formerly TC57CIM) package 'nsprefix' tag shall be 'cim'.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Namespace, errorTemplate = "The top-level {name} package's 'nsprefix' tag is invalid: <{nsprefix}>. It shall be 'cim'.")
public class Rule145_CIMRootPackageInvalidNamespacePrefix extends OntResourceBaseRule {

	public Rule145_CIMRootPackageInvalidNamespacePrefix(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (CIMRuleUtils.isRootCIMPackage(resource)) {
			String nsprefix = "cim";
			return !nsprefix.equalsIgnoreCase("cim");
		}
		return false;
	}
}