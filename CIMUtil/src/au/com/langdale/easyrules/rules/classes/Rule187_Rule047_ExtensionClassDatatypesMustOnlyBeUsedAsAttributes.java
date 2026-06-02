/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getLabelDefaultUnknown;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDFS;

@Rule(name = "Rule187:Rule047", description = "[Rule047] Extension datatype classes must only be used as the datatype for attributes.")
@RuleMetadata(compositeRule = "Rule187", compositeSubRule = "Rule047", type = RuleType.Extension, category = RuleCategory.Class, errorTemplate = "Extension {elementTypeLowerCase} {name} has restricted stereotype {stereotype} and therefore must only be used as a datatype for attributes. The model should be reviewed as it currently participates in the following association(s):\n {fullyQualifiedAssociations}")
public class Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes extends OntResourceBaseRule {

	public Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {
		if (isRestrictedClass(resource) && !isNormative(resource)) {

			ResIterator propertiesWithThisAsRange = resource.getOntModel().listSubjectsWithProperty(RDFS.range,
					resource);
			while (propertiesWithThisAsRange.hasNext()) {
				OntResource property = propertiesWithThisAsRange.nextResource();
				if (property.isObjectProperty() && property.hasProperty(OWL2.inverseOf))
					return true;
			}
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		StringBuffer fullyQualifiedAssociations = new StringBuffer();
		ResIterator propertiesWithThisAsRange = resource.getOntModel().listSubjectsWithProperty(RDFS.range, resource);
		while (propertiesWithThisAsRange.hasNext()) {
			OntResource property = propertiesWithThisAsRange.nextResource();
			if (property.isObjectProperty() && property.hasProperty(OWL2.inverseOf)) {
				StringBuffer duplicateAssociation = new StringBuffer();
				String packageHierarchy = getPackageHierarchy(property);
				packageHierarchy = (!packageHierarchy.isBlank() ? packageHierarchy + "::" : "");
				duplicateAssociation.append(packageHierarchy).append(getLabelDefaultUnknown(property.getDomain()))
						.append(".").append(getLabelDefaultUnknown(property));
				fullyQualifiedAssociations.append("\n").append("- ")
						.append(applyAsciidocStyling(duplicateAssociation.toString()));
			}
		}
		values.put("fullyQualifiedAssociations", fullyQualifiedAssociations.toString());
		//
		return values;
	}

}