/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.packages;

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

@Rule(name = "Rule035", description = "Informative packages should be specified as 'private' packages in Enterprise Architect package properties, so they can be filtered out of diagrams showing the sub-packages.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Package, errorTemplate = "Informative package {name} should be specified as 'private' within the Enterprise Architect package properties. This is necessary so it can be filtered out of diagrams showing the sub-packages.")
public class Rule035_PackageInformativePackagesShouldBePrivate extends DBBaseRule {

	public Rule035_PackageInformativePackagesShouldBePrivate(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isPackage(resource) && isNormative(resource) && CIMRuleUtils.isInformative(resource)
				&& !"Private".equals(rs.getString(COL_Scope))) {
			return true;
		}
		return false;
	}

}