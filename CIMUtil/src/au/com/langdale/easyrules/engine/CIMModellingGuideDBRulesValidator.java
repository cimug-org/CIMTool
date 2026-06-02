/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.engine;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.easyrules.rules.associations.Rule065_AssociationMustNotBeNamed;
import au.com.langdale.easyrules.rules.associations.Rule066_AssociationMustNotHaveDescription;
import au.com.langdale.easyrules.rules.associations.Rule073_AssociationMustImplicitlyBeBiDirectional;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule065_ExtensionAssociationMustNotBeNamed;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule066_ExtensionAssociationMustNotHaveDescription;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule073_ExtensionAssociationMustImplicitlyBeBiDirectional;
import au.com.langdale.easyrules.rules.attributes.Rule055_AttributeDataTypeMustNotBeNativeEADataType;
import au.com.langdale.easyrules.rules.attributes.Rule057_AttributeInitialValuesMustBeStaticAndConstant;
import au.com.langdale.easyrules.rules.attributes.Rule061_AttributeScopeMustBePublic;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule055_ExtensionAttributeDataTypeMustNotBeNativeEADataType;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule057_ExtensionAttributeInitialValuesMustBeStaticAndConstant;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule061_ExtensionAttributeScopeMustBePublic;
import au.com.langdale.easyrules.rules.classes.Rule044_ClassMustNotBeAbstract;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule044_ExtensionClassMustNotBeAbstract;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule084_EnumerationMustBeUMLClass;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule092_EnumerationLiteralScopeMustBePublic;
import au.com.langdale.easyrules.rules.enumerations.Rule084_EnumerationMustBeUMLClass;
import au.com.langdale.easyrules.rules.enumerations.Rule092_EnumerationLiteralScopeMustBePublic;
import au.com.langdale.easyrules.rules.packages.Rule027_PackageNameMustBeUnique;
import au.com.langdale.easyrules.rules.packages.Rule035_PackageInformativePackagesShouldBePrivate;
import au.com.langdale.easyrules.rules.packages.Rule036_PackageDocPackagesShouldBePrivate;
import au.com.langdale.easyrules.rules.packages.Rule037_PackageDetailedDiagramPackagesShouldBePrivate;
import au.com.langdale.easyrules.rules.packages.Rule176_ExtensionPackageNameMustBeUnique;
import au.com.langdale.easyrules.rules.packages.Rule177_ExtensionPackageNameMustBeUnique;
import au.com.langdale.easyrules.rules.packages.Rule181_Rule037_ExtensionPackageDetailedDiagramPackagesShouldBePrivate;
import au.com.langdale.easyrules.rules.packages.Rule182_Rule035_ExtensionPackageInformativePackagesShouldBePrivate;
import au.com.langdale.easyrules.rules.packages.Rule182_Rule036_ExtensionPackageDocPackagesShouldBePrivate;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.NamespaceResolver;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;

public class CIMModellingGuideDBRulesValidator implements DBModelRulesValidator {

	private static final String FACT_RESULT_SET = "rs";
	private static final String FACT_RESOURCE = "resource";
	private static final String FACT_NAMES_MAP = "namesMap";
	private static final String FACT_VIOLATIONS = "violations";
	//
	private Rules packageRules;
	private RulesEngine packageRulesEngine;
	//
	private Rules classAndEnumerationRules;
	private RulesEngine classAndEnumerationRulesEngine;
	//
	private Rules attributeAndEnumLiteralRules;
	private RulesEngine attributeAndEnumLiteralRulesEngine;
	//
	private Rules associationRules;
	private RulesEngine associationRulesEngine;

	public CIMModellingGuideDBRulesValidator(String baseURI, NamespaceResolver namespaceResolver,
			boolean selfHealingEnabled) {
		// Package Rules
		this.packageRules = new Rules();
		this.packageRulesEngine = new DefaultRulesEngine();
		this.packageRules.register(new Rule027_PackageNameMustBeUnique(baseURI, selfHealingEnabled, namespaceResolver));
		this.packageRules.register(
				new Rule035_PackageInformativePackagesShouldBePrivate(baseURI, selfHealingEnabled, namespaceResolver));
		this.packageRules.register(new Rule182_Rule035_ExtensionPackageInformativePackagesShouldBePrivate(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.packageRules.register(
				new Rule036_PackageDocPackagesShouldBePrivate(baseURI, selfHealingEnabled, namespaceResolver));
		this.packageRules.register(new Rule182_Rule036_ExtensionPackageDocPackagesShouldBePrivate(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.packageRules.register(new Rule037_PackageDetailedDiagramPackagesShouldBePrivate(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.packageRules.register(new Rule181_Rule037_ExtensionPackageDetailedDiagramPackagesShouldBePrivate(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.packageRules
				.register(new Rule176_ExtensionPackageNameMustBeUnique(baseURI, selfHealingEnabled, namespaceResolver));
		this.packageRules
				.register(new Rule177_ExtensionPackageNameMustBeUnique(baseURI, selfHealingEnabled, namespaceResolver));

		// Class Rules
		this.classAndEnumerationRules = new Rules();
		this.classAndEnumerationRulesEngine = new DefaultRulesEngine();
		this.classAndEnumerationRules
				.register(new Rule044_ClassMustNotBeAbstract(baseURI, selfHealingEnabled, namespaceResolver));
		this.classAndEnumerationRules.register(
				new Rule187_Rule044_ExtensionClassMustNotBeAbstract(baseURI, selfHealingEnabled, namespaceResolver));
		this.classAndEnumerationRules
				.register(new Rule084_EnumerationMustBeUMLClass(baseURI, selfHealingEnabled, namespaceResolver));
		this.classAndEnumerationRules.register(
				new Extension_Rule084_EnumerationMustBeUMLClass(baseURI, selfHealingEnabled, namespaceResolver));

		// Attribute Rules
		this.attributeAndEnumLiteralRules = new Rules();
		this.attributeAndEnumLiteralRulesEngine = new DefaultRulesEngine();
		this.attributeAndEnumLiteralRules.register(
				new Rule055_AttributeDataTypeMustNotBeNativeEADataType(baseURI, selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules
				.register(new Rule192_Rule055_ExtensionAttributeDataTypeMustNotBeNativeEADataType(baseURI,
						selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules.register(new Rule057_AttributeInitialValuesMustBeStaticAndConstant(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules
				.register(new Rule192_Rule057_ExtensionAttributeInitialValuesMustBeStaticAndConstant(baseURI,
						selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules
				.register(new Rule061_AttributeScopeMustBePublic(baseURI, selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules.register(new Rule192_Rule061_ExtensionAttributeScopeMustBePublic(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules.register(
				new Rule092_EnumerationLiteralScopeMustBePublic(baseURI, selfHealingEnabled, namespaceResolver));
		this.attributeAndEnumLiteralRules.register(new Extension_Rule092_EnumerationLiteralScopeMustBePublic(
				baseURI, selfHealingEnabled, namespaceResolver));

		// Association Rules
		this.associationRules = new Rules();
		this.associationRulesEngine = new DefaultRulesEngine();
		this.associationRules
				.register(new Rule065_AssociationMustNotBeNamed(baseURI, selfHealingEnabled, namespaceResolver));
		this.associationRules.register(
				new Rule196_Rule065_ExtensionAssociationMustNotBeNamed(baseURI, selfHealingEnabled, namespaceResolver));
		this.associationRules.register(
				new Rule066_AssociationMustNotHaveDescription(baseURI, selfHealingEnabled, namespaceResolver));
		this.associationRules.register(new Rule196_Rule066_ExtensionAssociationMustNotHaveDescription(baseURI,
				selfHealingEnabled, namespaceResolver));
		this.associationRules.register(
				new Rule073_AssociationMustImplicitlyBeBiDirectional(baseURI, selfHealingEnabled, namespaceResolver));
		this.associationRules.register(new Rule196_Rule073_ExtensionAssociationMustImplicitlyBeBiDirectional(baseURI,
				selfHealingEnabled, namespaceResolver));
	}

	@Override
	public void validatePackage(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations) {
		Facts facts = new Facts();
		facts.put(FACT_RESULT_SET, rs);
		facts.put(FACT_RESOURCE, resource);
		facts.put(FACT_NAMES_MAP, namesMap);
		facts.put(FACT_VIOLATIONS, violations);
		this.packageRulesEngine.fire(this.packageRules, facts);
	}

	@Override
	public void validateClass(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations) {
		Facts facts = new Facts();
		facts.put(FACT_RESULT_SET, rs);
		facts.put(FACT_RESOURCE, resource);
		facts.put(FACT_NAMES_MAP, namesMap);
		facts.put(FACT_VIOLATIONS, violations);
		this.classAndEnumerationRulesEngine.fire(this.classAndEnumerationRules, facts);
	}

	@Override
	public void validateAttribute(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations) {
		Facts facts = new Facts();
		facts.put(FACT_RESULT_SET, rs);
		facts.put(FACT_RESOURCE, resource);
		facts.put(FACT_NAMES_MAP, namesMap);
		facts.put(FACT_VIOLATIONS, violations);
		this.attributeAndEnumLiteralRulesEngine.fire(this.attributeAndEnumLiteralRules, facts);
	}

	@Override
	public void validateAssociation(ResultSet rs, OntResource resource, Map<String, List<String>> namesMap,
			List<RuleViolation> violations) {
		Facts facts = new Facts();
		facts.put(FACT_RESULT_SET, rs);
		facts.put(FACT_RESOURCE, resource);
		facts.put(FACT_NAMES_MAP, namesMap);
		facts.put(FACT_VIOLATIONS, violations);
		this.associationRulesEngine.fire(this.associationRules, facts);
	}

}
