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

@Rule(name = "Rule144", description = "The value of the CIM (formerly TC57CIM) top-level package 'nsuri' tag shall be: http://iec.ch/TC57/CIM<version># where <version> = the version number of the CIM.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Namespace, errorTemplate = "The top-level {name} package's 'nsuri' is invalid: <{nsuri}>. It shall be: http://iec.ch/TC57/CIM<version># where <version> = the version number of the CIM.")
public class Rule144_CIMRootPackageInvalidNamespaceURI extends OntResourceBaseRule {

	private static final String REGEX = "^http://iec\\.ch/TC57/CIM\\d+#";

	public Rule144_CIMRootPackageInvalidNamespaceURI(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (CIMRuleUtils.isRootCIMPackage(resource)) {
			String nsuri = "http://iec.ch/TC57/CIM100#";
			return !nsuri.matches(REGEX);
		}
		return false;
	}

}