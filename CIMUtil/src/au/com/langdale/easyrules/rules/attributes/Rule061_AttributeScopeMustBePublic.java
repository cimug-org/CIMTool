/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.attributes;

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

@Rule(name = "Rule061", description = "The scope of a CIM attribute shall be 'Public'.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, errorTemplate = "{elementType} {name} has scope '{scope}' but must be 'Public'. The model should be corrected.")
public class Rule061_AttributeScopeMustBePublic extends DBBaseRule {

	public Rule061_AttributeScopeMustBePublic(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isAttribute(resource) && isNormative(resource) && !"Public".equals(rs.getString(COL_Scope))) {
			return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(ResultSet rs, OntResource resource,
			Map<String, List<String>> namesMap) {
		Map<String, String> placeholderValues = super.getPlaceholderValues(resource, namesMap);
		try {
			placeholderValues.put(scope,
					(rs.getString(COL_Scope) != null && !rs.getString(COL_Scope).isBlank() ? rs.getString(COL_Scope)
							: "<Unknown>"));
		} catch (SQLException e) {
			placeholderValues.put(scope, "<Unknown>");
		}
		return placeholderValues;
	}

}