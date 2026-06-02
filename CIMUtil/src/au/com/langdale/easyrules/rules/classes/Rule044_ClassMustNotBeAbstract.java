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

@Rule(name = "Rule044", description = "A CIM UML class shall not be an abstract class.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Class, errorTemplate = "{elementType} {name} is specified as an abstract class which is not allowed in the CIM.")
public class Rule044_ClassMustNotBeAbstract extends DBBaseRule {

	public Rule044_ClassMustNotBeAbstract(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isCIMUmlClass(resource) && isNormative(resource) && (rs.getInt(COL_Abstract) == 1)) {
			return true;
		}
		return false;
	}

}