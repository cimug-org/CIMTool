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

@Rule(name = "Rule196:Rule073", description = "[Rule073] Associations shall implicitly be bi-directional (i.e. the 'Navigability' property of the association ends is unspecified).")
@RuleMetadata(compositeRule = "Rule196", compositeSubRule = "Rule073", type = RuleType.Extension, category = RuleCategory.Association, errorTemplate = "Extension {elementTypeLowerCase} {name} must have the *'Navigability'* property of the association ends set as *'Unspecified'*. This should be changed in the model.")
public class Rule196_Rule073_ExtensionAssociationMustImplicitlyBeBiDirectional extends DBBaseRule {

	public Rule196_Rule073_ExtensionAssociationMustImplicitlyBeBiDirectional(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isAssociation(resource) && !isNormative(resource) && !"Unspecified".equals(rs.getString(COL_Direction))) {
			if (("Source -> Destination".equals(rs.getString(COL_Direction)) && CIMRuleUtils.isSourceSide(resource))
					|| ("Destination -> Source".equals(rs.getString(COL_Direction))
							&& CIMRuleUtils.isTargetSide(resource))) {
				return true;
			}
		}
		return false;
	}
	
}