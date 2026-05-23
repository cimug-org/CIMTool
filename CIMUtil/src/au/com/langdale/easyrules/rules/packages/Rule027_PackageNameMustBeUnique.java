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

import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule027", description = "All packages containing classes shall have unique names (i.e., the package containment hierarchy shall not be used in uniquely identifying a package).")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Package, errorTemplate = "{elementType} named {name} defined in package {packageHierarchy} is not globally unique. Duplicate(s) detected:\n {fullyQualifiedPackages}")
public class Rule027_PackageNameMustBeUnique extends DBBaseRule {

	public Rule027_PackageNameMustBeUnique(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {

		if (isPackage(resource) && isNormative(resource) && CIMRuleUtils.hasLabel(resource)) {
			String packageName = resource.getLabel();
			packageName = (packageName.startsWith("Package_") ? packageName.substring(packageName.indexOf("_") + 1)
					: packageName);
			if (!"DetailedDiagrams".equals(packageName) && namesMap.containsKey(packageName)) {
				List<String> packages = namesMap.get(packageName);
				if (packages.size() > 1) {
					return true; // duplicate package name
				}
			}
			/**
			 * ResIterator it =
			 * resource.getOntModel().listSubjectsWithProperty(RDFS.isDefinedBy, resource);
			 * while (it.hasNext()) { OntResource possibleClass = it.nextResource(); if
			 * (!possibleClass.hasProperty(RDF.type, UML.Package)) { String packageName =
			 * resource.getLabel(); packageName = (packageName.startsWith("Package_") ?
			 * packageName.substring(packageName.indexOf("_") + 1) : packageName); if
			 * (!"DetailedDiagrams".equals(packageName) &&
			 * namesMap.containsKey(packageName)) { List<String> packages =
			 * namesMap.get(packageName); if (packages.size() > 1) { return true; //
			 * duplicate package name } } } }
			 */
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
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