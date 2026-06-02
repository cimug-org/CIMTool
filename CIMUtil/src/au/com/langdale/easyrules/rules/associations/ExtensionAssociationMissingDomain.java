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

@Rule(name = "Missing Domain", description = "Extension associations much have a domain specified.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {name} has no domain specified.")
public class ExtensionAssociationMissingDomain extends OntResourceBaseRule {

	public ExtensionAssociationMissingDomain(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		return isAssociation(resource) && !isNormative(resource) && resource.getDomain() == null;
	}

}