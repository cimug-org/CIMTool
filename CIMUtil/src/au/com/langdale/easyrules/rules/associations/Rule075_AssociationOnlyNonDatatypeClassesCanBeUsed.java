/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getLabelDefaultUnknown;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.common.OntResourceBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule075", description = "Only classes without datatype stereotypes shall participate in associations.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "Only classes without restricted stereotypes may participate in associations. Currently, {elementTypeLowerCase} {name} uses the following restricted class(es):\n\n{restrictedClasses}")
public class Rule075_AssociationOnlyNonDatatypeClassesCanBeUsed extends OntResourceBaseRule {

	public Rule075_AssociationOnlyNonDatatypeClassesCanBeUsed(String baseURI) {
		super(baseURI);
	}

	@Condition
	public boolean when(@Fact("resource") OntResource resource) {

		if (isAssociation(resource) && isNormative(resource) && CIMRuleUtils.isSourceSide(resource)) {
			OntResource domain = resource.getDomain();
			if (domain != null && isRestrictedClass(domain)) {
				return true;
			}
		}
		return false;
	}

	protected String getRuleErrorMsg(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = getPlaceholderValues(resource, namesMap);
		String template = getErrorTemplate();
		StringBuffer restrictedClasses = new StringBuffer();
		if (resource.getDomain() != null && isRestrictedClass(resource.getDomain())) {
			String fullyQualifiedDomainClass = applyAsciidocStyling(getPackageHierarchy(resource.getDomain()) + "::" + getLabelDefaultUnknown(resource.getDomain()));
			restrictedClasses.append("\n").append("- ").append(fullyQualifiedDomainClass).append(" with restricted stereotype ").append(values.get(domainStereotype));
		}
		if (resource.getRange() != null && isRestrictedClass(resource.getRange())) {
			String fullyQualifiedRangeClass = applyAsciidocStyling(getPackageHierarchy(resource.getRange()) + "::" + getLabelDefaultUnknown(resource.getRange()));
			restrictedClasses.append("\n").append("- ").append(fullyQualifiedRangeClass).append(" with restricted stereotype ").append(values.get(rangeStereotype));
		}
		
		values.put("restrictedClasses", restrictedClasses.toString());
		
		if (template != null) {
			for (Map.Entry<String, String> entry : values.entrySet()) {
				template = template.replace("{" + entry.getKey() + "}", entry.getValue());
			}
		}
		
		return template;
	}

}