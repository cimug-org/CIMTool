/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.NodeIterator;
import au.com.langdale.kena.OntResource;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.OWL2;

@Rule(name = "Rule093", description = "Enumeration literals have no multiplicity (by UML definition).")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Enumeration, errorTemplate = "{elementType} {name} must not have a multiplicity defined.")
public class Rule093_EnumerationLiteralShallNotHaveMultiplicity extends OntResourceBaseRule {

	public Rule093_EnumerationLiteralShallNotHaveMultiplicity(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isEnumLiteral(resource) && isNormative(resource)) {
			NodeIterator cardIter = resource.listLiteralProperties(OWL2.cardinality);
			NodeIterator minIter = resource.listLiteralProperties(OWL2.minCardinality);
			NodeIterator maxIter = resource.listLiteralProperties(OWL2.maxCardinality);
			if (cardIter.hasNext() || minIter.hasNext() || maxIter.hasNext())
				return true;
		}

		return false;
	}

}