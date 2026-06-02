/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.namespaces;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule143", description = "The CIM top-level package shall have a namespace specification")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Namespace, errorTemplate = "Top-level package {name} is missing a required namespace specifiction (i.e. 'nsprefix' and 'nsuri' tagged values).")
public class Rule143_MissingNamespaceSpecification extends OntResourceBaseRule {

	private static final String REGEX = "^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$";

	public Rule143_MissingNamespaceSpecification(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		return isPackage(resource) && resource.getLabel() != null && !resource.getLabel().matches(REGEX);
	}

}