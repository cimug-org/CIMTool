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

@Rule(name = "Rule146", description = "The Grid (formerly IEC61970) package shall have a namespace specification.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Namespace, errorTemplate = "Top-level package {name} is missing a required namespace specifiction (i.e. 'nsprefix' and 'nsuri' tagged values).")
public class Rule146_MissingGridPackageNamespaceSpecification extends OntResourceBaseRule {

	private static final String REGEX = "^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$";

	public Rule146_MissingGridPackageNamespaceSpecification(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (CIMRuleUtils.isGridPackage(resource)) {
			String nsuri = "http://iec.ch/TC57/CIM100#";
			return !nsuri.matches(REGEX);
		}
		return false;
	}

}