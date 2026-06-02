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
import au.com.langdale.xmi.UML;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Rule;

@Rule(name = "Rule057", description = "In instances where an attribute of a CIM class has an initial value, it shall be set as both static and constant.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Attribute, errorTemplate = "{elementType} {name} has an initial value {initialValue} declared but which is not defined as both *static* and *constant* in the model.")
public class Rule057_AttributeInitialValuesMustBeStaticAndConstant extends DBBaseRule {

	public Rule057_AttributeInitialValuesMustBeStaticAndConstant(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (resource.hasProperty(UML.hasInitialValue) && isAttribute(resource) && isNormative(resource)
				&& (rs.getInt(COL_Const) != 1 || rs.getInt(COL_IsStatic) != 1)) {
			return true;
		}
		return false;
	}

	protected Map<String, String> getPlaceholderValues(ResultSet rs, OntResource resource,
			Map<String, List<String>> namesMap) {
		Map<String, String> values = super.getPlaceholderValues(resource, namesMap);
		values.put("initialValue", applyAsciidocBoldStyling(resource.getString(UML.hasInitialValue)));
		return values;
	}

}