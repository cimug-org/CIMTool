/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public final class ShadowClassUtils {

	private static final Logger log = LoggerFactory.getLogger(ShadowClassUtils.class);

	/**
	 * This method is responsible for retrieving the complete list of "shadow
	 * classes" that exists in the full CIM model that is being used as the schema
	 * for the project.
	 * 
	 * The list is determined by combining both a bottom-up and top-down query. This
	 * is necessary to retrieve a complete list.
	 * 
	 * @param baseURI The base URI of the CIM schema (e.g.
	 *                http://www.ucaiug.org/CIM100#)
	 * @return A map contained the list of shadow classes with the key being the
	 *         absolute URI of the mapped shadow class.
	 */
	public static final Map<String, OntResource> getShadowClasses(OntModel model, String baseURI) {
		/**
		 * The bottom-up step is performed first whereby all classes are queried and
		 * iterated through to locate each of the normative CIM classes. When one is
		 * found that has parent classes, each parent classes is checked to determine
		 * whether it is a "shadow class" and if so it is added to the list.
		 */
		Map<String, OntResource> shadowClasses = new HashMap<String, OntResource>();
		ResIterator classesIterator = model.listSubjectsBuffered(RDF.type, OWL2.Class);
		while (classesIterator.hasNext()) {
			OntResource aClass = classesIterator.nextResource();
			if (aClass.getURI().startsWith(baseURI) && aClass.hasProperty(RDFS.subClassOf)) {
				if (log.isTraceEnabled())
					log.trace("CIM classes:  {}", aClass.describe());
				ResIterator it = aClass.listProperties(RDFS.subClassOf);
				while (it.hasNext()) {
					OntResource aSuperClass = it.nextResource();
					if (log.isTraceEnabled())
						log.trace("CIM superclass:  {}", aSuperClass.describe());
					//
					if (isShadowClass(baseURI, aSuperClass, aClass)) {
						shadowClasses.put(aSuperClass.getString(UML.id), aSuperClass);
						if (log.isDebugEnabled()) {
							log.debug("Shadow class identified [bottom-up]: '{}' ({}) shadows normative class '{}'",
									aSuperClass.getLabel() != null ? aSuperClass.getLabel()
											: aSuperClass.getLocalName(),
									aSuperClass.getURI(),
									aClass.getLabel() != null ? aClass.getLabel() : aClass.getLocalName());
						}
					}
				}
			}
		}

		/**
		 * The top-down step is performed second whereby just classes with a
		 * <<ShadowExtension>> stereotype are queried and iterated through and a check
		 * performed to determined if they were NOT identified during the bottom-up
		 * pass.
		 * 
		 * Note that we also check to ensure that namespace is not that of baseURI (i.e.
		 * the namespace for the normative CIM). Since <<ShadowExtension>> stereotypes
		 * have no relevance on normative CIM classes it would then indicate a scenario
		 * where a baseuri tagged value was not specified at an appropriate place in the
		 * model and this instead would have been logged as a CIM modeling violation
		 * during the model validation process. If the above criteria is met then the
		 * "shadow class" is added to the list of shadow classes. This second step is
		 * needed since the first step only verifies the immediate parents. That pass
		 * does not take into consideration that a "shadow class" itself can potentially
		 * have a parent "shadow class" but with the caveat that such classes MUST HAVE
		 * the <<ShadowExtension>> stereotype defined on it.
		 */
		ResIterator shadowClassesIterator = model.listSubjectsBuffered(UML.hasStereotype, UML.shadowextension);
		while (shadowClassesIterator.hasNext()) {
			OntResource aShadowClass = shadowClassesIterator.nextResource();
			if (!shadowClasses.containsKey(aShadowClass.getString(UML.id))
					&& !aShadowClass.getURI().startsWith(baseURI)) {
				if (log.isTraceEnabled())
					log.trace("Shadow class:  {}", aShadowClass.describe());
				shadowClasses.put(aShadowClass.getString(UML.id), aShadowClass);
				if (log.isDebugEnabled()) {
					log.debug("Shadow class identified [top-down, <<ShadowExtension>>]: '{}' ({})",
							aShadowClass.getLabel() != null ? aShadowClass.getLabel() : aShadowClass.getLocalName(),
							aShadowClass.getURI());
				}
			}
		}
		return shadowClasses;
	}

	/**
	 * Check if the the potential shadow class passed in is indeed a shadow class.
	 * There are currently three potential ways that a shadow class can be defined:
	 * 
	 * <pre>
	 * 1. If there is an explicit <<ShadowExtension>> stereotype defined on the class.
	 * 
	 * 2. If there is a <<MixIn>> or <<CIMExtension>> stereotype defined as an OWL
	 *    axiom on the class. Note, that this appears in the UML as one of these 
	 *    respective stereotypes declared on the generalizaton relationship that is 
	 *    defined between the shadow class and the normative class it shadows. When
	 *    importing, CIMTool converts such stereotypes into OWL Axioms defined  
	 *    specifically on the shadow class itself.
	 *    
	 * 3. If the class is in a namespace other than the normative CIM namespace, has 
	 *    none of the three stereotype explicitly assigned, and extends a subclass 
	 *    that is in the normative CIM namespace and which has the same name it has.
	 *    i.e. this, by definition, is an implied shadow class.
	 * </pre>
	 * 
	 * @param baseURI              The normative CIM namespace URI associated with
	 *                             the canonical CIM.
	 * @param potentialShadowClass The class to be evaluated as to whether it is a
	 *                             shadow class.
	 * @return true if the specified class is determined to be a shadow class; false
	 *         otherwise.
	 */
	public static boolean isShadowClass(String baseURI, OntResource potentialShadowClass) {
		if (baseURI != null && !baseURI.isBlank() && !potentialShadowClass.getURI().startsWith(baseURI)) {
			if (hasShadowExtension(potentialShadowClass) || //
					hasMixIn(potentialShadowClass) || //
					hasCIMExtension(potentialShadowClass)) {
				return true;
			} else {
				if (potentialShadowClass.hasProperty(RDFS.subClassOf)) {
					ResIterator it = potentialShadowClass.listSubClasses(true);
					while (it.hasNext()) {
						OntResource aSubClass = it.nextResource();
						if (log.isTraceEnabled())
							log.trace("Subclass of {}:  {}", potentialShadowClass.getURI(), aSubClass.describe());
						if (aSubClass.getURI() != null && aSubClass.getURI().startsWith(baseURI)
								&& aSubClass.getURI().endsWith("#" + potentialShadowClass.getLabel())) {
							if (log.isTraceEnabled())
								log.trace("Implied shadow class:  {}", potentialShadowClass.describe());
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Check if the the potential shadow class passed in is indeed a shadow class.
	 * There are currently three potential ways that a shadow class can be defined:
	 * 
	 * <pre>
	 * 1. If there is an explicit <<ShadowExtension>> stereotype defined on the class.
	 * 
	 * 2. If there is a <<MixIn>> or <<CIMExtension>> stereotype defined as an OWL
	 *    axiom on the class. Note, that this appears in the UML as one of these 
	 *    respective stereotypes declared on the generalizaton relationship that is 
	 *    defined between the shadow class and the normative class it shadows. When
	 *    importing, CIMTool converts such stereotypes into OWL Axioms defined  
	 *    specifically on the shadow class itself.
	 *    
	 * 3. If the class is in a namespace other than the normative CIM namespace, has 
	 *    none of the explicit three stereotype explicitly assigned, and extends a
	 *    subclass in the normative CIM namespace with the same name it has.
	 *    i.e. this, by definition, is an implied shadow class.
	 * </pre>
	 * 
	 * @param baseURI              The normative CIM namespace URI associated with
	 *                             the canonical CIM.
	 * @param potentialShadowClass The class to be evaluated as to whether it is a
	 *                             shadow class.
	 * @param normativeClass       The normative class that the potential shadow
	 *                             class shadows.
	 * @return true if the specified class is determined to be a shadow class; false
	 *         otherwise.
	 */
	public static boolean isShadowClass(String baseURI, OntResource potentialShadowClass, OntResource normativeClass) {
		if (baseURI == null || potentialShadowClass == null || normativeClass == null)
			return false;
		if (!potentialShadowClass.getURI().startsWith(baseURI)) {
			if (hasShadowExtension(potentialShadowClass) || //
					hasMixIn(potentialShadowClass) || //
					hasCIMExtension(potentialShadowClass) || //
					isImpliedShadowClass(baseURI, potentialShadowClass, normativeClass)) {
				if (log.isTraceEnabled())
					log.trace("Shadow class identified:  {}", potentialShadowClass.describe());
				return true;
			}
		} else {
			/**
			 * Here we know the shadow class was not given a namespace and therefore the
			 * proper handling is to log a warning and return false (i.e. treat it as if it
			 * were not a shadow class and leave it to the end user to fix).
			 */
			if (hasShadowExtension(potentialShadowClass)) {
				if (log.isWarnEnabled())
					log.warn(
							"A shadow class declared using the <<ShadowExtension>> stereotype is defined in the normative CIM namespace and should be corrected:  {}",
							potentialShadowClass.describe());
			} else if (hasMixIn(potentialShadowClass)) {
				if (log.isWarnEnabled())
					log.warn(
							"A shadow class declared using the <<MixIn>> stereotype is defined in the normative CIM namespace and should be corrected:  {}",
							potentialShadowClass.describe());
			} else if (hasCIMExtension(potentialShadowClass)) {
				if (log.isWarnEnabled())
					log.warn(
							"A shadow class declared using the <<CIMExtension>> stereotype is defined in the normative CIM namespace and should be corrected:  {}",
							potentialShadowClass.describe());
			}
		}
		return false;
	}

	/**
	 * Checks if the class is in a namespace other than the normative CIM namespace,
	 * has none of the explicit three stereotype explicitly assigned, and extends a
	 * subclass in the normative CIM namespace with the same name it has. (i.e.
	 * this, by definition, is an implied shadow class)
	 * 
	 * @param baseURI              The normative CIM namespace URI associated with
	 *                             the canonical CIM.
	 * @param potentialShadowClass The class to be evaluated as to whether it is a
	 *                             shadow class.
	 * @param normativeClass       The normative class that the potential shadow
	 *                             class shadows.
	 * @return true if the specified class is determined to be a shadow class; false
	 *         otherwise.
	 */
	private static boolean isImpliedShadowClass(String baseURI, OntResource potentialShadowClass,
			OntResource normativeClass) {
		boolean isImpliedShadowClass = false;

		isImpliedShadowClass = normativeClass.getLabel() != null && //
				baseURI != null && //
				!baseURI.isBlank() && //
				normativeClass.getURI().startsWith(baseURI) && //
				potentialShadowClass.getURI().endsWith("#" + normativeClass.getLabel());

		return isImpliedShadowClass;
	}

	/**
	 * Checks if there is an explicit <<ShadowExtension>> stereotype defined on the
	 * class.
	 * 
	 * @param baseURI              The normative CIM namespace URI associated with
	 *                             the canonical CIM.
	 * @param potentialShadowClass The class to be evaluated as to whether it is a
	 *                             shadow class.
	 * @param normativeClass       The normative class that the potential shadow
	 *                             class shadows.
	 * @return true if the specified class is determined to be a shadow class; false
	 *         otherwise.
	 */
	private static boolean hasShadowExtension(OntResource potentialShadowClass) {
		return potentialShadowClass.hasProperty(UML.hasStereotype, UML.shadowextension);
	}

	/**
	 * Checks if there exists an owl:Axiom for a <<MixIn>> stereotype where the
	 * given resource is the annotatedTarget.
	 * 
	 * @param potentialShadowClass the resource to check as annotatedTarget
	 * @return true if a matching axiom exists; false otherwise.
	 */
	private static boolean hasMixIn(OntResource potentialShadowClass) {
		OntModel model = potentialShadowClass.getOntModel();

		ResIterator axioms = model.listSubjectsWithProperty(OWL2.annotatedTarget, potentialShadowClass);
		while (axioms.hasNext()) {
			OntResource axiom = axioms.nextResource();
			if (axiom.hasProperty(UML.hasStereotype, UML.mixin))
				return true;
		}
		return false;
	}

	/**
	 * Checks if there exists an owl:Axiom for a <<CIMExtension>> stereotype where
	 * the given resource is the annotatedTarget. The <<CIMExtension>> stereotype is
	 * the "legacy" equivalent as described in our CIM Modelling Guidelines
	 * publication)
	 * 
	 * @param potentialShadowClass the resource to check as annotatedTarget
	 * @return true if a matching axiom exists; false otherwise.
	 */
	private static boolean hasCIMExtension(OntResource potentialShadowClass) {
		OntModel model = potentialShadowClass.getOntModel();

		ResIterator axioms = model.listSubjectsWithProperty(OWL2.annotatedTarget, potentialShadowClass);
		while (axioms.hasNext()) {
			OntResource axiom = axioms.nextResource();
			if (axiom.hasProperty(UML.hasStereotype, UML.legacyCIMExtension))
				return true;
		}
		return false;
	}

}
