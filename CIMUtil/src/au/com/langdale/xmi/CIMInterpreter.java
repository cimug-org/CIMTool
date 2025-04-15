/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;

/**
 * Extends the generic XMI interpreter to apply IEC CIM modelling conventions.
 *
 */
public class CIMInterpreter extends UMLInterpreter {

	public static final String LANG = null; // if changed, review the SearchWizard code

	public static OntModel interpret(OntModel raw, String baseURI, OntModel annote, boolean usePackageNames,
			boolean mergeShadowExtensions) {
		if (annote != null)
			raw.add(annote);
		CIMInterpreter interpreter = new CIMInterpreter();
		return interpreter.postProcess(raw, baseURI, usePackageNames, mergeShadowExtensions);
	}

	private OntModel postProcess(OntModel raw, String baseURI, boolean usePackageNames, boolean mergeShadowExtensions) {
		setModel(raw);

		System.out.println("Raw XMI model size: " + getModel().size());

		UML.loadOntology(getModel());
		pruneIncomplete();
		labelRoles();

		System.out.println("Stage 1 XMI model size: " + getModel().size());

		Translator translator = new Translator(getModel(), baseURI, usePackageNames);
		translator.run();
		setModel(translator.getModel());

		System.out.println("Stage 3 XMI model size: " + getModel().size());

		propagateComments();
		applyStereotypes(baseURI);
		classifyAttributes();
		removeUntyped();

		if (mergeShadowExtensions) {
			processExtensions(baseURI);
			ExtensionsTranslator extensionsTranslator = new ExtensionsTranslator(getModel(), baseURI);
			extensionsTranslator.run();
			setModel(extensionsTranslator.getModel());
		}

//		convertToSubClassOf("extensionMSITE");
		createOntologyHeader(baseURI);

		System.out.println("Stage 4 XMI model size: " + getModel().size());

		return getModel();
	}

	private void createOntologyHeader(String baseURI) {
		model.createIndividual(Translator.stripHash(baseURI), OWL.Ontology);
		model.setNsPrefix("", baseURI);
	}

	private void propagateComments() {
		ResIterator it = model.listObjectProperties();
		while (it.hasNext()) {
			OntResource prop = it.nextResource();
			if (prop.getComment() == null) {
				OntResource node = prop.getResource(UML.roleAOf);
				if (node == null)
					node = prop.getResource(UML.roleBOf);
				if (node != null) {
					String comment = node.getComment();
					if (comment != null)
						prop.addComment(comment, null);
				}
			}
		}
	}

	/**
	 * Find labels for association roles.
	 */
	private void labelRoles() {
		ResIterator it = model.listObjectProperties();
		while (it.hasNext()) {
			OntResource prop = it.nextResource();
			if (!reLabel(prop, UML.roleAOf, UML.roleALabel))
				reLabel(prop, UML.roleBOf, UML.roleBLabel);
		}
	}

	private boolean reLabel(OntResource prop, FrontsNode roleOf, FrontsNode hasLabel) {
		OntResource node = prop.getResource(roleOf);
		if (node != null) {
			OntResource assoc = node;
			Node label = assoc.getNode(hasLabel);
			if (label != null) {
				prop.addLabel(label.getLiteralLexicalForm(), XMIModel.LANG);
				return true;
			}
		}
		return false;
	}

	/**
	 * Apply CIM stereotypes and special tags to the model.
	 * 
	 * The OWL representation of a UML class is modified in place to match the
	 * stereotype. Recognise stereotypes 'primitive' and 'enumeration'.
	 *
	 */
	private void applyStereotypes(String baseURI) {
		processExtensionsWithCIMStereotypes(baseURI);

		ResIterator jt = model.listSubjectsBuffered(UML.hasStereotype, UML.enumeration);
		while (jt.hasNext()) {
			applyEnumerationStereotype(jt.nextResource());
		}

		applyPrimitiveStereotype(UML.cimdatatype, true);
		applyPrimitiveStereotype(UML.datatype, true);
		applyPrimitiveStereotype(UML.primitive, true); // in future, change to false
		applyPrimitiveStereotype(UML.base, true); // in future, change to false
		applyPrimitiveStereotype(UML.union, false);

		ResIterator lt = model.listSubjectsBuffered(UML.hasStereotype, UML.extendedBy);
		while (lt.hasNext()) {
			convertAssocToSubClassOf(lt.nextResource());
		}
	}

	private void applyPrimitiveStereotype(Resource stereo, boolean interpret_value) {
		ResIterator gt = model.listSubjectsBuffered(UML.hasStereotype, stereo);
		while (gt.hasNext()) {
			applyPrimitiveStereotype(gt.nextResource(), interpret_value);
		}
	}

	/**
	 * Covert primitive or union stereotyped class as a datatype.
	 */
	private void applyPrimitiveStereotype(OntResource clss, boolean interpret_value) {

		OntResource truetype = null;
		String units = null;
		String multiplier = null;
		//
		String valueEAGUID = null;
		String unitEAGUID = null;
		String multiplierEAGUID = null;
		//
		String valueComment = null;
		String unitComment = null;
		String multiplierComment = null;

		// strip the classes properties, record the value and units information
		ResIterator it = model.listSubjectsBuffered(RDFS.domain, clss);
		while (it.hasNext()) {
			OntResource m = it.nextResource();

			if (interpret_value) {
				String name = m.getLabel();
				if (name != null) {
					// this is a CIM-style annotation to indicate the primitive datatype
					if (name.equals("value")) {
						truetype = m.getResource(RDFS.range);
						if (m.hasProperty(UML.id))
							valueEAGUID = m.getString(UML.id);
						if (m.hasProperty(RDFS.comment))
							valueComment = m.getString(RDFS.comment);
					}
					if (name.equals("unit") || name.equals("units")) {
						units = m.getString(UML.hasInitialValue);
						if (m.hasProperty(UML.id))
							unitEAGUID = m.getString(UML.id);
						if (m.hasProperty(RDFS.comment))
							unitComment = m.getString(RDFS.comment);
					}
					if (name.equals("multiplier")) {
						multiplier = m.getString(UML.hasInitialValue);
						if (m.hasProperty(UML.id))
							multiplierEAGUID = m.getString(UML.id);
						if (m.hasProperty(RDFS.comment))
							multiplierComment = m.getString(RDFS.comment);
					}
				}
			}
			// remove spurious property attached to datatype
			m.removeProperties();
		}

		// for XML Schema datatypes remove all definitions
		if (clss.hasProperty(UML.hasStereotype, UML.primitive) || clss.getNameSpace().equals(XSD.getURI())) {
			/**
			 * NOTE: as of the 2.3.0 release, we have commented out the below call to
			 * clss.removeProperties() and replaced with code to remove just the RDF.type
			 * property declared as an OWL.Class. This is intended to serve as the interim
			 * solution to allow for the inclusion of "Primitive" elements in the XSLT
			 * transforms. By removing it as an OWL.Class it does not get processed along
			 * with other resources declared as type OWL.Class.
			 */
			// clss.removeProperties();
			clss.removeProperty(RDF.type, OWL.Class);
		} else {
			// for defined datatypes, establish an RDFS datatype
			clss.removeAll(RDF.type);
			clss.addProperty(RDF.type, RDFS.Datatype);
			//
			if (truetype != null && !clss.equals(truetype)) {
				clss.addProperty(OWL.equivalentClass, truetype);
			}
			if (units != null) {
				clss.addProperty(UML.hasUnits, units);
			}
			if (multiplier != null) {
				clss.addProperty(UML.hasMultiplier, multiplier);
			}
			// 
			// Add the EA GUIDS for the attributes of a CIMDatatype...
			if (valueEAGUID != null) {
				clss.addProperty(UML.valueEAGUID, valueEAGUID);
			}
			if (unitEAGUID != null) {
				clss.addProperty(UML.unitEAGUID, unitEAGUID);
			}
			if (multiplierEAGUID != null) {
				clss.addProperty(UML.multiplierEAGUID, multiplierEAGUID);
			}
			// 
			// Add the comments for the attributes of a CIMDatatype...
			if (valueComment != null) {
				clss.addProperty(UML.valueComment, valueComment);
			}
			if (unitComment != null) {
				clss.addProperty(UML.unitComment, unitComment);
			}
			if (multiplierComment != null) {
				clss.addProperty(UML.multiplierComment, multiplierComment);
			}
		}
	}

	/**
	 * Convert enumeration stereotyped class and attributes to a class and its
	 * individuals.
	 * 
	 * The properties of the class are re-interpreted as members of the enumeration.
	 * Note that OWL EnumeratedClass is not used here because that would create a
	 * closed enumeration.
	 */
	private void applyEnumerationStereotype(OntResource clss) {
		clss.removeAll(RDF.type);
		clss.addProperty(RDF.type, OWL.Class);

		// some UML models have inconsistent stereotypes
		model.remove(clss, UML.hasStereotype, UML.datatype);
		model.remove(clss, UML.hasStereotype, UML.cimdatatype);
		model.remove(clss, UML.hasStereotype, UML.base);
		model.remove(clss, UML.hasStereotype, UML.primitive);

		ResIterator it = model.listSubjectsBuffered(RDFS.domain, clss);
		while (it.hasNext()) {
			OntResource m = it.nextResource();
			if (m.hasProperty(UML.hasStereotype, UML.attribute)) {
				m.removeAll(RDF.type);
				m.removeAll(RDFS.range);
				m.removeAll(RDFS.domain);
				m.removeAll(UML.hasStereotype);
				m.addProperty(RDF.type, clss);
			}
		}
	}

	private void convertAssocToSubClassOf(OntResource assoc) {
		OntResource prop = assoc.getSubject(UML.roleBOf);
		if (prop != null)
			convertPropToSubClassOf(prop);
	}

	private void convertPropToSubClassOf(OntResource prop) {
		OntResource range = prop.getRange();
		OntResource domain = prop.getDomain();
		if (range != null && domain != null) {
			OntResource inv = prop.getInverseOf();
			prop.remove();

			if (model.contains(domain, UML.hasStereotype, UML.extension))
				range.addSuperClass(domain);
			else if (model.contains(range, UML.hasStereotype, UML.extension))
				domain.addSuperClass(range);
			else
				return;

			if (inv != null)
				inv.remove();
		}
	}

	private void processExtensionsWithCIMStereotypes(String baseURI) {
		/**
		 * Pass one here addresses approach one described in the method comments.
		 */
		List<OntResource> shadowClasses = new ArrayList<OntResource>();
		ResIterator shadowExtensions = model.listSubjectsBuffered(UML.hasStereotype, UML.shadowextension);
		while (shadowExtensions.hasNext()) {
			OntResource shadowClass = shadowExtensions.nextResource();
			if (shadowClass.hasProperty(UML.hasStereotype, UML.cimdatatype)
					|| shadowClass.hasProperty(UML.hasStereotype, UML.primitive)) {
				shadowClasses.add(shadowClass);
				// System.err.println("Shadow Extension: " + shadowClass.describe());
			}
		}

		for (OntResource shadowClass : shadowClasses) {
			List<OntResource> subclasses = new ArrayList<OntResource>();
			ResIterator shadowClassSubclasses = model.listSubjectsWithProperty(RDFS.subClassOf, shadowClass);
			while (shadowClassSubclasses.hasNext()) {
				OntResource subclass = shadowClassSubclasses.nextResource();
				subclasses.add(subclass);
				// System.err.println("Shadow Extension: " + shadowClass.describe());
			}

			if (subclasses.size() > 1) {
				StringBuffer errorMsg = new StringBuffer();
				errorMsg.append("<<ShadowExtension>> class '") //
						.append(shadowClass.getLocalName()) //
						.append("' is invalid. A shadow extension may only shadow a single CIM class while '"
								+ shadowClass.getLocalName() + "' shadows ") //
						.append(subclasses.size()) //
						.append(" classes:  ");
				subclasses.forEach(subclass -> {
					errorMsg.append("  ").append(subclass.getURI()).append("\n");
				});
				System.err.println(errorMsg.toString());
			}

			for (OntResource aClass : subclasses) {
				if (!aClass.hasProperty(UML.hasStereotype, UML.shadowextension)) {
					// System.err.println(aClass.describe());
					List<OntResource> props = new ArrayList<OntResource>();
					if (shadowClass.hasProperty(UML.hasStereotype, UML.primitive)) {
						ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
							// System.err.println(prop.describe());
						}
					} else if (shadowClass.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
						ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
							// System.err.println(prop.describe());
						}
					} else {
						ResIterator funcProps = model.listSubjectsWithProperty(RDFS.domain, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
							// System.err.println(prop.describe());
						}
						/**
						 * for (OntResource prop : props) { if (prop.getRange() != null &&
						 * prop.getRange().equals(shadowClass)) { // We've reached this point because
						 * we've identified that the // association is a "self-reference" meaning that
						 * both the domain // and range are the same class. Therefore we must also remap
						 * the // range to be the normative class. prop.removeProperty(RDFS.range,
						 * shadowClass); prop.addRange(aClass); } // Remap the domain class from the
						 * extension to the normative class. prop.removeProperty(RDFS.domain,
						 * shadowClass); prop.addDomain(aClass); if (prop.getIsDefinedBy() != null)
						 * prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy()); if
						 * (aClass.getIsDefinedBy() != null)
						 * prop.addIsDefinedBy(aClass.getIsDefinedBy()); }
						 */
					}

					// aClass.removeSuperClass(shadowClass);
					// model.removeSubject(shadowClass);
				} else {
					StringBuffer errorMsg = new StringBuffer();
					errorMsg.append("<<ShadowExtension>> class '") //
							.append(shadowClass.getLocalName()) //
							.append("' is currently defined in the model as the shadow class of another shadow class: '") //
							.append(aClass.getLocalName()) //
							.append("' which is not allowed. Please correct the model and re-import.");
					// System.err.println(errorMsg.toString());
				}
			}
		}

		/**
		 * Pass two here addresses approach two described above. This pass expects that
		 * the 'baseuri' tagged values have already processed and propagated throughout
		 * the model.
		 */
		/**
		 * List<OntResource> normativeExtendedClasses = new LinkedList<OntResource>();
		 * Map<String, Set<OntResource>> shadowClassesMapping = new HashMap<String,
		 * Set<OntResource>>(); Set<String> validityChecks = new HashSet<String>();
		 * 
		 * ResIterator classesIterator = model.listSubjectsWithProperty(RDF.type,
		 * OWL2.Class ); while (classesIterator.hasNext()) { OntResource aClass =
		 * classesIterator.nextResource(); if (aClass.getURI().startsWith(baseURI) &&
		 * aClass.hasProperty(RDFS.subClassOf)) { ResIterator it =
		 * aClass.listProperties(RDFS.subClassOf); if (it.hasNext()) { Set<OntResource>
		 * shadowSuperClasses = new HashSet<OntResource>(); while (it.hasNext()) {
		 * OntResource aSuperClass = it.nextResource(); // Check if the superclass has
		 * an identical class name but in a // different namespace (i.e. a "shadow class
		 * extension") if (!aSuperClass.getURI().startsWith(baseURI) &&
		 * aSuperClass.getURI().endsWith("#" + aClass.getLabel())) {
		 * shadowSuperClasses.add(aSuperClass); if
		 * (!validityChecks.contains(aSuperClass.getURI())) {
		 * validityChecks.add(aSuperClass.getURI()); } else {
		 * System.err.println("Invalid shadow class extension modeling. The follow
		 * shadow class is either a duplicate or is modeled as the superclass of
		 * multiple normative CIM classes (which is now allowed): " +
		 * aSuperClass.getURI()); } } } if (shadowSuperClasses.size() > 0) {
		 * normativeExtendedClasses.add(aClass);
		 * shadowClassesMapping.put(aClass.getURI(), shadowSuperClasses); } } } }
		 * 
		 * normativeExtendedClasses.forEach(aClass -> { // Retrieve the set of shadow
		 * classes for the current normative class Set<OntResource> mappedShadowClasses
		 * = shadowClassesMapping.get(aClass.getURI());
		 * mappedShadowClasses.forEach(aSuperClass -> { List<OntResource> props = new
		 * ArrayList<OntResource>(); if (aSuperClass.hasProperty(UML.hasStereotype,
		 * UML.enumeration)) { ResIterator funcProps =
		 * model.listSubjectsWithProperty(RDF.type, aSuperClass); while
		 * (funcProps.hasNext()) { OntResource prop = funcProps.nextResource();
		 * props.add(prop); } for (OntResource prop : props) { // Remap the RDF type on
		 * the enum literal from // the extension to the normative class.
		 * prop.removeProperty(RDF.type, aSuperClass); prop.addRDFType(aClass); if
		 * (prop.getIsDefinedBy() != null) prop.removeProperty(RDFS.isDefinedBy,
		 * prop.getIsDefinedBy()); if (aClass.getIsDefinedBy() != null)
		 * prop.addIsDefinedBy(aClass.getIsDefinedBy()); } } else { ResIterator
		 * funcProps = model.listSubjectsWithProperty(RDFS.domain, aSuperClass); while
		 * (funcProps.hasNext()) { OntResource prop = funcProps.nextResource();
		 * props.add(prop); } for (OntResource prop : props) {
		 * System.err.println(prop.describe()); if (prop.getRange() != null &&
		 * prop.getRange().equals(aSuperClass)) { // We've reached this point because
		 * we've identified that the // association is a "self-reference" meaning that
		 * both the domain // and range are the same class. Therefore we must also remap
		 * the // range to be the normative class. prop.removeProperty(RDFS.range,
		 * aSuperClass); prop.addRange(aClass); } // Remap the domain class from the
		 * extension to the normative class. prop.removeProperty(RDFS.domain,
		 * aSuperClass); prop.addDomain(aClass); if (prop.getIsDefinedBy() != null)
		 * prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy()); if
		 * (aClass.getIsDefinedBy() != null)
		 * prop.addIsDefinedBy(aClass.getIsDefinedBy()); } }
		 * 
		 * aClass.removeSuperClass(aSuperClass); model.removeSubject(aSuperClass); });
		 * });
		 */
	}

	/**
	 * Method for special processing of a any "extensions" to the cim (i.e. all
	 * non-normative CIM classes, attributes, and associations).
	 * 
	 * Synthesize all attributes and associations defined on shadow extension
	 * classes into the normative class that they shadow. Note the following "rules"
	 * related to these classes:
	 * 
	 * 1. A shadow extension class may only have a single generalization
	 * relationship generalization which must always be to the normative CIM class it shadows.
	 * 
	 * 2. A normative CIM class could have multiple shadow classes defined as
	 * parents to it in which case each shadow class's attributes and associations
	 * need to be migrated into the normative CIM class. Note that duplicate
	 * attribute names are not allowed and that for all associations defined across
	 * such classes the rules defined for their naming must comply with the
	 * association and role-end rules defined within the official CIM Modeling
	 * Guidelines document. The "lens" through which their naming should be via
	 * evaluating all of them as if they were all defined on the normative CIM class
	 * for which they are shadowing. In this way it can be assured that role ends
	 * are not duplicated, etc.
	 * 
	 * The OWL representation of a UML class is modified in place below.
	 * 
	 * The first manner in which a "shadow class extension" can be modeled is with a
	 * name different than the class it "shadows" (e.g. ExtMyIdentifiedObject) but
	 * with a corresponding <<ShadowExtension>> stereotype assigned to the class.
	 * 
	 * In this way CIMTool is made aware that the class is a "shadow class "
	 * 
	 * ExtMyIdentifiedObject ExtEuIdentifiedObject △ △ | | ExtEuIdentifiedObject
	 * 
	 * The second manner in which to model a shadow class extension is to name the
	 * shadow class the same as the class it shadows but with the caveat that it be
	 * namespaced (via the CIMTool baseuri tagged value) in a different namespace.
	 * Using this technique you can have multiple sets of extensions from different
	 * sources each defining their own IdentifiedObject shadow class with attributes
	 * and associations that will be merged into the normative IdentifiedObject
	 * class.
	 * 
	 * IdentifiedObject IdentifiedObject (http://my1.com#) (http://my2.com#) △ △ | |
	 * IdentifiedObject (http://iec.ch/TC57/CIM100#)
	 * 
	 * @param baseURI The baseURI for CIM to use for processing.
	 */
	private void processExtensions(String baseURI) {
		/**
		 * Pass one here addresses approach one described in the method comments.
		 */
		List<OntResource> shadowClasses = new ArrayList<OntResource>();
		ResIterator shadowExtensions = model.listSubjectsBuffered(UML.hasStereotype, UML.shadowextension);
		while (shadowExtensions.hasNext()) {
			OntResource shadowClass = shadowExtensions.nextResource();
			shadowClasses.add(shadowClass);
			System.err.println("Shadow Extension:  " + shadowClass.describe());
		}

		for (OntResource shadowClass : shadowClasses) {
			List<OntResource> subclasses = new ArrayList<OntResource>();
			ResIterator shadowClassSubclasses = model.listSubjectsWithProperty(RDFS.subClassOf, shadowClass);
			while (shadowClassSubclasses.hasNext()) {
				OntResource subclass = shadowClassSubclasses.nextResource();
				subclasses.add(subclass);
			}

			if (subclasses.size() > 1) {
				StringBuffer errorMsg = new StringBuffer();
				errorMsg.append("<<ShadowExtension>> class '") //
						.append(shadowClass.getLocalName()) //
						.append("' is invalid. A shadow extension may only shadow a single CIM class while '"
								+ shadowClass.getLocalName() + "' shadows ") //
						.append(subclasses.size()) //
						.append(" classes:  ");
				subclasses.forEach(subclass -> {
					errorMsg.append("  ").append(subclass.getURI()).append("\n");
				});
				System.err.println(errorMsg.toString());
				logger.log(errorMsg.toString());
				// TODO: throw an Exception here ??
			}

			for (OntResource aClass : subclasses) {
				if (!aClass.hasProperty(UML.hasStereotype, UML.shadowextension)) {
					List<OntResource> props = new ArrayList<OntResource>();
					if (shadowClass.hasProperty(UML.hasStereotype, UML.enumeration)) {
						ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
						}
						for (OntResource prop : props) {
							// Remap the RDF type on the enum literal from
							// the extension to the normative class.
							prop.removeProperty(RDF.type, shadowClass);
							prop.addRDFType(aClass);
							if (prop.getIsDefinedBy() != null)
								prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
							if (aClass.getIsDefinedBy() != null)
								prop.addIsDefinedBy(aClass.getIsDefinedBy());
						}
					} else if (shadowClass.hasProperty(UML.hasStereotype, UML.primitive)) {
						ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
							System.err.println(prop.describe());
						}
					} else if (shadowClass.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
						ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
							System.err.println(prop.describe());
						}
					} else {
						ResIterator funcProps = model.listSubjectsWithProperty(RDFS.domain, shadowClass);
						while (funcProps.hasNext()) {
							OntResource prop = funcProps.nextResource();
							props.add(prop);
						}
						for (OntResource prop : props) {
							if (prop.getRange() != null && prop.getRange().equals(shadowClass)) {
								// We've reached this point because we've identified that the
								// association is a "self-reference" meaning that both the domain
								// and range are the same class. Therefore we must also remap the
								// range to be the normative class.
								prop.removeProperty(RDFS.range, shadowClass);
								prop.addRange(aClass);
							}
							// Remap the domain class from the extension to the normative class.
							prop.removeProperty(RDFS.domain, shadowClass);
							prop.addDomain(aClass);
							if (prop.getIsDefinedBy() != null)
								prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
							if (aClass.getIsDefinedBy() != null)
								prop.addIsDefinedBy(aClass.getIsDefinedBy());
						}
					}

					aClass.removeSuperClass(shadowClass);
					model.removeSubject(shadowClass);
				}
			}
		}

		/**
		 * Pass two here addresses approach two described above. This pass expects that
		 * the 'baseuri' tagged values have already processed and propagated throughout
		 * the model.
		 */
		List<OntResource> normativeExtendedClasses = new LinkedList<OntResource>();
		Map<String, Set<OntResource>> shadowClassesMapping = new HashMap<String, Set<OntResource>>();
		Set<String> validityChecks = new HashSet<String>();

		ResIterator classesIterator = model.listSubjectsWithProperty(RDF.type, OWL2.Class);
		while (classesIterator.hasNext()) {
			OntResource aClass = classesIterator.nextResource();
			if (aClass.getURI().startsWith(baseURI) && aClass.hasProperty(RDFS.subClassOf)) {
				ResIterator it = aClass.listProperties(RDFS.subClassOf);
				if (it.hasNext()) {
					Set<OntResource> shadowSuperClasses = new HashSet<OntResource>();
					while (it.hasNext()) {
						OntResource aSuperClass = it.nextResource();
						// Check if the superclass has an identical class name but in a
						// different namespace (i.e. a "shadow class extension")
						if (!aSuperClass.getURI().startsWith(baseURI)
								&& aSuperClass.getURI().endsWith("#" + aClass.getLabel())) {
							shadowSuperClasses.add(aSuperClass);
							if (!validityChecks.contains(aSuperClass.getURI())) {
								validityChecks.add(aSuperClass.getURI());
							} else {
								System.err.println(
										"Invalid shadow class extension modeling. The follow shadow class is either a duplicate or is modeled as the superclass of multiple normative CIM classes (which is now allowed): "
												+ aSuperClass.getURI());
							}
						}
					}
					if (shadowSuperClasses.size() > 0) {
						normativeExtendedClasses.add(aClass);
						shadowClassesMapping.put(aClass.getURI(), shadowSuperClasses);
					}
				}
			}
		}

		normativeExtendedClasses.forEach(aClass -> {
			// Retrieve the set of shadow classes for the current normative class
			Set<OntResource> mappedShadowClasses = shadowClassesMapping.get(aClass.getURI());
			mappedShadowClasses.forEach(aSuperClass -> {
				List<OntResource> props = new ArrayList<OntResource>();
				if (aSuperClass.hasProperty(UML.hasStereotype, UML.enumeration)) {
					ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, aSuperClass);
					while (funcProps.hasNext()) {
						OntResource prop = funcProps.nextResource();
						props.add(prop);
					}
					for (OntResource prop : props) {
						// Remap the RDF type on the enum literal from
						// the extension to the normative class.
						prop.removeProperty(RDF.type, aSuperClass);
						prop.addRDFType(aClass);
						if (prop.getIsDefinedBy() != null)
							prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
						if (aClass.getIsDefinedBy() != null)
							prop.addIsDefinedBy(aClass.getIsDefinedBy());
					}
				} else {
					ResIterator funcProps = model.listSubjectsWithProperty(RDFS.domain, aSuperClass);
					while (funcProps.hasNext()) {
						OntResource prop = funcProps.nextResource();
						props.add(prop);
					}
					for (OntResource prop : props) {
						System.err.println(prop.describe());
						if (prop.getRange() != null && prop.getRange().equals(aSuperClass)) {
							// We've reached this point because we've identified that the
							// association is a "self-reference" meaning that both the domain
							// and range are the same class. Therefore we must also remap the
							// range to be the normative class.
							prop.removeProperty(RDFS.range, aSuperClass);
							prop.addRange(aClass);
						}
						// Remap the domain class from the extension to the normative class.
						prop.removeProperty(RDFS.domain, aSuperClass);
						prop.addDomain(aClass);
						if (prop.getIsDefinedBy() != null)
							prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
						if (aClass.getIsDefinedBy() != null)
							prop.addIsDefinedBy(aClass.getIsDefinedBy());
					}
				}

				aClass.removeSuperClass(aSuperClass);
				model.removeSubject(aSuperClass);
			});
		});
	}
}
