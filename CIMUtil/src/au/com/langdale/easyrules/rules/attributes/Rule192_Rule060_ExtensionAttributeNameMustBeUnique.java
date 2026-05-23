/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

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

import com.hp.hpl.jena.vocabulary.RDFS;

@Rule(name = "Rule192:Rule060", description = "[Rule060] Extension attribute names shall be unique among all levels of specialisation.")
@RuleMetadata(compositeRule = "Rule192", compositeSubRule = "Rule060", type = RuleType.Extension, category = RuleCategory.Attribute, errorTemplate = "Extension {elementTypeLowerCase} {name} is not unique among all levels of specialisation. Duplicate identified:\n {duplicateAttributes}")
public class Rule192_Rule060_ExtensionAttributeNameMustBeUnique extends OntResourceBaseRule {

	public Rule192_Rule060_ExtensionAttributeNameMustBeUnique(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isAttribute(resource) && !isNormative(resource)) {

			String name = resource.getLabel();
			if (resource.getDomain() != null) {
				OntResource domain = resource.getDomain();
				ResIterator superClasses = domain.listSuperClasses(false);
				while (superClasses.hasNext()) {
					OntResource superClass = superClasses.nextResource();
					ResIterator properties = resource.getOntModel().listSubjectsWithProperty(RDFS.domain, superClass);
					while (properties.hasNext()) {
						OntResource property = properties.nextResource();
						if (!property.isObjectProperty()) {
							if (name.equals(property.getLabel()))
								return true;
						}
					}
				}
			}
		}

		return false;
	}

	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		//
		StringBuffer duplicateAttributes = new StringBuffer();
		String name = resource.getLabel();
		OntResource domain = resource.getDomain();
		ResIterator superClasses = domain.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			ResIterator properties = resource.getOntModel().listSubjectsWithProperty(RDFS.domain, superClass);
			while (properties.hasNext()) {
				OntResource property = properties.nextResource();
				if (!property.isObjectProperty()) {
					if (name.equals(property.getLabel())) {
						StringBuffer duplicateAttribute = new StringBuffer();
						duplicateAttribute
								.append((!getPackageHierarchy(property).isBlank() ? getPackageHierarchy(property) + "::"
										: ""))
								.append(getLabelDefaultUnknown(property.getDomain())).append(".")
								.append(getLabelDefaultUnknown(property));
						duplicateAttributes.append("\n").append("- ")
								.append(applyAsciidocStyling(duplicateAttribute.toString()));
					}
				}
			}
		}
		values.put("duplicateAttributes", duplicateAttributes.toString());
		//
		return values;
	}

}