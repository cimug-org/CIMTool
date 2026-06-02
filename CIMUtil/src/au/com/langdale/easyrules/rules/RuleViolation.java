/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules;

import au.com.langdale.easyrules.rules.common.RulePlaceholderValuesSupport;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleSeverity;
import au.com.langdale.easyrules.rules.metadata.RuleType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a rule violation including metadata to support filtering, sorting,
 * and reporting.
 */
public class RuleViolation implements Comparable<RuleViolation>, RulePlaceholderValuesSupport {

	private final String ruleId;
	private final String compositeRuleId;
	private final String compositeSubRuleId;
	private final RuleType type;
	private final RuleCategory category;
	private final String errorMsg;
	private final String resourceURI;
	private final String resourceLabel;
	private final RuleSeverity severity;
	private final boolean normative;
	private final String packageHierarchy;
	private final Map<String, String> placeholderValues;
	private final String resourceNamespace;
	//
	// ShadowExtension specific attributes
	private final String plantUML;
	private final List<RuleViolation> shadowExtensionViolations;

	public RuleViolation(String ruleId, RuleType type, RuleCategory category, String errorMsg, String resourceURI,
			String resourceNamespace, String resourceLabel, RuleSeverity severity, boolean normative,
			String packageHierarchy, Map<String, String> placeholderValues) {
		this(ruleId, null, null, type, category, errorMsg, resourceURI, resourceNamespace, resourceLabel, severity,
				normative, packageHierarchy, placeholderValues, null, null);
	}

	public RuleViolation(String ruleId, String compositeRuleId, String compositeSubRuleId, RuleType type,
			RuleCategory category, String errorMsg, String resourceURI, String resourceNamespace, String resourceLabel,
			RuleSeverity severity, boolean normative, String packageHierarchy, Map<String, String> placeholderValues) {
		this(ruleId, compositeRuleId, compositeSubRuleId, type, category, errorMsg, resourceURI, resourceNamespace,
				resourceLabel, severity, normative, packageHierarchy, placeholderValues, null, null);
	}

	public RuleViolation(String ruleId, String compositeRuleId, String compositeSubRuleId, RuleType type,
			RuleCategory category, String errorMsg, String resourceURI, String resourceNamespace, String resourceLabel,
			RuleSeverity severity, boolean normative, String packageHierarchy, Map<String, String> placeholderValues,
			String plantUML, List<RuleViolation> shadowExtensionViolations) {
		this.ruleId = ruleId;
		this.compositeRuleId = compositeRuleId;
		this.compositeSubRuleId = compositeSubRuleId;
		this.type = type;
		this.category = category;
		this.errorMsg = errorMsg;
		this.resourceURI = resourceURI;
		this.resourceNamespace = resourceNamespace;
		this.resourceLabel = (resourceLabel != null ? resourceLabel : "<Unknown>");
		this.severity = severity;
		this.normative = normative;
		this.packageHierarchy = packageHierarchy;
		this.placeholderValues = (placeholderValues != null ? placeholderValues : new HashMap<>());
		this.plantUML = plantUML;
		this.shadowExtensionViolations = shadowExtensionViolations;
	}

	public boolean isCompositeRule() {
		return compositeRuleId != null && !compositeRuleId.trim().isBlank() && compositeSubRuleId != null
				&& !compositeSubRuleId.trim().isBlank();
	}

	public String getRuleId() {
		return ruleId;
	}

	public String getCompositeRuleId() {
		return compositeRuleId;
	}

	public String getCompositeSubRuleId() {
		return compositeSubRuleId;
	}

	public RuleType getType() {
		return type;
	}

	public RuleCategory getCategory() {
		return category;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public String getResourceURI() {
		return resourceURI;
	}

	public String getResourceLabel() {
		return resourceLabel;
	}

	public RuleSeverity getSeverity() {
		return severity;
	}

	public boolean isNormative() {
		return normative;
	}

	public String getPackageHierarchy() {
		return packageHierarchy;
	}

	public boolean isInTopLevelGridPackage() {
		if (isNormative() && (packageHierarchy != null && packageHierarchy.trim().length() > 0)) {
			return (packageHierarchy.toLowerCase().contains("tc57cim::iec61970::")
					|| packageHierarchy.toLowerCase().contains("cim::grid::"));
		}
		return false;
	}

	public boolean isInTopLevelEnterprisePackage() {
		if (isNormative() && (packageHierarchy != null && packageHierarchy.trim().length() > 0)) {
			return (packageHierarchy.toLowerCase().contains("tc57cim::iec61968::")
					|| packageHierarchy.toLowerCase().contains("cim::enterprise::"));
		}
		return false;
	}

	public boolean isInTopLevelMarketPackage() {
		if (isNormative() && (packageHierarchy != null && packageHierarchy.trim().length() > 0)) {
			return (packageHierarchy.toLowerCase().contains("tc57cim::iec62325::")
					|| packageHierarchy.toLowerCase().contains("cim::market::"));
		}
		return false;
	}

	public String getPlantUML() {
		return plantUML;
	}

	public List<RuleViolation> getShadowExtensionViolations() {
		return shadowExtensionViolations;
	}

	public Map<String, String> getPlaceholderValues() {
		return placeholderValues;
	}

	public String getPlaceholderValue(String placeholderKey) {
		if (placeholderValues.containsKey(placeholderKey))
			return placeholderValues.get(placeholderKey);
		return null;
	}

	@Override
	public int compareTo(RuleViolation other) {
		return this.ruleId.compareTo(other.ruleId);
	}

	@Override
	public String toString() {
		return String.format("[%s] %s: (%s) - %s", severity, ruleId, resourceLabel, errorMsg);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RuleViolation that = (RuleViolation) o;
		return normative == that.normative && //
				Objects.equals(ruleId, that.ruleId) && //
				type == that.type && //
				category == that.category && //
				Objects.equals(errorMsg, that.errorMsg) && //
				Objects.equals(resourceURI, that.resourceURI) && //
				Objects.equals(resourceLabel, that.resourceLabel) && //
				Objects.equals(packageHierarchy, that.packageHierarchy) && //
				severity == that.severity;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ruleId, type, category, errorMsg, resourceURI, resourceLabel, severity, normative,
				packageHierarchy);
	}

}
