/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.associations;

import au.com.langdale.easyrules.rules.common.DBBaseRule;
import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.NamespaceResolver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule196:Rule066", description = "Extension associations shall not have descriptions.")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule066", type = RuleType.Normative, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {name} has a description. This is not allowed in the CIM and should be removed from the model.")
public class Rule196_Rule066_ExtensionAssociationMustNotHaveDescription extends DBBaseRule {

	public Rule196_Rule066_ExtensionAssociationMustNotHaveDescription(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isAssociation(resource) && !isNormative(resource)
				&& (rs.getString(COL_Notes) != null && !rs.getString(COL_Notes).isBlank())
				&& CIMRuleUtils.isSourceSide(resource)) {
			return true;
		}
		return false;
	}

}