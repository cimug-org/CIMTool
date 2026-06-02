/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.isEANativeType;

import au.com.langdale.easyrules.rules.common.DBBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.NamespaceResolver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule192:Rule055", description = "[Rule055] Extension attribute data types shall be one of the stereotyped CIM classes (i.e. shall not be one of the Enterprise Architect native data types).")
@RuleMetadata(compositeRule = "Rule192", compositeSubRule = "Rule055", type = RuleType.Extension, category = RuleCategory.Attribute, errorTemplate = "Extension {elementTypeLowerCase} {name} has a declared type of {declaredType} which is an Enterprise Architect native data type. The declared type must be one of the standard CIM data types: «Primitive», «CIMDatatype», «Compound», or «enumeration». From within Sparx EA you must reselect the desired declared type for the attribute.")
public class Rule192_Rule055_ExtensionAttributeDataTypeMustNotBeNativeEADataType extends DBBaseRule {

	public Rule192_Rule055_ExtensionAttributeDataTypeMustNotBeNativeEADataType(String baseURI,
			boolean selfHealingEnabled, NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isAttribute(resource) && !isNormative(resource) && rs.getInt(COL_Classifier) == 0) {
			return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(ResultSet rs, OntResource resource,
			Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		try {
			values.put("declaredType", applyAsciidocBoldStyling(rs.getString(COL_Type)));
		} catch (SQLException e) {
			values.put("declaredType", applyAsciidocBoldStyling("<Unrecognized>"));
		}
		return values;
	}

	protected String getRuleErrorMsg(ResultSet resultSet, OntResource resource, Map<String, List<String>> namesMap) {
		// Default error message...
		String eaDeclaredType = null;
		boolean isEANativeType = false;
		String template = "Extension {elementTypeLowerCase} {name} has an invalid or unrecognized declared type {declaredType}. The declared type must be one of the standard CIM data types: «Primitive», «CIMDatatype», «Compound», or «enumeration». From within Sparx EA you must reselect the desired declared type for the attribute.";

		try {
			eaDeclaredType = resultSet.getString(COL_Type);
			isEANativeType = isEANativeType(eaDeclaredType);
			if (isEANativeType) {
				template = getErrorTemplate();
			}
		} catch (SQLException e) {
		}

		if (isSelfHealingEnabled())
			template = template + getSelfHealingResultsMsg(resultSet, resource, namesMap);

		Map<String, String> placeholderValues = getPlaceholderValues(resultSet, resource, namesMap);
		if (template != null) {
			for (Map.Entry<String, String> entry : placeholderValues.entrySet()) {
				template = template.replace("{" + entry.getKey() + "}", entry.getValue());
			}
		}

		return template;
	}

	protected String getSelfHealingResultsMsg(ResultSet resultSet, OntResource resource,
			Map<String, List<String>> namesMap) {
		String eaDeclaredType = null;
		boolean isEANativeType = false;
		try {
			eaDeclaredType = resultSet.getString(COL_Type);
		} catch (SQLException e) {
		}

		isEANativeType = isEANativeType(eaDeclaredType);

		StringBuffer template = new StringBuffer();
		template.append("\n\nNOTE: Self healing on import was enabled. ");

		OntResource range = resource.getRange();
		if (!isEANativeType && range != null && range.getLabel().equals(eaDeclaredType)) {
			template.append(
					"CIMTool was able to remap orphaned declared type {declaredType} to CIM's {rangeStereotype} {rangeName}. Note that the declared type for the {elementTypeLowerCase} must still be corrected in the model.");
		} else if (!isEANativeType && range != null && !range.getLabel().equals(eaDeclaredType)) {
			template.append(
					"CIMTool was able to map declared type {declaredType} to CIM's {rangeStereotype} {rangeName}. Note that the declared type for the {elementTypeLowerCase} must still be corrected in the model.");
		} else if (isEANativeType && range != null) {
			template.append(
					"CIMTool was able to map EA native type {declaredType} to CIM's {rangeStereotype} {rangeName}. Note that the declared type for the {elementTypeLowerCase} must still be corrected in the model.");
		} else if (isEANativeType && range == null) {
			template.append(
					"However, CIMTool was unable to map EA native type {declaredType} to an equivalent CIM data type. Note that the declared type for the {elementTypeLowerCase} must still be corrected in the model.");
		} else if (!isEANativeType && range == null) {
			template.append(
					"However, CIMTool was unable to map declared type {declaredType} to an equivalent CIM data type. Note that the declared type for the {elementTypeLowerCase} must still be corrected in the model.");
		}

		return template.toString();
	}

}