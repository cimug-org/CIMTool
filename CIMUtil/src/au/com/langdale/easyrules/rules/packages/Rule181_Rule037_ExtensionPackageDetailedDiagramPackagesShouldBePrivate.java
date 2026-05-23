/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.packages;

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

@Rule(name = "Rule181:Rule037", description = "[Rule037] DetailedDiagram extensions packages should be specified as 'private' packages in Enterprise Architect package properties, so they can be filtered out of diagrams showing the sub-packages.")
@RuleMetadata(compositeRule = "Rule181", compositeSubRule = "Rule037", type = RuleType.Extension, category = RuleCategory.Package, errorTemplate = "Diagrams extension {elementTypeLowerCase} {name} located in package {packageHierarchy} should be specified as 'private' in the Enterprise Architect package properties. This is necessary so it can be filtered out of diagrams showing the sub-packages.")
public class Rule181_Rule037_ExtensionPackageDetailedDiagramPackagesShouldBePrivate extends DBBaseRule {

	public Rule181_Rule037_ExtensionPackageDetailedDiagramPackagesShouldBePrivate(String baseURI,
			boolean selfHealingEnabled, NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isPackage(resource) && !isNormative(resource) && resource.getLabel().startsWith("DetailedDiagram")
				&& !"Private".equals(rs.getString(COL_Scope))) {
			return true;
		}
		return false;
	}

}