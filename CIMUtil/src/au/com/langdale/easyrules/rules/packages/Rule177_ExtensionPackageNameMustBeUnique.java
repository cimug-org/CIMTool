/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.packages;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

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

import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule177", description = "In instances where an existing CIM package has the intended name of the CIM extension package, the intended name of the CIM extension package shall be changed.")
@RuleMetadata(type = RuleType.Extension, category = RuleCategory.Package, errorTemplate = "Extension {elementTypeLowerCase} named {name} defined in package {packageHierarchy} is a duplicate of a normative CIM package. Rename the package or consider adopting a naming prefix convention (e.g. ExtMyWires) when naming extension packages.")
public class Rule177_ExtensionPackageNameMustBeUnique extends DBBaseRule {

	public Rule177_ExtensionPackageNameMustBeUnique(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	@Condition
	protected boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap)
			throws SQLException {

		if (isPackage(resource) && !isNormative(resource) && CIMRuleUtils.hasLabel(resource)) {
			String packageName = resource.getLabel();
			packageName = (packageName.startsWith("Package_") ? packageName.substring(packageName.indexOf("_") + 1)
					: packageName);
			if (!"DetailedDiagrams".equals(packageName) && namesMap.containsKey(packageName)) {
				List<String> packages = namesMap.get(packageName);
				if (packages.size() > 1) {
					for (String fullyQualifiedPackage : packages) {
						if (isNormativePackage(fullyQualifiedPackage))
							return true; // duplicate package name
					}
				}
			}
		}
		return false;
	}

	private boolean isNormativePackage(String fullyQualifiedPackage) {
		if (fullyQualifiedPackage != null && !fullyQualifiedPackage.isBlank()) {
			boolean isInTopLevelGridPackage = (fullyQualifiedPackage.toLowerCase().contains("tc57cim::iec61970::")
					|| fullyQualifiedPackage.toLowerCase().contains("cim::grid::"));
			boolean isInTopLevelEnterprisePackage = (fullyQualifiedPackage.toLowerCase().contains("tc57cim::iec61968::")
					|| fullyQualifiedPackage.toLowerCase().contains("cim::enterprise::"));
			boolean isInTopLevelMarketPackage = (fullyQualifiedPackage.toLowerCase().contains("tc57cim::iec62325::")
					|| fullyQualifiedPackage.toLowerCase().contains("cim::market::"));
			return isInTopLevelGridPackage || isInTopLevelEnterprisePackage || isInTopLevelMarketPackage;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(rs, resource, namesMap);
		//
		String packageName = resource.getLabel();
		packageName = (packageName.startsWith("Package_") ? packageName.substring(packageName.indexOf("_") + 1)
				: packageName);
		String parentHierarchy = getPackageHierarchy(resource);
		String fullyQualifiedResourceName = parentHierarchy + (!parentHierarchy.isBlank() ? "::" : "") + packageName;
		//
		int duplicateFullyQualifiedPackageCount = 0;
		StringBuffer fullyQualifiedPackages = new StringBuffer();
		List<String> duplicates = namesMap.get(resource.getLabel());
		for (String fullyQualifiedPackageName : duplicates) {
			if (!fullyQualifiedPackageName.equals(fullyQualifiedResourceName)) {
				fullyQualifiedPackages.append("\n").append("- ")
						.append(applyAsciidocStyling(fullyQualifiedPackageName));
			} else {
				duplicateFullyQualifiedPackageCount++;
			}
		}
		if (duplicateFullyQualifiedPackageCount > 1) {
			fullyQualifiedPackages.append("\n").append("- ").append(applyAsciidocStyling(fullyQualifiedResourceName));
		}
		values.put("fullyQualifiedPackages", fullyQualifiedPackages.toString());
		//
		return values;
	}

}