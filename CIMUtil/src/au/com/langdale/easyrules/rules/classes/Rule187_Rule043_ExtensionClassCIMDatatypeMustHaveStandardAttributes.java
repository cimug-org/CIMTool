/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.RDFS;

@Rule(name = "Rule187:Rule043", description = "[Rule043] «CIMDatatype» classes must have attributes: value, unit, multiplier")
@RuleMetadata(compositeRule = "Rule187", compositeSubRule = "Rule043", type = RuleType.Extension, category = RuleCategory.Class)
public class Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes extends OntResourceBaseRule {

	public Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isCIMDatatype(resource) && !isNormative(resource)) {

			OntModel model = resource.getOntModel();
			ResIterator attributes = model.listSubjectsWithProperty(RDFS.domain, resource);

			boolean hasValue = false;
			boolean hasUnit = false;
			boolean hasMultiplier = false;

			while (attributes.hasNext()) {
				OntResource attr = attributes.nextResource();
				String label = attr.getLabel();
				if ("value".equalsIgnoreCase(label))
					hasValue = true;
				if ("unit".equalsIgnoreCase(label) || "units".equalsIgnoreCase(label))
					hasUnit = true;
				if ("multiplier".equalsIgnoreCase(label))
					hasMultiplier = true;
			}

			if (hasValue && hasUnit && hasMultiplier)
				return true;
		}
		return false;
	}

	@Action
	public void then(@Fact("resource") OntResource resource, @Fact("namesMap") Map<String, List<String>> namesMap,
			@Fact("violations") List<RuleViolation> violations) {
		OntModel model = resource.getOntModel();
		ResIterator attributes = model.listSubjectsWithProperty(RDFS.domain, resource);

		boolean hasValue = false;
		boolean hasUnit = false;
		boolean hasMultiplier = false;

		while (attributes.hasNext()) {
			OntResource attr = attributes.nextResource();
			String label = attr.getLabel();
			if ("value".equalsIgnoreCase(label))
				hasValue = true;
			if ("unit".equalsIgnoreCase(label) || "units".equalsIgnoreCase(label))
				hasUnit = true;
			if ("multiplier".equalsIgnoreCase(label))
				hasMultiplier = true;
		}

		StringBuilder missing = new StringBuilder();
		if (!hasValue)
			missing.append("'value', ");
		if (!hasUnit)
			missing.append("'unit', ");
		if (!hasMultiplier)
			missing.append("'multiplier', ");

		if (missing.length() > 0)
			missing.setLength(missing.length() - 2); // remove trailing comma

		createRuleViolation(resource, namesMap, "CIMDatatype {name} is missing required attributes: " + missing + ".");
	}
}