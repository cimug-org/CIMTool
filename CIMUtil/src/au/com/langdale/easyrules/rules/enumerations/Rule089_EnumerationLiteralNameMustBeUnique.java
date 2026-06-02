/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

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

@Rule(name = "Rule089", description = "Enumeration literals must be unique among all levels of specialisation.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Enumeration, errorTemplate = "{elementType} {name} is not unique among all levels of specialisation. Duplicate identified: {duplicateEnumLiteral}")
public class Rule089_EnumerationLiteralNameMustBeUnique extends OntResourceBaseRule {

	public Rule089_EnumerationLiteralNameMustBeUnique(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isEnumLiteral(resource) && isNormative(resource)) {

			String name = resource.getLabel();
			if (resource.getDomain() != null) {

				OntResource domain = resource.getDomain();
				ResIterator superClasses = domain.listSuperClasses(false);

				while (superClasses.hasNext()) {
					OntResource superClass = superClasses.nextResource();
					ResIterator properties = resource.getOntModel().listSubjectsWithProperty(RDFS.domain, superClass);
					while (properties.hasNext()) {
						OntResource property = properties.nextResource();
						if (!property.isObjectProperty()
								|| (property.isObjectProperty() && property.isDatatypeProperty())) {
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
		String name = resource.getLabel();
		if (resource.getDomain() != null) {
			OntResource domain = resource.getDomain();
			ResIterator superClasses = domain.listSuperClasses(false);
			while (superClasses.hasNext()) {
				OntResource superClass = superClasses.nextResource();
				ResIterator properties = resource.getOntModel().listSubjectsWithProperty(RDFS.domain, superClass);
				while (properties.hasNext()) {
					OntResource property = properties.nextResource();
					if (!property.isObjectProperty()
							|| (property.isObjectProperty() && property.isDatatypeProperty())) {
						if (name.equals(property.getLabel())) {
							StringBuffer duplicateEnumLiteral = new StringBuffer();
							duplicateEnumLiteral.append(getPackageHierarchy(property)).append("::")
									.append(getLabelDefaultUnknown(property.getDomain())).append(".")
									.append(getLabelDefaultUnknown(property));
							values.put("duplicateEnumLiteral", duplicateEnumLiteral.toString());
							break;
						}
					}
				}
			}
		}
		//
		return values;
	}

}