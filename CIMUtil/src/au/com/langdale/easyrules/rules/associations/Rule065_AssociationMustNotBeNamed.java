/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.isSourceSide;

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

@Rule(name = "Rule065", description = "Associations shall not have names.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "An {elementTypeLowerCase} between classes {domainName} and {rangeName} currently is named {associationName}. This is not allowed in the CIM and should be corrected in the model.")
public class Rule065_AssociationMustNotBeNamed extends DBBaseRule {

	public Rule065_AssociationMustNotBeNamed(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isAssociation(resource) && isNormative(resource)
				&& (rs.getString(COL_Name) != null && !rs.getString(COL_Name).isBlank() && isSourceSide(resource))) {
			return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(ResultSet rs, OntResource resource,
			Map<String, List<String>> namesMap) {
		Map<String, String> placeholderValues = super.getPlaceholderValues(resource, namesMap);
		try {
			placeholderValues.put("associationName",
					applyAsciidocStyling((rs.getString(COL_Name) != null && !rs.getString(COL_Name).isBlank()
							? rs.getString(COL_Name)
							: "<Unknown>")));
		} catch (SQLException e) {
			placeholderValues.put("associationName", applyAsciidocStyling("<Unknown>"));
		}
		return placeholderValues;
	}

}