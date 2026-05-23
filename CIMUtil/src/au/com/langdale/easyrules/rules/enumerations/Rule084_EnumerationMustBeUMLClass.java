/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.enumerations;

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

@Rule(name = "Rule084", description = "A CIM enumeration shall be a UML class with the «enumeration» stereotype.")
@RuleMetadata(type = RuleType.Normative, category = RuleCategory.Enumeration, errorTemplate = "CIM {elementTypeLowerCase} {name} is defined in the model to be of UML type *'Enumeration'*. It is required that it be defined as a UML 'Class' with an «enumeration» stereotype. This should be corrected in the model.")
public class Rule084_EnumerationMustBeUMLClass extends DBBaseRule {

	private static final String ENUMERATION = "Enumeration";

	public Rule084_EnumerationMustBeUMLClass(String baseURI, boolean selfHealingEnabled,
			NamespaceResolver namespaceResolver) {
		super(baseURI, selfHealingEnabled, namespaceResolver);
	}

	public boolean doWhen(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap) throws SQLException {
		if (isEnumeration(resource) && isNormative(resource) && ENUMERATION.equals(rs.getString(COL_Object_Type))) {
			return true;
		}
		return false;
	}

	protected String getSelfHealingResultsMsg(ResultSet resultSet, OntResource resource,
			Map<String, List<String>> namesMap) {
		StringBuffer template = new StringBuffer();
		//
		template.append(CRLF);
		template.append(CRLF);
		template.append("[NOTE]").append(CRLF);
		template.append("====").append(CRLF);
		template.append(
				"Self healing on import was enabled. CIMTool successfully converted the {elementTypeLowerCase} to a class in the ontology for the purposes of profiling. However, the *'Type'* property of the {name} {elementTypeLowerCase} should should be corrected and set to *'Class'* in the model.")
				.append(CRLF);
		template.append(CRLF);
		template.append("image::Rule084-UMLClass.png[]").append(CRLF);
		template.append("====").append(CRLF);
		//
		return template.toString();
	}

}