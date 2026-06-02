/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.utils;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.xmi.UML;
import au.com.langdale.xmi.XMI;

import java.util.Objects;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Utility class for use by Rules classes. This class was introduced for future
 * use when EasyRules scripting is one day utilized. When that occurs, this
 * class and its static methods should be easily integrated and used within the
 * scripting language.
 */
public class CIMRuleUtils {

	/**
	 * Top level packages to use to validate uni-directional model dependencies.
	 */
	private static final String[] ROOT_CIM = new String[] { "TC57CIM", "CIM" };
	private static final String[] GRID = new String[] { "IEC61970", "Grid" };
	private static final String[] DYNAMICS = new String[] { "Dynamics" };
	private static final String[] ENTERPRISE = new String[] { "IEC61968", "Enterprise" };
	private static final String[] MARKET = new String[] { "IEC62325", "Market" };
	private static final String[] ALL_TOP_LEVEL = new String[] { //
			"IEC61970", "Grid", //
			"IEC61968", "Enterprise", //
			"IEC62325", "Market" };

	public static boolean isNormative(OntResource resource, String baseURI) {
		if (resource.hasRDFType(UML.Package)) {
			return isPackageInNormativeHierarchy(resource);
		} else if ((resource.isClass() || resource.hasProperty(RDF.type, RDFS.Datatype))
				&& !resource.equals(UML.Stereotype)) {
			return isInNormativeCIMPackage(resource);
		} else if (resource.getURI().startsWith(XMI.NS)) {
			/**
			 * If the above logic is met it means that import processing is in the early
			 * stages and namespace/name remapping has not yet happened and therefore a
			 * slightly different check for normative is needed.
			 */
			if (resource.isObjectProperty() && resource.hasProperty(OWL2.inverseOf)) {
				OntResource domain = resource.getDomain();
				OntResource range = resource.getRange();
				if (domain != null && range != null) {
					boolean isDomainNormative = isInNormativeCIMPackage(domain);
					boolean isRangeNormative = isInNormativeCIMPackage(range);
					if (isDomainNormative && isRangeNormative) {
						String taggedValueBaseURI = resource.getString(UML.baseuri);
						if (taggedValueBaseURI == null
								|| (taggedValueBaseURI != null && taggedValueBaseURI.startsWith(baseURI)))
							return true;
					}
				}
			} else if (resource.hasProperty(UML.hasStereotype, UML.attribute)) {
				OntResource domain = resource.getDomain();
				if (domain != null && isInNormativeCIMPackage(domain)) {
					return true;
				}
			}
		} else {
			if (resource.getURI() != null && resource.getURI().startsWith(baseURI))
				return true;
		}
		return false;
	}

	public static boolean isInformative(OntResource resource) {
		if (resource.hasRDFType(UML.Package)) {
			return isPackageInformative(resource);
		} else if (resource.isClass() && !resource.equals(UML.Stereotype)) {
			return isPackageInformative(resource.getIsDefinedBy());
		}
		return false;
	}

	/**
	 * Returns the value to be used for the class when determining dependency
	 * hierarchy ordering relative to other classes. This is needed when validating
	 * "source" and "target" sides for association and generalization relationships.
	 * A class that has a lower value is lower in the dependency hierarchy.
	 * 
	 * @param aClass A resource that is required to be a class.
	 * @return The value to be used for the class in determining dependency ordering
	 *         relative to other classes.
	 */
	public static int getDependencyPriority(OntResource aClass) {
		Objects.requireNonNull(aClass);
		if (!(aClass.isClass() || aClass.isDatatype() || aClass.hasProperty(UML.hasStereotype, UML.primitive)
				|| aClass.hasProperty(UML.hasStereotype, UML.constrainedprimitive)))
			throw new IllegalArgumentException(String.format("Resource '%s' is not a class.", aClass.getLabel()));
		if (isInGrid(aClass))
			return 5;
		if (isInDynamics(aClass))
			return 4;
		if (isInEnterprise(aClass))
			return 3;
		if (isInMarket(aClass))
			return 2;
		return 1;
	}

	/**
	 * Convenience method to determine if the specified package is an informative
	 * package. By definition, this is any package that adheres to the naming
	 * convention of prepending 'Inf' prefix to any package this contains classes
	 * that are not normative (e.g. InfWires). Additionally, this automatically
	 * extends to any sub-packages (at any level) if such package; even if the
	 * subpackages do not start with the 'Inf' prefix. It is very strongy encouraged
	 * that all packages, at any level, be prefixed with 'Inf'.
	 * 
	 * @param aPackage The package to be tested to determine if it is an informative
	 *                 package.
	 * @return True if it is determined, by definition, that the package is an
	 *         informative package; false otherwise.
	 */
	public static boolean isPackageInformative(OntResource aPackage) {
		if (aPackage == null)
			return false;
		if (!aPackage.hasRDFType(UML.Package))
			throw new IllegalArgumentException(String.format("Resource '%s' is not a package.", aPackage.getLabel()));
		while (aPackage != null && !aPackage.equals(UML.global_package)) {
			if (aPackage.getLabel().startsWith("Inf"))
				return true;
			aPackage = aPackage.getIsDefinedBy(); // Assign to it's parent package and loop...
		}
		return false;
	}

	public static boolean isRootCIMPackage(OntResource resource) {
		if (resource == null)
			return false;
		if (resource.hasRDFType(UML.Package)) {
			for (String aPackage : ROOT_CIM) {
				if (aPackage.equals(resource.getLabel())) {
					// If the package is in the specified set then we proceed to
					// check if the parent of that package is the global package.
					if (resource.getIsDefinedBy() != null) {
						if (resource.getIsDefinedBy().equals(UML.global_package))
							return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isTopLevelCIMPackage(OntResource resource) {
		if (resource == null)
			return false;
		if (resource.hasRDFType(UML.Package)) {
			for (String aPackage : ALL_TOP_LEVEL) {
				if (aPackage.equals(resource.getLabel())) {
					OntResource parentPackage = resource.getIsDefinedBy();
					// If the package is in the specified set then we proceed to
					// check if the parent of that package is the root CIM package.
					if (parentPackage != null) {
						return isRootCIMPackage(parentPackage);
					}
				}
			}
		}
		return false;
	}

	public static boolean isGridPackage(OntResource resource) {
		if (resource == null)
			return false;
		if (resource.hasRDFType(UML.Package)) {
			for (String aPackage : GRID) {
				if (aPackage.equals(resource.getLabel())) {
					OntResource parentPackage = resource.getIsDefinedBy();
					// If the package is in the specified set then we proceed to
					// check if the parent of that package is the root CIM package.
					if (parentPackage != null) {
						return isRootCIMPackage(parentPackage);
					}
				}
			}
		}
		return false;
	}

	public static boolean isEnterprisePackage(OntResource resource) {
		if (resource == null)
			return false;
		if (resource.hasRDFType(UML.Package)) {
			for (String aPackage : ENTERPRISE) {
				if (aPackage.equals(resource.getLabel())) {
					OntResource parentPackage = resource.getIsDefinedBy();
					// If the package is in the specified set then we proceed to
					// check if the parent of that package is the root CIM package.
					if (parentPackage != null) {
						return isRootCIMPackage(parentPackage);
					}
				}
			}
		}
		return false;
	}

	public static boolean isMarketPackage(OntResource resource) {
		if (resource == null)
			return false;
		if (resource.hasRDFType(UML.Package)) {
			for (String aPackage : MARKET) {
				if (aPackage.equals(resource.getLabel())) {
					OntResource parentPackage = resource.getIsDefinedBy();
					// If the package is in the specified set then we proceed to
					// check if the parent of that package is the root CIM package.
					if (parentPackage != null) {
						return isRootCIMPackage(parentPackage);
					}
				}
			}
		}
		return false;
	}

	public static boolean isInExtensionPackage(OntResource resource) {
		if (resource.equals(UML.Stereotype) && (!resource.hasProperty(RDF.type, RDFS.Datatype)))
			throw new IllegalArgumentException(
					String.format("Resource '%s' is not a valid class.", resource.getLabel()));
		else if (!isInNormativeCIMPackage(resource))
			return true;
		return false;
	}

	public static boolean isPackageInNormativeHierarchy(OntResource resource) {
		if (!resource.hasRDFType(UML.Package))
			throw new IllegalArgumentException(
					String.format("Resource '%s' is not a valid package.", resource.getLabel()));
		OntResource aPackage = resource;
		while (aPackage != null && !aPackage.equals(UML.global_package)) {
			if (isTopLevelCIMPackage(aPackage))
				return true;
			aPackage = aPackage.getIsDefinedBy(); // Assign to the parent package in the hierarchy
		}
		return false;
	}

	public static boolean isInNormativeCIMPackage(OntResource resource) {
		if (resource.equals(UML.Stereotype) || (!resource.isClass() && !resource.hasProperty(RDF.type, RDFS.Datatype)))
			throw new IllegalArgumentException(
					String.format("Resource '%s' is not a valid class.", resource.getLabel()));
		if (isInGrid(resource) || isInEnterprise(resource) || isInMarket(resource))
			return true;
		return false;
	}

	/**
	 * Returns whether the specified resource is contained within the top level CIM
	 * modeling hierarchy (i.e. the 'TC57CIM' package prior to CIM18 and the 'CIM'
	 * package beginning with CIM18).
	 * 
	 * @param resorce
	 * @return True if the class is within the normative CIM hierarchy; false
	 *         otherwise.
	 */
	public static boolean isInRootCIM(OntResource resource) {
		if (resource.equals(UML.Stereotype) && resource.isClass() && resource.getIsDefinedBy() != null) {
			OntResource aPackage = resource.getIsDefinedBy();
			while (aPackage != null && !aPackage.equals(UML.global_package)) {
				String currentPackageName = aPackage.getLabel();
				aPackage = aPackage.getIsDefinedBy();
				for (String pkgName : ROOT_CIM) {
					if (pkgName.equals(currentPackageName)) {
						// If the package is in the specified set then we proceed to
						// check if the parent of that package is the global package.
						if (aPackage.equals(UML.global_package))
							return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isInDynamics(OntResource resource) {
		return isContainedIn(resource, DYNAMICS);
	}

	public static boolean isInGrid(OntResource resource) {
		return isContainedIn(resource, GRID);
	}

	public static boolean isInEnterprise(OntResource resource) {
		return isContainedIn(resource, ENTERPRISE);
	}

	public static boolean isInMarket(OntResource resource) {
		return isContainedIn(resource, MARKET);
	}

	private static boolean isContainedIn(OntResource resource, String[] packageSet) {
		if ((resource.isClass() || resource.hasProperty(RDF.type, RDFS.Datatype)) && resource.getIsDefinedBy() != null
				&& packageSet != null) {
			OntResource aPackage = resource.getIsDefinedBy();
			while (aPackage != null && !aPackage.equals(UML.global_package)) {
				String currentPackageName = aPackage.getLabel(); // First save the current package name
				aPackage = aPackage.getIsDefinedBy(); // Then assign to the parent package
				for (String pkgName : packageSet) {
					if (pkgName.equals(currentPackageName)) {
						// If the package is in the specified set then we proceed to
						// check if the parent of that package is the root CIM package.
						return isRootCIMPackage(aPackage);
					}
				}
			}
		}
		return false;
	}

	public static boolean isSourceSide(OntResource association) {
		if (association != null && (association.isObjectProperty() && association.hasProperty(OWL2.inverseOf))
				&& association.hasProperty(UML.id)) {
			String id = association.getString(UML.id);
			if (id != null && (id.endsWith("-A") || id.endsWith("-B"))) {
				return id.endsWith("-A");
			}

		}
		throw new IllegalArgumentException(
				String.format("OntResource <%s> is not an association.", association.getURI()));
	}

	public static boolean isTargetSide(OntResource association) {
		if (association != null && (association.isObjectProperty() && association.hasProperty(OWL2.inverseOf))
				&& association.hasProperty(UML.id)) {
			String id = association.getString(UML.id);
			if (id != null && (id.endsWith("-A") || id.endsWith("-B"))) {
				return id.endsWith("-B");
			}

		}
		throw new IllegalArgumentException(String.format("OntResource <%s> is not an association.",
				(association != null ? association.getURI() : "null")));
	}

	/**
	 * Convenience method that accepts a class (or any element) as input and which
	 * constructs the complete package hierarchy from the containing package up
	 * through the top-most root package.
	 * 
	 * @param resource The class for which to construct the package hierarchy for.
	 * @return The fully package hierarchy from the top-most package down through
	 *         the containing package.
	 */
	public static String getPackageHierarchy(OntResource resource) {
		String packageHierarchy = null;
		if (resource.getIsDefinedBy() != null) {
			OntResource aPackage = resource.getIsDefinedBy();
			while (aPackage != null && !aPackage.equals(UML.global_package)) {
				String result = aPackage.getLabel();
				if (result == null) {
					if (aPackage.isAnon()) {
						result = "Unnamed";
					} else {
						result = aPackage.getLocalName();
					}
				}
				packageHierarchy = (packageHierarchy != null ? result + "::" + packageHierarchy : result);
				aPackage = aPackage.getIsDefinedBy();
			}
		}
		return (packageHierarchy == null ? "" : packageHierarchy);
	}

	public static boolean hasLabel(OntResource resource) {
		if (resource != null && resource.getLabel() != null && !resource.getLabel().trim().isBlank()) {
			return true;
		}
		return false;
	}

	public static boolean hasLocalName(OntResource resource) {
		if (resource != null && resource.getLocalName() != null && !resource.getLocalName().trim().isBlank()) {
			return true;
		}
		return false;
	}

	public static boolean hasComment(OntResource resource) {
		if (resource != null && resource.getComment() != null && !resource.getComment().trim().isBlank()) {
			return true;
		}
		return false;
	}

	public static String getLabel(OntResource resource) {
		if (resource != null && resource.getLabel() != null && resource.getLabel().trim().isBlank()) {
			return resource.getLabel();
		}
		return null;
	}

	public static String getLabel(OntResource resource, String defaultValue) {
		if (hasLabel(resource)) {
			return resource.getLabel();
		}
		return defaultValue;
	}

	public static String getLabelDefaultUnknown(OntResource resource) {
		if (hasLabel(resource)) {
			return resource.getLabel();
		}
		return "<Unknown>";
	}

	public static String getLocalNameDefaultUnknown(OntResource resource) {
		if (hasLocalName(resource)) {
			return resource.getLocalName();
		}
		return "<Unknown>";
	}

	public static String cardString(int card) {
		return cardString(card, "n");
	}

	public static String cardString(int card, String unbounded) {
		return card == Integer.MAX_VALUE ? unbounded : Integer.toString(card);
	}

	public static String cardString(int minCard, int maxCard) {
		return cardString(minCard) + ".." + cardString(maxCard);
	}

	public static boolean isUncapitalized(String word) {
		if (word == null || word.isBlank())
			return false;
		return Character.isLowerCase(word.charAt(0));
	}

	public static boolean isCapitalized(String word) {
		if (word == null || word.isBlank())
			return false;
		return Character.isUpperCase(word.charAt(0));
	}

	public static String toUncapitalized(String word) {
		if (word == null || word.isBlank())
			return word;
		return word.substring(0, 1).toLowerCase() + word.substring(1);
	}

	public static String toCapitalized(String word) {
		if (word == null || word.isBlank())
			return word;
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}

	/**
	 * Validates if a given string follows lower camel case convention. Accepts
	 * strings like: voltage, ts3, r0, voltageLevel, someID3.
	 * 
	 * <pre>
	 * Rules:
	 * - Must start with a lowercase letter
	 * - Can contain digits
	 * - Can include camel humps (e.g., UpperCamelWords after the first)
	 * - No underscores or leading digits
	 * </pre>
	 * 
	 * @param value the string to validate
	 * @return true if it is lower camel case; false otherwise
	 */
	public static boolean isLowerCamelCase(String value) {
		return value != null && value.matches("^[a-z][a-z0-9]*([A-Z][a-z0-9]*)*$");
	}

	/**
	 * Validates if a given string follows Upper Camel Case convention. Accepts
	 * strings like: Voltage, Ts3, VoltageLevel, SomeID3.
	 *
	 * Rules: - Must start with an uppercase letter - Can contain lowercase letters
	 * and digits - Can include additional camel humps - No underscores or leading
	 * digits
	 *
	 * @param value the string to validate
	 * @return true if it is Upper Camel Case; false otherwise
	 */
	public static boolean isUpperCamelCase(String value) {
		return value != null && value.matches("^[A-Z][a-z0-9]*([A-Z][a-z0-9]*)*$");
	}

	public static boolean isPossiblyPlural(String word) {
		if (word != null) {
			word = word.trim().toLowerCase();
			if (word.endsWith("ies") && word.length() > 3)
				return true;
			if (word.endsWith("es") && word.length() > 2)
				return true;
			if (word.endsWith("s") && !word.endsWith("ss"))
				return true;
		}
		return false;
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
	 * @param baseURI             The base URI of the normative CIM canonical model.
	 * @param possibleShadowClass The resource for which to test whether or not it
	 *                            is a shadow class in the model.
	 * @return True if the resource passed in is, by definition, a shadow class;
	 *         false otherwise.
	 */
	public static boolean isShadowClass(String baseURI, OntResource possibleShadowClass) {
		if (possibleShadowClass.isClass() || possibleShadowClass.hasProperty(RDF.type, RDFS.Datatype)) {
			if (possibleShadowClass.hasProperty(UML.hasStereotype, UML.shadowextension))
				return true;
			String shadowClassName = possibleShadowClass.getLabel();
			if (shadowClassName != null && !shadowClassName.isBlank()
					&& !possibleShadowClass.getURI().startsWith(baseURI)) {
				ResIterator subClasses = possibleShadowClass.listSubClasses(true);
				while (subClasses.hasNext()) {
					OntResource subClass = subClasses.nextResource();
					if (shadowClassName.equals(subClass.getLabel()) && subClass.getURI().startsWith(baseURI)) {
						return true;
					}
				}
			}
		}
		return false;
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
	public static OntResource getClassBeingShadowed(String baseURI, OntResource shadowClass) {
		if (isShadowClass(baseURI, shadowClass)) {
			String shadowClassName = shadowClass.getLabel();
			OntResource subClass = null;
			int subClassCount = 0;
			ResIterator subClasses = shadowClass.listSubClasses(false);
			while (subClasses.hasNext()) {
				subClass = subClasses.nextResource();
				subClassCount++;
			}
			//
			if (subClassCount == 1 && (!shadowClass.getURI().startsWith(baseURI))) {
				if (!shadowClass.hasProperty(UML.hasStereotype, UML.shadowextension)) {
					// If the shadow class does not have an explicit ShadowExtension stereotype
					// assigned to it we know that it's name must mirror that of a normative
					// class and the name must be used as part of determining the search result.
					if (shadowClassName != null && !shadowClassName.isBlank()
							&& shadowClassName.equals(subClass.getLabel())) {
						return subClass;
					}
				} else {
					return subClass;
				}
			}
		}
		return null;
	}

	/**
	 * Method to determine if the specified declared type is a Sparx EA native type
	 * that can be converted to one of the normative CIM primitive types.
	 * 
	 * @param declaredType
	 * @return true if the declared type specified is an EA native type; false
	 *         otherwise.
	 */
	public static boolean isEANativeType(String declaredType) {
		if (declaredType != null) {
			switch (declaredType) {
			case "boolean":
			case "short":
			case "int":
			case "integer":
			case "long":
			case "float":
			case "double":
			case "char":
			case "string":
				return true;
			}
		}
		return false;
	}

	public static String getCIMDeclaredTypeFromEADeclaredType(String declaredType) {
		if (declaredType != null) {
			// For "self-healing" we infer a mapping for recognized
			// native EA types to CIM primitives...
			switch (declaredType.toLowerCase()) {
			case "boolean":
				return "Boolean";
			case "short":
			case "int":
			case "integer":
			case "long":
				return "Integer";
			case "float":
				return "Float";
			case "double":
				return "Decimal";
			case "char":
			case "string":
				return "String";
			default:
				return "";
			}
		}
		return "";
	}

	/**
	 * Convenience method that checks whether the resource passed in is a package.
	 * For the purposes of this method, a 'Package' is any resource having a an
	 * RDF.type property with a value of UML.Package.
	 * 
	 * @return True if the resource passed in is, by definition, a package; false
	 *         otherwise.
	 */
	public static boolean isPackage(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.hasProperty(RDF.type, UML.Package);
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
	 * «ConstrainedPrimitive»
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
	public static boolean isClass(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.isClass() && !resource.isDatatype() && //
				(!resource.hasProperty(UML.hasStereotype, UML.enumeration)
						&& !resource.hasProperty(UML.hasStereotype, UML.compound)
						&& !resource.hasProperty(UML.hasStereotype, UML.primitive)
						&& !resource.hasProperty(UML.hasStereotype, UML.constrainedprimitive)
						&& !resource.hasProperty(UML.hasStereotype, UML.cimdatatype));
	}

	/**
	 * Convenience method that checks whether the resource passed in is a datatype
	 * class. For the purposes of this method, a 'Datatype Class' is any class
	 * defined in the CIM that are/have in essence an equivalent primitive.
	 * Therefore, either «CIMDatatype», «CIMDatatype» or «Primitive» currently fall
	 * into this category.
	 * 
	 * @param resource The resource for which to test whether or not it is a
	 *                 datatype class.
	 * @return True if the resource passed in is, by definition, a CIM datatype
	 *         class; false otherwise.
	 */
	public static boolean isDatatypeClass(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.isDatatype();
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
	public static boolean isEnumeration(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.isClass() && resource.hasProperty(UML.hasStereotype, UML.enumeration);
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
	public static boolean isCompound(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.isClass() && resource.hasProperty(UML.hasStereotype, UML.compound);
	}

	/**
	 * Convenience method that checks whether the resource passed in is an
	 * primitive. For the purposes of this method, a primitive class is any class
	 * defined in the CIM that has the «Primitive» stereotype assigned. Such classes
	 * in the ontology currently do not have an RDF.type = RDFS.Datatype assigned.
	 * 
	 * @param resource The resource for which to test whether or not it is a CIM
	 *                 primitive class.
	 * @return True if the resource passed in is, by definition, a CIM primitive
	 *         class; false otherwise.
	 */
	public static boolean isPrimitive(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.hasProperty(UML.hasStereotype, UML.primitive);
	}

	/**
	 * Convenience method that checks whether the resource passed in is a
	 * constrained primitive. For the purposes of this method, a constrained
	 * primitive class is any class defined in the CIM that has the
	 * «ConstrainedPrimitive» stereotype assigned. Such classes in the ontology
	 * currently do not have an RDF.type = RDFS.Datatype assigned.
	 * 
	 * @param resource The resource for which to test whether or not it is a CIM
	 *                 constrained primitive class.
	 * @return True if the resource passed in is, by definition, a CIM constrained
	 *         primitive class; false otherwise.
	 */
	public static boolean isConstrainedPrimitive(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.hasProperty(UML.hasStereotype, UML.constrainedprimitive);
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
	public static boolean isCIMDatatype(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.isDatatype() && resource.hasProperty(UML.hasStereotype, UML.cimdatatype);
	}

	/**
	 * Convenience method that checks whether the resource passed in is in the set
	 * of all possible CIM UML model class types. This set includes:
	 * 
	 * <pre>
	 * 1. Enumerations
	 * 2. Primitives
	 * 3. Constrained Primitives
	 * 4. Compounds
	 * 5. CIMDatatypes
	 * 6. Classes (i.e. classes without one of the above "restricted stereotypes" assigned)
	 * </pre>
	 * 
	 * @param resource The resource for which to test whether or not it is in the
	 *                 set of possible CIM UML model class types.
	 * @return True if the resource passed in is, by definition, in the set of all
	 *         possible CIM UML class types; false otherwise.
	 */
	public static boolean isCIMUmlClass(OntResource resource) {
		return isClass(resource) || isEnumeration(resource) || isPrimitive(resource) || isConstrainedPrimitive(resource)
				|| isCompound(resource) || isCIMDatatype(resource);
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
	 * «ConstrainedPrimitive»
	 * «Compound»
	 * </pre>
	 * 
	 * @param resource The resource for which to test whether or not it is in the
	 *                 set of CIM classes with restricted stereotypes.
	 * @return True if the resource passed in is, by definition, in the set of CIM
	 *         UML class types; false otherwise.
	 */
	public static boolean isRestrictedClass(OntResource resource) {
		return isEnumeration(resource) || isPrimitive(resource) || isConstrainedPrimitive(resource)
				|| isCompound(resource) || isCIMDatatype(resource);
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
	public static boolean isAssociation(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.isObjectProperty() && resource.hasProperty(OWL2.inverseOf);
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
	public static boolean isAttribute(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.hasProperty(UML.hasStereotype, UML.attribute)
				&& !resource.hasProperty(UML.hasStereotype, UML.enumliteral)
				&& (!(resource.isObjectProperty() && resource.hasProperty(OWL2.inverseOf)));
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
	public static boolean isEnumLiteral(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		return resource != null && resource.hasProperty(UML.hasStereotype, UML.enumliteral)
				&& (!(resource.isObjectProperty() && resource.hasProperty(OWL2.inverseOf)));
	}

	/**
	 * Convenience method that returns, as an enumerated literal, the type of
	 * resource as defined by the previous convenience methods.
	 *
	 * @param resource The resource for which to return an enumerated literal of the
	 *                 type of resource.
	 * @return A ResourceType corresponding to the type of resource passed in.
	 */
	public static ResourceType getResourceType(OntResource resource) {
		if (resource == null)
			throw new IllegalArgumentException("Resource passed in may not be null.");
		if (isPackage(resource)) {
			return ResourceType.Package;
		} else if (isEnumeration(resource)) {
			return ResourceType.Enumeration;
		} else if (isCompound(resource)) {
			return ResourceType.Compound;
		} else if (isPrimitive(resource)) {
			return ResourceType.Primitive;
		} else if (isConstrainedPrimitive(resource)) {
			return ResourceType.ConstrainedPrimitive;
		} else if (isCIMDatatype(resource)) {
			return ResourceType.CIMDatatype;
		} else if (isAssociation(resource)) {
			return ResourceType.Association;
		} else if (isAttribute(resource)) {
			return ResourceType.Attribute;
		} else if (isEnumLiteral(resource)) {
			return ResourceType.EnumLiteral;
		} else if (isClass(resource)) {
			return ResourceType.Class;
		}
		return ResourceType.Unknown;
	}

	public enum ResourceType {
		Package, //
		Enumeration, //
		CIMDatatype, //
		Compound, //
		Primitive, //
		ConstrainedPrimitive, //
		Class, //
		Association, //
		Attribute, //
		EnumLiteral("Enum Literal"), //
		Unknown;

		private String displayText;

		private ResourceType() {
			displayText = this.name();
		}

		private ResourceType(String display) {
			displayText = display;
		}

		public String toString() {
			return displayText;
		}
	}

}
