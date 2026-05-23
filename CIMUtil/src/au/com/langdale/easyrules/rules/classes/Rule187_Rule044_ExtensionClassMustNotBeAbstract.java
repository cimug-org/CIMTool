/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.classes;

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

@Rule(name = "Rule187:Rule044", description = "[Rule044] A CIM class shall not be an abstract class.")
@RuleMetadata(compositeRule = "Rule187", compositeSubRule = "Rule044", type = RuleType.Extension, category = RuleCategory.Class, errorTemplate = "Extension {elementTypeLowerCase} {name} is specified as an abstract class which is not allowed in the CIM.")
public class Rule187_Rule044_ExtensionClassMustNotBeAbstract extends DBBaseRule {

	public Rule187_Rule044_ExtensionClassMustNotBeAbstract(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isCIMUmlClass(resource) && !isNormative(resource) && (rs.getInt(COL_Abstract) == 1)) {
			return true;
		}
		return false;
	}

}