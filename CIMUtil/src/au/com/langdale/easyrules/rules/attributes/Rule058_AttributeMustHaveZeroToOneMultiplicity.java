/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule058", description = "Attribute multiplicity shall always be [0..1] (i.e. all CIM attributes are optional).")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, errorTemplate = "{elementType} {name} has invalid multiplicity [{attributeCard}].")
public class Rule058_AttributeMustHaveZeroToOneMultiplicity extends OntResourceBaseRule {

	public Rule058_AttributeMustHaveZeroToOneMultiplicity(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isAttribute(resource) && isNormative(resource)) {

			Integer minCard = 0; // Default min cardinality
			Integer maxCard = 1; // Default max cardinality
			//
			Integer min = resource.getInteger(UML.hasMinCardinality);
			if (min != null)
				minCard = min.intValue();
			//
			Integer max = resource.getInteger(UML.hasMaxCardinality);
			if (max != null)
				maxCard = max.intValue();

			if (minCard > 0 || maxCard != 1)
				return true;
		}
		return false;
	}

}