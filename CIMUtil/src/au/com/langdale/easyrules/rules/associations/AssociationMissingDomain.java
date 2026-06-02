/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Missing Domain", description = "Detects object properties with no domain")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "{elementType} {name} has no domain specified.")
public class AssociationMissingDomain extends OntResourceBaseRule {

	public AssociationMissingDomain(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		return isAssociation(resource) && isNormative(resource) && resource.getDomain() == null;
	}

}