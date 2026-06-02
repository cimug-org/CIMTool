/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleSeverity;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.RDFS;

@Rule(name = "Unused Class", description = "Detects classes with no instances or subclasses")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Class, severity = RuleSeverity.INFO, errorTemplate = "{elementType} {name} is unused within the model.")
public class ClassUnusedRule extends OntResourceBaseRule {

	public ClassUnusedRule(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		boolean isUsed = false;
		if (isCIMUmlClass(resource) && isNormative(resource)) {
			ResIterator subClasses = resource.listSubClasses(false);
			if (subClasses.hasNext())
				isUsed = true;
			//
			ResIterator superClasses = resource.listSuperClasses(false);
			if (superClasses.hasNext())
				isUsed = true;
			//
			ResIterator propertiesWithThisAsRange = resource.getOntModel().listSubjectsWithProperty(RDFS.range,
					resource);
			if (propertiesWithThisAsRange.hasNext())
				isUsed = true;
		}
		return !isUsed;
	}

}