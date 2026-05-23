/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.common;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.OntResource;

import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Fact;

/**
 * Base class for rule classes to enable access to rule metadata like name
 * description.
 */
public abstract class OntResourceBaseRule extends BaseRule {

	public OntResourceBaseRule(String baseURI) {
		super(baseURI);
	}

	protected RuleViolation createRuleViolation(OntResource resource, Map<String, List<String>> namesMap,
			String message) {
		return new RuleViolation( //
				getRuleId(), //
				getCompositeRuleId(), //
				getCompositeSubRuleId(), //
				getRuleType(), //
				getRuleCategory(), //
				message, //
				resource.getURI(), //
				resource.getURI().substring(0, resource.getURI().lastIndexOf("#") + 1), //
				resource.getLabel(), //
				getViolationSeverity(), //
				isNormative(resource), //
				getPackageHierarchy(resource), //
				getPlaceholderValues(resource, namesMap));
	}

	protected RuleViolation createRuleViolation(OntResource resource, Map<String, List<String>> namesMap) {
		return createRuleViolation(resource, namesMap, getRuleErrorMsg(resource, namesMap));
	}

	/**
	 * Replace placeholders in the errorMsg template with actual values. Supported
	 * format: {key} for replacement using Map values.
	 * 
	 * @return formatted error message string.
	 */
	protected String getRuleErrorMsg(OntResource resource, Map<String, List<String>> namesMap) {
		String template = getErrorTemplate();
		if (template != null) {
			for (Map.Entry<String, String> entry : getPlaceholderValues(resource, namesMap).entrySet()) {
				template = template.replace("{" + entry.getKey() + "}", entry.getValue());
			}
		}
		return template;
	}

	@Action
	public void then(@Fact("resource") OntResource resource, @Fact("namesMap") Map<String, List<String>> namesMap,
			@Fact("violations") List<RuleViolation> violations) {
		violations.add(createRuleViolation(resource, namesMap));
	}

}