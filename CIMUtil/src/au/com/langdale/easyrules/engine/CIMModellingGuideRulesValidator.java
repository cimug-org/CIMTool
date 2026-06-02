/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.engine;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;

import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.easyrules.rules.associations.AssociationMissingDomain;
import au.com.langdale.easyrules.rules.associations.AssociationMissingRange;
import au.com.langdale.easyrules.rules.associations.ExtensionAssociationMissingDomain;
import au.com.langdale.easyrules.rules.associations.ExtensionAssociationMissingRange;
import au.com.langdale.easyrules.rules.associations.Extension_Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName;
import au.com.langdale.easyrules.rules.associations.Rule067_AssociationMissingRole;
import au.com.langdale.easyrules.rules.associations.Rule068_AssociationRoleMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.associations.Rule069_AssociationRoleMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.associations.Rule070_AssociationRoleMissingDescription;
import au.com.langdale.easyrules.rules.associations.Rule071_AssociationRoleMissingMultiplicity;
import au.com.langdale.easyrules.rules.associations.Rule072_AssociationCompositionAssociationsNotAllowedRule;
import au.com.langdale.easyrules.rules.associations.Rule075_AssociationOnlyNonDatatypeClassesCanBeUsed;
import au.com.langdale.easyrules.rules.associations.Rule081_AssociationAvoidAggregationsWithEnterpriseClassesRule;
import au.com.langdale.easyrules.rules.associations.Rule083_AssociationInvalidSourceAndTargetRule;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule067_ExtensionAssociationMissingRole;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule068_ExtensionAssociationRoleMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule069_ExtensionAssociationRoleMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule070_ExtensionAssociationRoleMissingDescription;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule071_ExtensionAssociationRoleMissingMultiplicity;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule072_ExtensionAssociationCompositionAssociationsNotAllowedRule;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule075_ExtensionAssociationOnlyNonDatatypeClassesCanBeUsed;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule081_ExtensionAssociationAvoidAggregationsWithEnterpriseClassesRule;
import au.com.langdale.easyrules.rules.associations.Rule196_Rule083_ExtensionAssociationInvalidSourceAndTargetRule;
import au.com.langdale.easyrules.rules.associations.Rule197_AssociationRoleMustBeSingular;
import au.com.langdale.easyrules.rules.associations.Rule197_ExtensionAssociationRoleMustBeSingular;
import au.com.langdale.easyrules.rules.associations.Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName;
import au.com.langdale.easyrules.rules.attributes.Rule049_AttributeNameMustBeLowerCamelCase;
import au.com.langdale.easyrules.rules.attributes.Rule050_AttributeNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.attributes.Rule051_AttributeNameMustBeSingular;
import au.com.langdale.easyrules.rules.attributes.Rule052_AttributeCIMDatatypeValueMustBePrimitive;
import au.com.langdale.easyrules.rules.attributes.Rule053_AttributeCIMDatatypeUnitMustBeEnumeration;
import au.com.langdale.easyrules.rules.attributes.Rule054_AttributeCIMDatatypeMultiplierMustBeEnumeration;
import au.com.langdale.easyrules.rules.attributes.Rule058_AttributeMustHaveZeroToOneMultiplicity;
import au.com.langdale.easyrules.rules.attributes.Rule060_AttributeNameMustBeUnique;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule049_ExtensionAttributeNameMustBeLowerCamelCase;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule050_ExtensionAttributeNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule051_ExtensionAttributeNameMustBeSingular;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule052_ExtensionAttributeCIMDatatypeValueMustBePrimitive;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule053_ExtensionAttributeCIMDatatypeUnitMustBeEnumeration;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule054_ExtensionAttributeCIMDatatypeMultiplierMustBeEnumeration;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule058_ExtensionAttributeMustHaveZeroToOneMultiplicity;
import au.com.langdale.easyrules.rules.attributes.Rule192_Rule060_ExtensionAttributeNameMustBeUnique;
import au.com.langdale.easyrules.rules.attributes.Rule195_ExtensionUserDefinedCIMExtensionAttribute;
import au.com.langdale.easyrules.rules.classes.ClassUnusedRule;
import au.com.langdale.easyrules.rules.classes.ExtensionClassUnusedRule;
import au.com.langdale.easyrules.rules.classes.Rule038_ClassNameMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.classes.Rule039_ClassNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.classes.Rule040_ClassNameMustBeUnique;
import au.com.langdale.easyrules.rules.classes.Rule042_ClassNameMustBeSingular;
import au.com.langdale.easyrules.rules.classes.Rule043_ClassCIMDatatypeMustHaveStandardAttributes;
import au.com.langdale.easyrules.rules.classes.Rule046_ClassDatatypesMustNotHaveRelationships;
import au.com.langdale.easyrules.rules.classes.Rule047_ClassDatatypesMustOnlyBeUsedAsAttributes;
import au.com.langdale.easyrules.rules.classes.Rule185_ExtensionNewPrimitivesShouldBeAvoided;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule038_ExtensionClassNameMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule040_ExtensionClassNameMustBeUnique;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule042_ExtensionClassNameMustBeSingular;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships;
import au.com.langdale.easyrules.rules.classes.Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes;
import au.com.langdale.easyrules.rules.descriptions.Rule104_ElementMustHaveDescription;
import au.com.langdale.easyrules.rules.descriptions.Rule104_ExtensionElementMustHaveDescription;
import au.com.langdale.easyrules.rules.enumerations.EnumerationNameMustEndWithKind;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule085_EnumerationNameMustBeUnique;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule086_EnumerationLiteralNameMustBeLowerCamelCase;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule087_EnumerationLiteralNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule089_EnumerationLiteralNameMustBeUnique;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule090_EnumerationLiteralMustHaveAnEnumStereotype;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule091_EnumerationLiteralShallNotHaveADatatype;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule093_EnumerationLiteralShallNotHaveMultiplicity;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule095_EnumerationLiteralDuplicateInitialValue;
import au.com.langdale.easyrules.rules.enumerations.Extension_Rule096_EnumerationCodeUsageMustBeConsistent;
import au.com.langdale.easyrules.rules.enumerations.Rule085_EnumerationNameMustBeUnique;
import au.com.langdale.easyrules.rules.enumerations.Rule086_EnumerationLiteralNameMustBeLowerCamelCase;
import au.com.langdale.easyrules.rules.enumerations.Rule087_EnumerationLiteralNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.enumerations.Rule089_EnumerationLiteralNameMustBeUnique;
import au.com.langdale.easyrules.rules.enumerations.Rule090_EnumerationLiteralMustHaveAnEnumStereotype;
import au.com.langdale.easyrules.rules.enumerations.Rule091_EnumerationLiteralShallNotHaveADatatype;
import au.com.langdale.easyrules.rules.enumerations.Rule093_EnumerationLiteralShallNotHaveMultiplicity;
import au.com.langdale.easyrules.rules.enumerations.Rule095_EnumerationLiteralDuplicateInitialValue;
import au.com.langdale.easyrules.rules.enumerations.Rule096_EnumerationCodeUsageMustBeConsistent;
import au.com.langdale.easyrules.rules.extensions.ShadowClassMustBeInExtensionsPackage;
import au.com.langdale.easyrules.rules.extensions.ShadowClassShouldNotBeInNormativePackage;
import au.com.langdale.easyrules.rules.extensions.ShadowExtensionRule;
import au.com.langdale.easyrules.rules.inheritance.Rule115_ExtensionInheritanceMultipleInheritanceNotAllowed;
import au.com.langdale.easyrules.rules.inheritance.Rule115_InheritanceMultipleInheritanceNotAllowed;
import au.com.langdale.easyrules.rules.inheritance.Rule117_ExtensionInheritanceInvalidSourceAndTarget;
import au.com.langdale.easyrules.rules.inheritance.Rule117_InheritanceInvalidSourceAndTarget;
import au.com.langdale.easyrules.rules.inheritance.Rule119_ExtensionInheritanceNotAllowedForDatatypes;
import au.com.langdale.easyrules.rules.inheritance.Rule119_InheritanceNotAllowedForDatatypes;
import au.com.langdale.easyrules.rules.namespaces.MissingExtensionNamespaceRule;
import au.com.langdale.easyrules.rules.packages.Rule025_ExtensionPackageNameMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.packages.Rule025_PackageNameMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.packages.Rule026_ExtensionPackageNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.packages.Rule026_PackageNameMustBeBritishEnglish;
import au.com.langdale.easyrules.rules.packages.Rule178_ExtensionPackageNameMustBeUpperCamelCase;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.ShadowClassUtils;
import au.com.langdale.xmi.UML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.RuleListener;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;

public class CIMModellingGuideRulesValidator implements ModelRulesValidator {

	private static final String FACT_VIOLATIONS = "violations";
	private static final String FACT_RESOURCE = "resource";
	private static final String FACT_NAMES_MAP = "namesMap";

	private Rules packageRules;
	private DefaultRulesEngine packageRulesEngine;

	private Rules classRules;
	private DefaultRulesEngine classRulesEngine;

	private Rules attributeRules;
	private DefaultRulesEngine attributeRulesEngine;

	private Rules associationRules;
	private DefaultRulesEngine associationRulesEngine;

	private Rules enumerationRules;
	private DefaultRulesEngine enumerationRulesEngine;

	private Rules elementDescriptionRules;
	private DefaultRulesEngine elementDescriptionRulesEngine;

	private Rules inheritanceRules;
	private DefaultRulesEngine inheritanceRulesEngine;

	private Rules namespaceRules;
	private DefaultRulesEngine namespaceRulesEngine;

	private Rules extensionClassRules;
	private DefaultRulesEngine extensionRulesEngine;

	private Rules shadowClassExtensionRules;
	private DefaultRulesEngine shadowClassExtensionRulesEngine;

	private Rules allRules;
	private DefaultRulesEngine allRulesEngine;

	private String baseURI;
	
	private RuleListener ruleListener = new DefaultRuleListener(CIMModellingGuideRulesValidator.class.getName() + "-1");
	private RuleListener ruleListener2 = new DefaultRuleListener(CIMModellingGuideRulesValidator.class.getName() + "-2");

	public CIMModellingGuideRulesValidator(String baseURI) {

		this.baseURI = baseURI;

		// Section 5.3: Package Rules [Rule025 - Rule037]
		// Section 5.3.1: Package Naming Rules [Rule025 - Rule034]
		this.packageRules = new Rules();
		this.packageRulesEngine = new DefaultRulesEngine();
		this.packageRulesEngine.registerRuleListener(ruleListener);
		this.packageRules.register(new Rule025_PackageNameMustBeUpperCamelCase(baseURI));
		this.packageRules.register(new Rule025_ExtensionPackageNameMustBeUpperCamelCase(baseURI));
		this.packageRules.register(new Rule026_PackageNameMustBeBritishEnglish(baseURI));
		this.packageRules.register(new Rule026_ExtensionPackageNameMustBeBritishEnglish(baseURI));
		this.packageRules.register(new Rule178_ExtensionPackageNameMustBeUpperCamelCase(baseURI));

		// Section 5.3.2: Package Specification Rules [Rule035 - Rule037]
		// Rule035 - Rule 037 are validated as part of the
		// CIMModellingGuideRulesDBValidator

		// Section 5.4: Class Rules [Rule038 - Rule048]
		this.classRules = new Rules();
		this.classRulesEngine = new DefaultRulesEngine();
		this.classRulesEngine.registerRuleListener(ruleListener);
		this.classRules.register(new ClassUnusedRule(baseURI));
		this.classRules.register(new Rule038_ClassNameMustBeUpperCamelCase(baseURI));
		this.classRules.register(new Rule039_ClassNameMustBeBritishEnglish(baseURI));
		this.classRules.register(new Rule040_ClassNameMustBeUnique(baseURI));
		this.classRules.register(new Rule042_ClassNameMustBeSingular(baseURI));
		this.classRules.register(new Rule043_ClassCIMDatatypeMustHaveStandardAttributes(baseURI));
		this.classRules.register(new Rule046_ClassDatatypesMustNotHaveRelationships(baseURI));
		this.classRules.register(new Rule047_ClassDatatypesMustOnlyBeUsedAsAttributes(baseURI));
		// Note that Rule198 is an Association rule that is easiest to implement against class resources. 
		// Therefore it is executed by the classes rule engine but violations will be included in the
		// association rules section of the generated report...
		this.classRules.register(new Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName(baseURI));
		this.classRules.register(new ShadowClassMustBeInExtensionsPackage(baseURI));

		// Attribute Rules
		this.attributeRules = new Rules();
		this.attributeRulesEngine = new DefaultRulesEngine();
		this.attributeRulesEngine.registerRuleListener(ruleListener);
		this.attributeRules.register(new Rule049_AttributeNameMustBeLowerCamelCase(baseURI));
		this.attributeRules.register(new Rule192_Rule049_ExtensionAttributeNameMustBeLowerCamelCase(baseURI));
		this.attributeRules.register(new Rule050_AttributeNameMustBeBritishEnglish(baseURI));
		this.attributeRules.register(new Rule192_Rule050_ExtensionAttributeNameMustBeBritishEnglish(baseURI));
		this.attributeRules.register(new Rule051_AttributeNameMustBeSingular(baseURI));
		this.attributeRules.register(new Rule192_Rule051_ExtensionAttributeNameMustBeSingular(baseURI));
		this.attributeRules.register(new Rule052_AttributeCIMDatatypeValueMustBePrimitive(baseURI));
		this.attributeRules.register(new Rule192_Rule052_ExtensionAttributeCIMDatatypeValueMustBePrimitive(baseURI));
		this.attributeRules.register(new Rule053_AttributeCIMDatatypeUnitMustBeEnumeration(baseURI));
		this.attributeRules.register(new Rule192_Rule053_ExtensionAttributeCIMDatatypeUnitMustBeEnumeration(baseURI));
		this.attributeRules.register(new Rule054_AttributeCIMDatatypeMultiplierMustBeEnumeration(baseURI));
		this.attributeRules
				.register(new Rule192_Rule054_ExtensionAttributeCIMDatatypeMultiplierMustBeEnumeration(baseURI));
		this.attributeRules.register(new Rule058_AttributeMustHaveZeroToOneMultiplicity(baseURI));
		this.attributeRules.register(new Rule192_Rule058_ExtensionAttributeMustHaveZeroToOneMultiplicity(baseURI));
		this.attributeRules.register(new Rule060_AttributeNameMustBeUnique(baseURI));
		this.attributeRules.register(new Rule192_Rule060_ExtensionAttributeNameMustBeUnique(baseURI));
		// Temporarily commenting out Rule194 for further discussion if this is needed
		// any longer...
		// this.attributeRules.register(new
		// Rule194_ExtensionNoStereotypesOnAttributeOfUserDefinedExtensionClass(baseURI));
		this.attributeRules.register(new Rule195_ExtensionUserDefinedCIMExtensionAttribute(baseURI));

		// Association Rules
		this.associationRules = new Rules();
		this.associationRulesEngine = new DefaultRulesEngine();
		this.associationRulesEngine.registerRuleListener(ruleListener);
		this.associationRules.register(new AssociationMissingDomain(baseURI));
		this.associationRules.register(new AssociationMissingRange(baseURI));
		this.associationRules.register(new ExtensionAssociationMissingDomain(baseURI));
		this.associationRules.register(new ExtensionAssociationMissingRange(baseURI));
		this.associationRules.register(new Rule067_AssociationMissingRole(baseURI));
		this.associationRules.register(new Rule196_Rule067_ExtensionAssociationMissingRole(baseURI));
		this.associationRules.register(new Rule068_AssociationRoleMustBeUpperCamelCase(baseURI));
		this.associationRules.register(new Rule196_Rule068_ExtensionAssociationRoleMustBeUpperCamelCase(baseURI));
		this.associationRules.register(new Rule069_AssociationRoleMustBeBritishEnglish(baseURI));
		this.associationRules.register(new Rule196_Rule069_ExtensionAssociationRoleMustBeBritishEnglish(baseURI));
		this.associationRules.register(new Rule070_AssociationRoleMissingDescription(baseURI));
		this.associationRules.register(new Rule196_Rule070_ExtensionAssociationRoleMissingDescription(baseURI));
		this.associationRules.register(new Rule071_AssociationRoleMissingMultiplicity(baseURI));
		this.associationRules.register(new Rule196_Rule071_ExtensionAssociationRoleMissingMultiplicity(baseURI));
		this.associationRules.register(new Rule072_AssociationCompositionAssociationsNotAllowedRule(baseURI));
		this.associationRules
				.register(new Rule196_Rule072_ExtensionAssociationCompositionAssociationsNotAllowedRule(baseURI));
		this.associationRules.register(new Rule075_AssociationOnlyNonDatatypeClassesCanBeUsed(baseURI));
		this.associationRules
				.register(new Rule196_Rule075_ExtensionAssociationOnlyNonDatatypeClassesCanBeUsed(baseURI));
		this.associationRules.register(new Rule081_AssociationAvoidAggregationsWithEnterpriseClassesRule(baseURI));
		this.associationRules
				.register(new Rule196_Rule081_ExtensionAssociationAvoidAggregationsWithEnterpriseClassesRule(baseURI));
		this.associationRules.register(new Rule083_AssociationInvalidSourceAndTargetRule(baseURI));
		this.associationRules.register(new Rule196_Rule083_ExtensionAssociationInvalidSourceAndTargetRule(baseURI));
		this.associationRules.register(new Rule197_AssociationRoleMustBeSingular(baseURI));
		this.associationRules.register(new Rule197_ExtensionAssociationRoleMustBeSingular(baseURI));

		// Section 5.7: Enumeration Rules [Rule084 - Rule096]
		this.enumerationRules = new Rules();
		this.enumerationRulesEngine = new DefaultRulesEngine();
		this.enumerationRulesEngine.registerRuleListener(ruleListener);
		this.enumerationRules.register(new EnumerationNameMustEndWithKind(baseURI));
		this.enumerationRules.register(new Rule085_EnumerationNameMustBeUnique(baseURI));
		this.enumerationRules.register(new Extension_Rule085_EnumerationNameMustBeUnique(baseURI));
		this.enumerationRules.register(new Rule086_EnumerationLiteralNameMustBeLowerCamelCase(baseURI));
		this.enumerationRules.register(new Extension_Rule086_EnumerationLiteralNameMustBeLowerCamelCase(baseURI));
		this.enumerationRules.register(new Rule087_EnumerationLiteralNameMustBeBritishEnglish(baseURI));
		this.enumerationRules.register(new Extension_Rule087_EnumerationLiteralNameMustBeBritishEnglish(baseURI));
		this.enumerationRules.register(new Rule089_EnumerationLiteralNameMustBeUnique(baseURI));
		this.enumerationRules.register(new Extension_Rule089_EnumerationLiteralNameMustBeUnique(baseURI));
		this.enumerationRules.register(new Rule090_EnumerationLiteralMustHaveAnEnumStereotype(baseURI));
		this.enumerationRules.register(new Extension_Rule090_EnumerationLiteralMustHaveAnEnumStereotype(baseURI));
		this.enumerationRules.register(new Rule091_EnumerationLiteralShallNotHaveADatatype(baseURI));
		this.enumerationRules.register(new Extension_Rule091_EnumerationLiteralShallNotHaveADatatype(baseURI));
		this.enumerationRules.register(new Rule093_EnumerationLiteralShallNotHaveMultiplicity(baseURI));
		this.enumerationRules.register(new Extension_Rule093_EnumerationLiteralShallNotHaveMultiplicity(baseURI));
		this.enumerationRules.register(new Rule095_EnumerationLiteralDuplicateInitialValue(baseURI));
		this.enumerationRules.register(new Extension_Rule095_EnumerationLiteralDuplicateInitialValue(baseURI));
		this.enumerationRules.register(new Rule096_EnumerationCodeUsageMustBeConsistent(baseURI));
		this.enumerationRules.register(new Extension_Rule096_EnumerationCodeUsageMustBeConsistent(baseURI));

		// Section 5.9: Element Description Rules [Rule104 - Rule114]
		this.elementDescriptionRules = new Rules();
		this.elementDescriptionRulesEngine = new DefaultRulesEngine();
		this.elementDescriptionRulesEngine.registerRuleListener(ruleListener);
		this.elementDescriptionRules.register(new Rule104_ElementMustHaveDescription(baseURI));
		this.elementDescriptionRules.register(new Rule104_ExtensionElementMustHaveDescription(baseURI));

		// Section 5.10: Inheritance Rules [Rule115 - Rule119]
		// TODO: Implement Rule118
		this.inheritanceRules = new Rules();
		this.inheritanceRulesEngine = new DefaultRulesEngine();
		this.inheritanceRulesEngine.registerRuleListener(ruleListener);
		this.inheritanceRules.register(new Rule115_InheritanceMultipleInheritanceNotAllowed(baseURI));
		this.inheritanceRules.register(new Rule115_ExtensionInheritanceMultipleInheritanceNotAllowed(baseURI));
		this.inheritanceRules.register(new Rule117_InheritanceInvalidSourceAndTarget(baseURI));
		this.inheritanceRules.register(new Rule117_ExtensionInheritanceInvalidSourceAndTarget(baseURI));
		this.inheritanceRules.register(new Rule119_InheritanceNotAllowedForDatatypes(baseURI));
		this.inheritanceRules.register(new Rule119_ExtensionInheritanceNotAllowedForDatatypes(baseURI));

		// Section 5.12: Namespace Rules [Rule132 - Rule154]
		// Rule143 - Rule146 can not be implemented until support for
		// tagged values is added. We've commented them out for now.
		this.namespaceRules = new Rules();
		this.namespaceRulesEngine = new DefaultRulesEngine();
		this.namespaceRulesEngine.registerRuleListener(ruleListener);
		this.namespaceRules.register(new MissingExtensionNamespaceRule(baseURI));
		// this.namespaceRules.register(new
		// Rule143_MissingNamespaceSpecification(baseURI));
		// this.namespaceRules.register(new
		// Rule144_CIMRootPackageInvalidNamespaceURI(baseURI));
		// this.namespaceRules.register(new
		// Rule145_CIMRootPackageInvalidNamespacePrefix(baseURI));
		// this.namespaceRules.register(new
		// Rule146_MissingNamespaceSpecification(baseURI));

		// Section 6: CIM UML Extension Rules and Recommendations
		// Section 6.2: General Extension Rules [Rule163 - Rule170]
		// Section 6.3: Package Extension Rules [Rule171 - Rule183]
		// Section 6.4: Class Extension Rules [Rule184 - Rule191]
		// Section 6.5: Attribute Extension Rules [Rule192 - Rule195]
		// Section 6.6: Association Extension Rules [Rule196 - Rule204]
		// Section 6.7: Enumeration Extension Rules [Rule205 - Rule206]
		this.extensionClassRules = new Rules();
		this.extensionRulesEngine = new DefaultRulesEngine();
		this.extensionRulesEngine.registerRuleListener(ruleListener);
		this.extensionClassRules.register(new ShadowClassShouldNotBeInNormativePackage(baseURI));
		this.extensionClassRules.register(new ExtensionClassUnusedRule(baseURI));
		this.extensionClassRules.register(new Rule187_Rule038_ExtensionClassNameMustBeUpperCamelCase(baseURI));
		this.extensionClassRules.register(new Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish(baseURI));
		this.extensionClassRules.register(new Rule187_Rule040_ExtensionClassNameMustBeUnique(baseURI));
		this.extensionClassRules.register(new Rule187_Rule042_ExtensionClassNameMustBeSingular(baseURI));
		this.extensionClassRules
				.register(new Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes(baseURI));
		this.extensionClassRules.register(new Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships(baseURI));
		this.extensionClassRules
				.register(new Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes(baseURI));
		this.extensionClassRules.register(new Rule185_ExtensionNewPrimitivesShouldBeAvoided(baseURI));
		// Note that Rule198 is an Association rule that is easiest to implement against class resources. 
		// Therefore it is executed by the classes rule engine but violations will be included in the
		// association rules section of the generated report...
		this.extensionClassRules.register(new Extension_Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName(baseURI));


		// Shadow Extension Rules
		this.shadowClassExtensionRules = new Rules();
		this.shadowClassExtensionRulesEngine = new DefaultRulesEngine();
		this.shadowClassExtensionRulesEngine.registerRuleListener(ruleListener);
		this.shadowClassExtensionRules.register(new ShadowExtensionRule(baseURI));
		//
		//
		this.allRules = new Rules();
		this.allRulesEngine = new DefaultRulesEngine();
		this.allRulesEngine.registerRuleListener(ruleListener2);

		// Section 5.3: Package Rules [Rule025 - Rule037]
		// Section 5.3.1: Package Naming Rules [Rule025 - Rule034]
		this.allRules.register(new Rule025_PackageNameMustBeUpperCamelCase(baseURI));
		this.allRules.register(new Rule025_ExtensionPackageNameMustBeUpperCamelCase(baseURI));
		this.allRules.register(new Rule026_PackageNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule026_ExtensionPackageNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule178_ExtensionPackageNameMustBeUpperCamelCase(baseURI));

		// Section 5.3.2: Package Specification Rules [Rule035 - Rule037]
		// Rule035 - Rule 037 are validated as part of the
		// CIMModellingGuideRulesDBValidator

		// Section 5.4: Class Rules [Rule038 - Rule048]
		this.allRules.register(new ClassUnusedRule(baseURI));
		this.allRules.register(new Rule038_ClassNameMustBeUpperCamelCase(baseURI));
		this.allRules.register(new Rule039_ClassNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule040_ClassNameMustBeUnique(baseURI));
		this.allRules.register(new Rule042_ClassNameMustBeSingular(baseURI));
		this.allRules.register(new Rule043_ClassCIMDatatypeMustHaveStandardAttributes(baseURI));
		this.allRules.register(new Rule046_ClassDatatypesMustNotHaveRelationships(baseURI));
		this.allRules.register(new Rule047_ClassDatatypesMustOnlyBeUsedAsAttributes(baseURI));
		this.allRules.register(new ShadowClassMustBeInExtensionsPackage(baseURI));

		// Attribute Rules
		this.allRules.register(new Rule049_AttributeNameMustBeLowerCamelCase(baseURI));
		this.allRules.register(new Rule192_Rule049_ExtensionAttributeNameMustBeLowerCamelCase(baseURI));
		this.allRules.register(new Rule050_AttributeNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule192_Rule050_ExtensionAttributeNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule051_AttributeNameMustBeSingular(baseURI));
		this.allRules.register(new Rule192_Rule051_ExtensionAttributeNameMustBeSingular(baseURI));
		this.allRules.register(new Rule052_AttributeCIMDatatypeValueMustBePrimitive(baseURI));
		this.allRules.register(new Rule192_Rule052_ExtensionAttributeCIMDatatypeValueMustBePrimitive(baseURI));
		this.allRules.register(new Rule053_AttributeCIMDatatypeUnitMustBeEnumeration(baseURI));
		this.allRules.register(new Rule192_Rule053_ExtensionAttributeCIMDatatypeUnitMustBeEnumeration(baseURI));
		this.allRules.register(new Rule054_AttributeCIMDatatypeMultiplierMustBeEnumeration(baseURI));
		this.allRules.register(new Rule192_Rule054_ExtensionAttributeCIMDatatypeMultiplierMustBeEnumeration(baseURI));
		this.allRules.register(new Rule058_AttributeMustHaveZeroToOneMultiplicity(baseURI));
		this.allRules.register(new Rule192_Rule058_ExtensionAttributeMustHaveZeroToOneMultiplicity(baseURI));
		this.allRules.register(new Rule060_AttributeNameMustBeUnique(baseURI));
		this.allRules.register(new Rule192_Rule060_ExtensionAttributeNameMustBeUnique(baseURI));
		// Temporarily commenting out Rule194 for further discussion if this is needed
		// any longer...
		// this.allRules.register(new
		// Rule194_ExtensionNoStereotypesOnAttributeOfUserDefinedExtensionClass(baseURI));
		this.allRules.register(new Rule195_ExtensionUserDefinedCIMExtensionAttribute(baseURI));

		// Association Rules
		this.allRules.register(new AssociationMissingDomain(baseURI));
		this.allRules.register(new AssociationMissingRange(baseURI));
		this.allRules.register(new ExtensionAssociationMissingDomain(baseURI));
		this.allRules.register(new ExtensionAssociationMissingRange(baseURI));
		this.allRules.register(new Rule067_AssociationMissingRole(baseURI));
		this.allRules.register(new Rule196_Rule067_ExtensionAssociationMissingRole(baseURI));
		this.allRules.register(new Rule068_AssociationRoleMustBeUpperCamelCase(baseURI));
		this.allRules.register(new Rule196_Rule068_ExtensionAssociationRoleMustBeUpperCamelCase(baseURI));
		this.allRules.register(new Rule069_AssociationRoleMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule196_Rule069_ExtensionAssociationRoleMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule070_AssociationRoleMissingDescription(baseURI));
		this.allRules.register(new Rule196_Rule070_ExtensionAssociationRoleMissingDescription(baseURI));
		this.allRules.register(new Rule071_AssociationRoleMissingMultiplicity(baseURI));
		this.allRules.register(new Rule196_Rule071_ExtensionAssociationRoleMissingMultiplicity(baseURI));
		this.allRules.register(new Rule072_AssociationCompositionAssociationsNotAllowedRule(baseURI));
		this.allRules.register(new Rule196_Rule072_ExtensionAssociationCompositionAssociationsNotAllowedRule(baseURI));
		this.allRules.register(new Rule075_AssociationOnlyNonDatatypeClassesCanBeUsed(baseURI));
		this.allRules.register(new Rule196_Rule075_ExtensionAssociationOnlyNonDatatypeClassesCanBeUsed(baseURI));
		this.allRules.register(new Rule081_AssociationAvoidAggregationsWithEnterpriseClassesRule(baseURI));
		this.allRules
				.register(new Rule196_Rule081_ExtensionAssociationAvoidAggregationsWithEnterpriseClassesRule(baseURI));
		this.allRules.register(new Rule083_AssociationInvalidSourceAndTargetRule(baseURI));
		this.allRules.register(new Rule196_Rule083_ExtensionAssociationInvalidSourceAndTargetRule(baseURI));
		this.allRules.register(new Rule197_AssociationRoleMustBeSingular(baseURI));
		this.allRules.register(new Rule197_ExtensionAssociationRoleMustBeSingular(baseURI));
		// Note that Rule198 is an association rule that is easiest to implement against class resources. 
		// Therefore it is executed by the classes rule engine but violations will be included in the
		// association rules section of the generated report...
		this.classRules.register(new Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName(baseURI));
		this.classRules.register(new Extension_Rule198_AssociationMultipleAssociationsBetweenTwoClassesWithDuplicateName(baseURI));

		// Section 5.7: Enumeration Rules [Rule084 - Rule096]
		this.allRules.register(new EnumerationNameMustEndWithKind(baseURI));
		this.allRules.register(new Rule085_EnumerationNameMustBeUnique(baseURI));
		this.allRules.register(new Extension_Rule085_EnumerationNameMustBeUnique(baseURI));
		this.allRules.register(new Rule086_EnumerationLiteralNameMustBeLowerCamelCase(baseURI));
		this.allRules.register(new Extension_Rule086_EnumerationLiteralNameMustBeLowerCamelCase(baseURI));
		this.allRules.register(new Rule087_EnumerationLiteralNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Extension_Rule087_EnumerationLiteralNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule089_EnumerationLiteralNameMustBeUnique(baseURI));
		this.allRules.register(new Extension_Rule089_EnumerationLiteralNameMustBeUnique(baseURI));
		this.allRules.register(new Rule090_EnumerationLiteralMustHaveAnEnumStereotype(baseURI));
		this.allRules.register(new Extension_Rule090_EnumerationLiteralMustHaveAnEnumStereotype(baseURI));
		this.allRules.register(new Rule091_EnumerationLiteralShallNotHaveADatatype(baseURI));
		this.allRules.register(new Extension_Rule091_EnumerationLiteralShallNotHaveADatatype(baseURI));
		this.allRules.register(new Rule093_EnumerationLiteralShallNotHaveMultiplicity(baseURI));
		this.allRules.register(new Extension_Rule093_EnumerationLiteralShallNotHaveMultiplicity(baseURI));
		this.allRules.register(new Rule095_EnumerationLiteralDuplicateInitialValue(baseURI));
		this.allRules.register(new Extension_Rule095_EnumerationLiteralDuplicateInitialValue(baseURI));
		this.allRules.register(new Rule096_EnumerationCodeUsageMustBeConsistent(baseURI));
		this.allRules.register(new Extension_Rule096_EnumerationCodeUsageMustBeConsistent(baseURI));

		// Section 5.9: Element Description Rules [Rule104 - Rule114]
		this.allRules.register(new Rule104_ElementMustHaveDescription(baseURI));
		this.allRules.register(new Rule104_ExtensionElementMustHaveDescription(baseURI));

		// Section 5.10: Inheritance Rules [Rule115 - Rule119]
		// TODO: Implement Rule118
		this.allRules.register(new Rule115_InheritanceMultipleInheritanceNotAllowed(baseURI));
		this.allRules.register(new Rule115_ExtensionInheritanceMultipleInheritanceNotAllowed(baseURI));
		this.allRules.register(new Rule117_InheritanceInvalidSourceAndTarget(baseURI));
		this.allRules.register(new Rule117_ExtensionInheritanceInvalidSourceAndTarget(baseURI));
		this.allRules.register(new Rule119_InheritanceNotAllowedForDatatypes(baseURI));
		this.allRules.register(new Rule119_ExtensionInheritanceNotAllowedForDatatypes(baseURI));

		// Section 5.12: Namespace Rules [Rule132 - Rule154]
		// Rule143 - Rule146 can not be implemented until support for
		// tagged values is added. We've commented them out for now.
		this.allRules.register(new MissingExtensionNamespaceRule(baseURI));
		// this.allRules.register(new
		// Rule143_MissingNamespaceSpecification(baseURI));
		// this.allRules.register(new
		// Rule144_CIMRootPackageInvalidNamespaceURI(baseURI));
		// this.allRules.register(new
		// Rule145_CIMRootPackageInvalidNamespacePrefix(baseURI));
		// this.allRules.register(new
		// Rule146_MissingNamespaceSpecification(baseURI));

		// Section 6: CIM UML Extension Rules and Recommendations
		// Section 6.2: General Extension Rules [Rule163 - Rule170]
		// Section 6.3: Package Extension Rules [Rule171 - Rule183]
		// Section 6.4: Class Extension Rules [Rule184 - Rule191]
		// Section 6.5: Attribute Extension Rules [Rule192 - Rule195]
		// Section 6.6: Association Extension Rules [Rule196 - Rule204]
		// Section 6.7: Enumeration Extension Rules [Rule205 - Rule206]
		this.allRules.register(new ShadowClassShouldNotBeInNormativePackage(baseURI));
		this.allRules.register(new ExtensionClassUnusedRule(baseURI));
		this.allRules.register(new Rule187_Rule038_ExtensionClassNameMustBeUpperCamelCase(baseURI));
		this.allRules.register(new Rule187_Rule039_ExtensionClassNameMustBeBritishEnglish(baseURI));
		this.allRules.register(new Rule187_Rule040_ExtensionClassNameMustBeUnique(baseURI));
		this.allRules.register(new Rule187_Rule042_ExtensionClassNameMustBeSingular(baseURI));
		this.allRules.register(new Rule187_Rule043_ExtensionClassCIMDatatypeMustHaveStandardAttributes(baseURI));
		this.allRules.register(new Rule187_Rule046_ExtensionClassDatatypesMustNotHaveRelationships(baseURI));
		this.allRules.register(new Rule187_Rule047_ExtensionClassDatatypesMustOnlyBeUsedAsAttributes(baseURI));
		this.allRules.register(new Rule185_ExtensionNewPrimitivesShouldBeAvoided(baseURI));

		// Shadow Extension Rules
		this.allRules.register(new ShadowExtensionRule(baseURI));
	}

	/**
	 * Typically, we could simply pass the entire collection of all OntResources to
	 * a RulesEngine containing all rules. However, this is not an efficient
	 * implementation.
	 * 
	 * Therefore, in the implementation below we have "specialized" distinct engines
	 * to match categories of rules so that we can selectively pass just the
	 * matching OntResource(s) to be validated. This helps reduce the overhead by
	 * preventing all rules firing for each resource across all rules.
	 */
	@Override
	public List<RuleViolation> validate(OntModel model) {
		List<RuleViolation> violations = new ArrayList<>();
		List<RuleViolation> violations2 = new ArrayList<>();
		//
		// Pre-process ontological resources into sets targeted for specific
		// engines/rulesets
		//
		Map<String, List<String>> packageNamesMap = new HashMap<>();
		ResIterator it = model.listSubjectsWithProperty(RDF.type, UML.Package);
		while (it.hasNext()) {
			OntResource aPackage = it.nextResource();
			if (aPackage.getLabel() != null && !aPackage.getLabel().isBlank()) {
				String packageName = (aPackage.getLabel().startsWith("Package_")
						? aPackage.getLabel().substring(aPackage.getLabel().indexOf("_") + 1)
						: aPackage.getLabel());
				if (!packageName.equals("DetailedDiagram")) {
					StringBuffer theFullyQualifiedName = new StringBuffer();
					String parentHierarchy = getPackageHierarchy(aPackage);
					theFullyQualifiedName.append(parentHierarchy).append((!parentHierarchy.isBlank() ? "::" : ""))
							.append(packageName);
					List<String> list = (packageNamesMap.containsKey(packageName) ? packageNamesMap.get(packageName)
							: new ArrayList<>());
					list.add(theFullyQualifiedName.toString());
					packageNamesMap.put(packageName, list);
				}
			}
		}
		//
		Map<String, OntResource> shadowClasses = ShadowClassUtils.getShadowClasses(model, this.baseURI);
		//
		Map<String, List<String>> classNamesMap = new HashMap<>();
		List<OntResource> allClasses = new ArrayList<>();
		List<OntResource> normativeClasses = new ArrayList<>();
		List<OntResource> extensionClasses = new ArrayList<>();
		//
		List<OntResource> resources = new ArrayList<>();

		ResIterator types = model.listSubjectsWithProperty(RDF.type);
		while (types.hasNext()) {
			OntResource resource = types.nextResource();
			if (!resource.equals(UML.Stereotype) && !resource.hasProperty(RDF.type, UML.Package)) {
				if (resource.isClass() || resource.isDatatype() || resource.isProperty()
						|| resource.isFunctionalProperty() || resource.isInverseFunctionalProperty()) {
					// We first vet out the CIM UML classes and add to the names map...
					if ((resource.isClass() || resource.isDatatype()
							|| resource.hasProperty(UML.hasStereotype, UML.primitive)) && resource.getLabel() != null
							&& !resource.getLabel().isBlank()) {
						OntResource aClass = resource;
						StringBuffer theFullyQualifiedName = new StringBuffer();
						String parentHierarchy = getPackageHierarchy(aClass);
						theFullyQualifiedName.append(parentHierarchy).append((!parentHierarchy.isBlank() ? "::" : ""))
								.append(aClass.getLabel());
						List<String> list = (classNamesMap.containsKey(aClass.getLabel())
								? classNamesMap.get(aClass.getLabel())
								: new ArrayList<>());
						list.add(theFullyQualifiedName.toString());
						classNamesMap.put(aClass.getLabel(), list);
						
						allClasses.add(aClass);
						if (CIMRuleUtils.isInExtensionPackage(aClass)) {
							extensionClasses.add(aClass);
						} else {
							normativeClasses.add(aClass);
						}
					}
					resources.add(resource);
				}
			}
		}
		//
		// Section 5.3: Package Rules (validates both normative CIM and extension)
		ResIterator packages = model.listSubjectsWithProperty(RDF.type, UML.Package);
		validateResources(packages, packageRulesEngine, packageRules, packageNamesMap, violations);
		//
		// Section 5.4: Class Rules (normative CIM)
		// Section 6.4: Class Rules (CIM extensions)
		// Shadow Class Rules (shadow extension)
		validateResources(normativeClasses, classRulesEngine, classRules, classNamesMap, violations);
		validateResources(extensionClasses, extensionRulesEngine, extensionClassRules, classNamesMap, violations);
		validateResources(shadowClasses.values(), shadowClassExtensionRulesEngine, shadowClassExtensionRules,
				classNamesMap, violations);
		//
		// Section 5.5: Attribute Rules (normative CIM)
		// Section 6.5: Attribute Rules (CIM extensions)
		ResIterator attributes = model.listSubjectsWithProperty(UML.hasStereotype, UML.attribute);
		validateResources(attributes, attributeRulesEngine, attributeRules, Map.of(), violations);
		//
		// Section 5.6: Association Rules (normative CIM)
		// Section 6.6: Association Rules (CIM extensions)
		ResIterator associations = model.listSubjectsWithProperty(RDF.type, OWL2.ObjectProperty);
		validateResources(associations, associationRulesEngine, associationRules, Map.of(), violations);
		//
		// Section 5.7: Enumeration Rules (normative CIM)
		// Section 6.7: Enumeration Rules (CIM extensions)
		List<OntResource> enumResources = new ArrayList<>();
		ResIterator enums = model.listSubjectsWithProperty(UML.hasStereotype, UML.enumeration);
		enums.forEachRemaining(item -> {
			OntResource enumClass = (OntResource) item;
			enumResources.add(enumClass);
		});
		ResIterator enumLiterals = model.listSubjectsWithProperty(UML.hasStereotype, UML.enumliteral);
		enumLiterals.forEachRemaining(item -> {
			OntResource enumLiteral = (OntResource) item;
			enumResources.add(enumLiteral);
		});
		validateResources(enumResources, enumerationRulesEngine, enumerationRules, Map.of(), violations);
		//
		// Section 5.9: Element Description Rules (normative CIM & extensions)
		ResIterator allResources = model.listSubjectsWithProperty(RDF.type);
		validateResources(allResources, elementDescriptionRulesEngine, elementDescriptionRules, Map.of(), violations);
		//
		// Section 5.10: Inheritance Rules (normative CIM & extensions)
		validateResources(allClasses, inheritanceRulesEngine, inheritanceRules, Map.of(), violations);
		//
		// Section 5.12: Namespace Rules (normative CIM & extensions)
		validateResources(allClasses, namespaceRulesEngine, namespaceRules, Map.of(), violations);
		//
		validateResources(resources, allRulesEngine, allRules, classNamesMap, violations2);
		//
		System.out.println(ruleListener.toString());
		System.out.println(ruleListener2.toString());
		//
		return violations2;
	}

	private void validateResources(ResIterator it, RulesEngine rulesEngine, Rules rules,
			Map<String, List<String>> namesMap, List<RuleViolation> violations) {
		while (it.hasNext()) {
			OntResource resource = it.nextResource();
			Facts facts = new Facts();
			facts.put(FACT_RESOURCE, resource);
			facts.put(FACT_VIOLATIONS, violations);
			facts.put(FACT_NAMES_MAP, namesMap);
			rulesEngine.fire(rules, facts);
		}
	}

	private void validateResources(Collection<OntResource> resources, RulesEngine rulesEngine, Rules rules,
			Map<String, List<String>> namesMap, List<RuleViolation> violations) {
		for (OntResource resource : resources) {
			Facts facts = new Facts();
			facts.put(FACT_RESOURCE, resource);
			facts.put(FACT_VIOLATIONS, violations);
			facts.put(FACT_NAMES_MAP, namesMap);
			rulesEngine.fire(rules, facts);
		}
	}

}
