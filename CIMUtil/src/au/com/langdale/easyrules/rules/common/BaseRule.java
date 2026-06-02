/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.common;

import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.cardString;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getDependencyPriority;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getLabelDefaultUnknown;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getPackageHierarchy;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.getResourceType;
import static au.com.langdale.easyrules.rules.utils.CIMRuleUtils.isSourceSide;

import au.com.langdale.easyrules.rules.metadata.RuleCategory;
import au.com.langdale.easyrules.rules.metadata.RuleMetadata;
import au.com.langdale.easyrules.rules.metadata.RuleSeverity;
import au.com.langdale.easyrules.rules.metadata.RuleType;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.UML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jeasy.rules.annotation.Rule;

/**
 * Base class for rule classes to enable access to rule metadata like name
 * description.
 */
public abstract class BaseRule implements RulePlaceholderValuesSupport {

	protected static final String CRLF = System.lineSeparator();

	private String baseURI;

	public BaseRule(String baseURI) {
		this.baseURI = baseURI;
	}

	public String getBaseURI() {
		return baseURI;
	}

	protected boolean isNormative(OntResource resource) {
		return CIMRuleUtils.isNormative(resource, getBaseURI());
	}

	/**
	 * Convenience method that checks whether the resource passed in is a package.
	 * For the purposes of this method, a 'Package' is any resource having a an
	 * RDF.type property with a value of UML.Package.
	 * 
	 * @return True if the resource passed in is, by definition, a package; false
	 *         otherwise.
	 */
	protected boolean isPackage(OntResource resource) {
		return CIMRuleUtils.isPackage(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an
	 * enumeration. For the purposes of this method, a primitive class is any class
	 * defined in the CIM that has the «Primitive» stereotype assigned. Such classes
	 * in the ontology are also of RDF.type = RDFS.Datatype.
	 * 
	 * @param resource The resource for which to test whether or not it is a CIM
	 *                 primitive class.
	 * @return True if the resource passed in is, by definition, a CIM primitive
	 *         class; false otherwise.
	 */
	protected boolean isPrimitive(OntResource resource) {
		return CIMRuleUtils.isPrimitive(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is a
	 * CIMDatatype class. For the purposes of this method, a CIMDatatype class is
	 * any class defined in the CIM that has the «CIMDatatype» stereotype assigned.
	 * Such classes in the ontology are also of RDF.type = RDFS.Datatype.
	 * 
	 * @param resource The resource for which to test whether or not it is a
	 *                 CIMDatatype class.
	 * @return True if the resource passed in is, by definition, a CIMDatatype
	 *         class; false otherwise.
	 */
	protected boolean isCIMDatatype(OntResource resource) {
		return CIMRuleUtils.isCIMDatatype(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an
	 * enumeration. For the purposes of this method, a compound class is any class
	 * defined in the CIM that has the «Compound» stereotype assigned. Such classes
	 * in the ontology are also of RDF.type = RDFS.Class.
	 * 
	 * @param resource The resource for which to test whether or not it is a CIM
	 *                 compound class.
	 * @return True if the resource passed in is, by definition, a CIM compound
	 *         class; false otherwise.
	 */
	protected boolean isCompound(OntResource resource) {
		return CIMRuleUtils.isCompound(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an
	 * enumeration. For the purposes of this method, a 'Enumeration' is any class
	 * defined in the CIM that has a stereotype of «enumeration». Such classes in
	 * the ontology also are of RDF.type = RDFS.Class.
	 * 
	 * @param resource The resource for which to test whether or not it is a CIM
	 *                 enumeration.
	 * @return True if the resource passed in is, by definition, a CIM enumeration;
	 *         false otherwise.
	 */
	protected boolean isEnumeration(OntResource resource) {
		return CIMRuleUtils.isEnumeration(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an
	 * association. For the purposes of this method, a resource is an association if
	 * it is declared as an object property that also has has an inverseOf. Note
	 * that enumerations are also defined as object properties but since they are
	 * not associations (but rather purely attributes) they will never have the
	 * inverse, thus how associations are differentiated by this method.
	 * 
	 * @param resource The resource for which to test whether or not it is an
	 *                 association.
	 * @return True if the resource passed in is, by definition, an association;
	 *         false otherwise.
	 */
	protected boolean isAssociation(OntResource resource) {
		return CIMRuleUtils.isAssociation(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an attribute
	 * (in contrast to an association or an enum literal). For the purposes of this
	 * method, a resource is an attribute if it meets the following criteria:
	 * 
	 * <pre>
	 *  1. It has assigned the «attribute» stereotype.
	 *  2. It does not have assigned the «enumliteral» stereotype.
	 *  3. It is not an object property nor does it have an inverseOf property.
	 * </pre>
	 * 
	 * @param resource The resource for which to test whether or not it is an
	 *                 attribute.
	 * @return True if the resource passed in is, by definition, an attribute; false
	 *         otherwise.
	 */
	protected boolean isAttribute(OntResource resource) {
		return CIMRuleUtils.isAttribute(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an attribute
	 * (in contrast to an association or an enum literal). For the purposes of this
	 * method, a resource is an enum literal if it meets the following criteria:
	 * 
	 * <pre>
	 *  1. It has assigned the «enumliteral» stereotype.
	 *  2. It is not an object property nor does it have an inverseOf property.
	 * </pre>
	 * 
	 * @param resource The resource for which to test whether or not it is an enum
	 *                 literal.
	 * @return True if the resource passed in is, by definition, an enum literal;
	 *         false otherwise.
	 */
	protected boolean isEnumLiteral(OntResource resource) {
		return CIMRuleUtils.isEnumLiteral(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is a class. For
	 * the purposes of this method, a 'Class' is defined as any class defined in the
	 * CIM that does not have one of the "restricted stereotypes" assigned to it.
	 * The set of such "restricted stereotypes" includes:
	 * 
	 * <pre>
	 * «enumeration»
	 * «CIMDatatype»
	 * «Primitive»
	 * «Compound»
	 * </pre>
	 * 
	 * Note that the term "restricted" simply means that such classes are now
	 * allowed to participate in association or inheritance relationships, but
	 * instead are restricted to only being used as a declared type of an attribute
	 * in the model.
	 * 
	 * @param resource The resource for which to test whether or not it is a class.
	 * @return True if the resource passed in is, by definition, a CIM Class; false
	 *         otherwise.
	 */
	protected boolean isClass(OntResource resource) {
		return CIMRuleUtils.isClass(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is a datatype
	 * class. For the purposes of this method, a 'Datatype Class' is any class
	 * defined in the CIM that are/have in essence an equivalent primitive.
	 * Therefore, either «CIMDatatype» or «Primitive» currently fall into this
	 * category.
	 * 
	 * @param resource The resource for which to test whether or not it is a
	 *                 datatype class.
	 * @return True if the resource passed in is, by definition, a CIM datatype
	 *         class; false otherwise.
	 */
	protected boolean isDatatypeClass(OntResource resource) {
		return CIMRuleUtils.isDatatypeClass(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is in the set
	 * of all possible CIM UML model class types. This set includes:
	 * 
	 * <pre>
	 * 1. Enumerations
	 * 2. Primitives
	 * 3. Compounds
	 * 4. CIMDatatypes
	 * 5. Classes (i.e. classes without one of the above "restricted stereotypes" assigned)
	 * </pre>
	 * 
	 * @param resource The resource for which to test whether or not it is in the
	 *                 set of possible CIM UML model class types.
	 * @return True if the resource passed in is, by definition, in the set of all
	 *         possible CIM UML class types; false otherwise.
	 */
	protected boolean isCIMUmlClass(OntResource resource) {
		return CIMRuleUtils.isCIMUmlClass(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is in the set
	 * of CIM UML model classes restricted from participation in association or
	 * inheritance relationships in the CIM. Such classes are only to be used as
	 * declared types on an attribute. By definition, this is any class that has
	 * assigned one of the "restricted stereotypes" below:
	 * 
	 * <pre>
	 * «enumeration»
	 * «CIMDatatype»
	 * «Primitive»
	 * «Compound»
	 * </pre>
	 * 
	 * @param resource The resource for which to test whether or not it is in the
	 *                 set of CIM classes with restricted stereotypes.
	 * @return True if the resource passed in is, by definition, in the set of CIM
	 *         classes with restricted stereotypes; false otherwise.
	 */
	protected boolean isRestrictedClass(OntResource resource) {
		return CIMRuleUtils.isRestrictedClass(resource);
	}

	/**
	 * Convenience method that checks whether the resource passed in is a shadow
	 * class. By definition, this is any class that is either:
	 * 
	 * <pre>
	 * 1. A class in an extensions package that has a «ShadowExtension» stereotype
	 * explicitly declared and which is the superclass of normative CIM class.
	 * 
	 * 2. A class in an extensions package that does not have a «ShadowExtension»
	 * stereotype explicitly declared but which is a class in a non-normative
	 * namespace and which has an identical name of a normative CIM class of which
	 * it is the parent class of in the model.
	 * </pre>
	 * 
	 * @param baseURI  The base URI of the normative CIM canonical model.
	 * @param resource The resource for which to test whether or not it is a shadow
	 *                 class in the model.
	 * @return True if the resource passed in is, by definition, a shadow class;
	 *         false otherwise.
	 */
	protected boolean isShadowClass(String baseURI, OntResource resource) {
		return CIMRuleUtils.isShadowClass(baseURI, resource);
	}

	/**
	 * Convenience method that, for a given shadow class passed in, will obtain the
	 * class that is being shadowed. Note that the method verifies first that the
	 * specified class is indeed a shadow class. By definition, a class being
	 * shadowed must meet the following criteria:
	 * 
	 * <pre>
	 * 1. It must reside in a different namespace than the shadow class (i.e.
	 * typically the CIM normative namespace).
	 * 
	 * 2. It must be the only child class of the specified shadow class.
	 * </pre>
	 * 
	 * @param baseURI     The base URI of the normative CIM canonical model.
	 * @param shadowClass The resource for which to test whether or not it is a
	 *                    shadow class in the model.
	 * @return The class (typically normative) being shadowed by the specified
	 *         shadow class.
	 */
	protected OntResource getClassBeingShadowed(String baseURI, OntResource resource) {
		return CIMRuleUtils.getClassBeingShadowed(baseURI, resource);
	}

	/**
	 * Retrieve the composite rule ID (i.e., the name from the @Rule annotation).
	 *
	 * @return rule ID as defined in @Rule(name = ...)
	 */
	protected String getRuleId() {
		Rule ruleAnnotation = getRuleAnnotation();
		return ruleAnnotation != null ? ruleAnnotation.name() : getClass().getSimpleName();
	}

	/**
	 * Retrieve the rule description from the @Rule annotation.
	 *
	 * @return rule description if present, else an empty string.
	 */
	protected String getRuleDescription() {
		Rule ruleAnnotation = getRuleAnnotation();
		return ruleAnnotation != null ? ruleAnnotation.description() : "";
	}

	/**
	 * Retrieve the composite rule ID (i.e., the compositeRuleId from
	 * the @RuleMetadata annotation).
	 *
	 * @return composite rule ID as defined in @RuleMetadata(compositeRuleId = ...)
	 */
	protected String getCompositeRuleId() {
		RuleMetadata info = getClass().getAnnotation(RuleMetadata.class);
		return info != null ? info.compositeRule() : "";
	}

	/**
	 * Retrieve the composite subrule ID (i.e., the compositeSubRuleId from
	 * the @RuleMetadata annotation).
	 *
	 * @return composite subrule ID as defined in @RuleMetadata(compositeSubRuleId =
	 *         ...)
	 */
	protected String getCompositeSubRuleId() {
		RuleMetadata info = getClass().getAnnotation(RuleMetadata.class);
		return info != null ? info.compositeSubRule() : "";
	}

	/**
	 * Retrieve the rule category from the @RuleViolationInfo annotation.
	 */
	protected RuleType getRuleType() {
		RuleMetadata info = getClass().getAnnotation(RuleMetadata.class);
		return info != null ? info.type() : null;
	}

	/**
	 * Retrieve the rule category from the @RuleViolationInfo annotation.
	 */
	protected RuleCategory getRuleCategory() {
		RuleMetadata info = getClass().getAnnotation(RuleMetadata.class);
		return info != null ? info.category() : null;
	}

	/**
	 * Retrieve the violation severity from the @RuleViolationInfo annotation.
	 */
	protected RuleSeverity getViolationSeverity() {
		RuleMetadata info = getClass().getAnnotation(RuleMetadata.class);
		return info != null ? info.severity() : null;
	}

	/**
	 * Retrieve the error message template override from the @RuleViolationInfo
	 * annotation.
	 *
	 * @return errorMsg if present, else null
	 */
	protected String getErrorTemplate() {
		RuleMetadata info = getClass().getAnnotation(RuleMetadata.class);
		return info != null ? (info.errorTemplate().length() > 0 ? info.errorTemplate() : null) : null;
	}

	/**
	 * Gets the @Rule annotation for this rule class.
	 *
	 * @return the Rule annotation instance or null if not present
	 */
	private Rule getRuleAnnotation() {
		return getClass().getAnnotation(Rule.class);
	}

	/**
	 * Replace placeholders in the errorMsg template with actual values. Supported
	 * format: {key} for replacement using Map values.
	 * 
	 * <pre>
	 * Supported placeholders are:
	 * 
	 * {uri}: The URI of the resource
	 * {stereotype}: The name of the class
	 * {elementType}: The name of the class
	 * {name}: The name of the class
	 * {localName}: The local name of the resource
	 * {domainName}: The name of the domain
	 * {domainStereotype}: If relevant, the restricted stereotype of the domain
	 * {rangeName}: The name of the range
	 * {rangeStereotype}: If relevant, the restricted stereotype of the range
	 * {attributeCard}: The cardinality of the attribute
	 * {enumLiteralCard}: The invalid cardinality of an enum literal if present
	 * {sourceRoleEnd}: The source side role end name
	 * {targetRoleEnd}: The target side role end name
	 * {sourceCard}: The source side cardinality
	 * {targetCard}: The target side cardinality
	 * {packageHierarchy}: The full package hierarchy of the class
	 * {correctedValue}: A placeholder for any corrected value
	 * {scope}: The scope (e.g. Public/Private) of a class or attribute
	 * {fullyQualifiedName}: The fully qualified of the class or element
	 * {roleEndName}: The role end name for an association
	 *
	 * </pre>
	 * 
	 * @param resource The resource from which to extract the placeholder values.
	 * @return A map of placeholder keys to their replacement values
	 */
	protected Map<String, String> getPlaceholderValues(OntResource resource, Map<String, List<String>> namesMap) {
		Map<String, String> placeholderValues = new HashMap<>();

		StringBuffer theFullyQualifiedName = new StringBuffer();
		//
		placeholderValues.put(elementType, getResourceType(resource).toString());
		placeholderValues.put(elementTypeLowerCase, getResourceType(resource).toString().toLowerCase());
		//
		String thePackageHierarchy = getPackageHierarchy(resource);
		placeholderValues.put(packageHierarchy, applyAsciidocBoldStyling(thePackageHierarchy));
		theFullyQualifiedName.append((!thePackageHierarchy.isBlank() ? thePackageHierarchy + "::" : ""));
		//
		placeholderValues.put(uri, (resource.getURI() != null ? resource.getURI() : "<Unknown>"));
		//
		switch (getResourceType(resource)) {
		case Enumeration:
			placeholderValues.put(stereotype, "«enumeration»");
			break;
		case Primitive:
			placeholderValues.put(stereotype, "«Primitive»");
			break;
		case ConstrainedPrimitive:
			placeholderValues.put(stereotype, "«ConstrainedPrimitive»");
			break;
		case Compound:
			placeholderValues.put(stereotype, "«Compound»");
			break;
		case CIMDatatype:
			placeholderValues.put(stereotype, "«CIMDatatype»");
			break;
		}
		//
		String resourceName = "";
		switch (getResourceType(resource)) {
		case Package:
			resourceName = getLabelDefaultUnknown(resource);
			resourceName = (resourceName.startsWith("Package_") ? resourceName.substring(resourceName.indexOf("_") + 1)
					: resourceName);
			break;
		case Enumeration:
		case Primitive:
		case ConstrainedPrimitive:
		case Compound:
		case CIMDatatype:
		case Class:
			resourceName = getLabelDefaultUnknown(resource);
			break;
		case Attribute:
			resourceName = getLabelDefaultUnknown(resource.getDomain()) + "." + getLabelDefaultUnknown(resource);
			//
			Integer minCard = 0; // Default min cardinality
			Integer maxCard = 1; // Default max cardinality
			//
			Integer min = resource.getInteger(UML.hasMinCardinality);
			if (min != null)
				minCard = min.intValue();
			//
			Integer max = resource.getInteger(UML.hasMaxCardinality);
			if (max != null)
				maxCard = max.intValue();
			//
			placeholderValues.put(attributeCard, cardString(minCard, maxCard));
			break;
		case Association:
			resourceName = getLabelDefaultUnknown(resource.getDomain()) + "." + getLabelDefaultUnknown(resource);
			placeholderValues.put(roleEndName, applyAsciidocStyling(getLabelDefaultUnknown(resource)));
			//
			OntResource domain = resource.getDomain();
			OntResource range = resource.getRange();
			if (domain != null && range != null) {
				if (isSourceSide(resource)) {
					placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
					placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
				} else {
					placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
					placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
				}
			} else if (domain != null) {
				if (isSourceSide(resource)) {
					placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
					placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
				} else {
					placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
					placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
				}
			} else if (range != null) {
				if (isSourceSide(resource)) {
					placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
					placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
				} else {
					placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
					placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
				}
			} else {
				placeholderValues.put(sourceClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
				placeholderValues.put(targetClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
			}

			/**
			 * We pass null to force the default ('<Unknown>') to be returned since we can
			 * not legitimately determine correct source/target classes unless both are
			 * present.
			 */
			if (domain == null || range == null) {
				placeholderValues.put(correctSourceClass, applyAsciidocStyling(getLabelDefaultUnknown(null)));
				placeholderValues.put(correctTargetClass, applyAsciidocStyling(getLabelDefaultUnknown(null)));
			} else {
				if (getDependencyPriority(domain) > getDependencyPriority(range)) {
					placeholderValues.put(correctSourceClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
					placeholderValues.put(correctTargetClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
				} else {
					placeholderValues.put(correctSourceClass, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
					placeholderValues.put(correctTargetClass, applyAsciidocStyling(getLabelDefaultUnknown(range)));
				}
			}
			//
			Integer associationMinCard = 0; // Default min cardinality
			Integer associationMaxCard = Integer.MAX_VALUE; // Default max cardinality
			//
			Integer associationMin = resource.getInteger(UML.hasMinCardinality);
			if (associationMin != null)
				associationMinCard = associationMin.intValue();
			//
			Integer associationMax = resource.getInteger(UML.hasMaxCardinality);
			if (associationMax != null)
				associationMaxCard = associationMax.intValue();
			//
			placeholderValues.put(sourceCard, cardString(associationMinCard, associationMaxCard));
			/**
			 * The targetRoleEnd is here because it is what is expressed as part of the
			 * resource object...
			 */
			placeholderValues.put(targetRoleEnd, getLabelDefaultUnknown(resource));
			//
			OntResource inverseResource = resource.getInverse();
			if (inverseResource != null) {
				Integer inverseAssociationMinCard = 0; // Default min cardinality
				Integer inverseAssociationMaxCard = Integer.MAX_VALUE; // Default max cardinality
				//
				Integer inverseAssociationMin = resource.getInteger(UML.hasMinCardinality);
				if (inverseAssociationMin != null)
					inverseAssociationMinCard = inverseAssociationMin.intValue();
				//
				Integer inverseAssociationMax = resource.getInteger(UML.hasMaxCardinality);
				if (inverseAssociationMax != null)
					inverseAssociationMaxCard = inverseAssociationMax.intValue();
				placeholderValues.put(targetCard, cardString(inverseAssociationMinCard, inverseAssociationMaxCard));
				/**
				 * The sourceRoleEnd is here because it is what is expressed as part of the
				 * inverse resource object...
				 */
				placeholderValues.put(sourceRoleEnd, getLabelDefaultUnknown(inverseResource));
			} else {
				placeholderValues.put(targetCard, "<Unknown>");
				placeholderValues.put(sourceRoleEnd, getLabelDefaultUnknown(null));
			}
			break;
		case EnumLiteral:
			resourceName = getLabelDefaultUnknown(resource.getDomain()) + "." + getLabelDefaultUnknown(resource);
			if (resource.hasProperty(UML.hasMinCardinality) || resource.hasProperty(UML.hasMaxCardinality)) {
				//
				// For enum literals we default to null (shouldn't be any cardinality)
				Integer enumLiteralMinCard = 0;
				Integer enumLiteralMaxCard = 1;

				Integer enumLiteralMin = resource.getInteger(UML.hasMinCardinality);
				if (enumLiteralMin != null)
					enumLiteralMinCard = enumLiteralMin.intValue();
				//
				Integer enumLiteralMax = resource.getInteger(UML.hasMaxCardinality);
				if (enumLiteralMax != null)
					enumLiteralMaxCard = enumLiteralMax.intValue();
				//
				placeholderValues.put(enumLiteralCard, cardString(enumLiteralMinCard, enumLiteralMaxCard));
			} else {
				placeholderValues.put(enumLiteralCard, "");
			}
			break;
		case Unknown:
			break;
		}

		placeholderValues.put(name, applyAsciidocStyling(resourceName));

		theFullyQualifiedName.append(resourceName);
		placeholderValues.put(fullyQualifiedName, applyAsciidocStyling(theFullyQualifiedName.toString()));

		if (resource.getDomain() != null) {
			OntResource domain = resource.getDomain();
			placeholderValues.put(domainName, applyAsciidocStyling(getLabelDefaultUnknown(domain)));
			if (domain.hasProperty(UML.hasStereotype, UML.enumeration)) {
				placeholderValues.put(domainStereotype, "«enumeration»");
			} else if (domain.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
				placeholderValues.put(domainStereotype, "«CIMDatatype»");
			} else if (domain.hasProperty(UML.hasStereotype, UML.compound)) {
				placeholderValues.put(domainStereotype, "«Compound»");
			} else if (domain.hasProperty(UML.hasStereotype, UML.primitive)) {
				placeholderValues.put(domainStereotype, "«Primitive»");
			}
		}
		if (resource.getRange() != null) {
			OntResource range = resource.getRange();
			placeholderValues.put(rangeName, applyAsciidocStyling(getLabelDefaultUnknown(range)));
			if (range.hasProperty(UML.hasStereotype, UML.enumeration)) {
				placeholderValues.put(rangeStereotype, "«enumeration»");
			} else if (range.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
				placeholderValues.put(rangeStereotype, "«CIMDatatype»");
			} else if (range.hasProperty(UML.hasStereotype, UML.compound)) {
				placeholderValues.put(rangeStereotype, "«Compound»");
			} else if (range.hasProperty(UML.hasStereotype, UML.primitive)) {
				placeholderValues.put(rangeStereotype, "«Primitive»");
			}
		}
		//
		return placeholderValues;
	}

	protected String applyAsciidocBoldStyling(String element) {
		if (element == null)
			return "";
		StringBuffer styledElement = new StringBuffer();
		styledElement.append("*'").append(element).append("'*");
		return styledElement.toString();
	}

	/**
	 * Convenience method that applies asciidoc styling conventions to an element.
	 * Currently, conventions are simple in that the method styles the package name
	 * in bold and the class, enumeration, attribute or association name in red.
	 * 
	 * @param element
	 * @return The element with simply asciidoc styles applied.
	 */
	protected String applyAsciidocStyling(String element) {
		if (element == null)
			return "";
		StringBuffer styledElement = new StringBuffer();
		int lastIndexOfDoubleColon = element.lastIndexOf("::");
		if (lastIndexOfDoubleColon >= 0) {
			styledElement.append("*").append(element.substring(0, lastIndexOfDoubleColon + 2)).append("*")
					.append("[.red]#").append(element.substring(lastIndexOfDoubleColon + 2)).append("#");
		} else {
			styledElement.append("[.red]#'").append(element).append("'#");
		}
		return styledElement.toString();
	}

}