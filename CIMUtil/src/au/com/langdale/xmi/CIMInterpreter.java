/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
			processShadowExtensions(baseURI);
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
	 * Method for special processing of a any "shadow extensions" to the CIM 
	 * (i.e. all non-normative CIM classes, attributes, and associations).
	 * 
	 * Merges all attributes and associations defined on "shadow classes"
	 * into the normative class that they shadow. Note the following rules
	 * related to these classes:
	 * 
	 * 1. A shadow extension class may only have a single generalization
	 * relationship which must always be to the normative CIM class it shadows.
	 * 
	 * 2. A normative CIM class could have multiple shadow classes defined as
	 * parents to it in which case each shadow class's attributes and associations
	 * need to be migrated into the normative CIM class. Note that duplicate
	 * attribute names are not allowed and that for all associations defined across
	 * such classes the rules defined for their naming must comply with the
	 * association and role-end rules defined within the official CIM Modeling
	 * Guidelines document. The "lens" through which their naming should be via
	 * evaluating all of them as if they were all defined on the normative CIM 
	 * class for which they are shadowing. In this way it can be assured that 
	 * role ends are not duplicated, etc.
	 * 
	 * The OWL representation of a UML class is modified in place below.
	 * 
	 * The first manner in which a "shadow class" can be modeled is with a
	 * name different than the class it "shadows" (e.g. ExtMyIdentifiedObject) 
	 * but with a corresponding <<ShadowExtension>> stereotype declared on the 
	 * class.
	 * 
	 * In this way CIMTool is made aware that the class is a "shadow class"
	 * 
	 * <pre>
	 *  <<ShadowExtension>>    <<ShadowExtension>>
	 * ExtMyIdentifiedObject  ExtEuIdentifiedObject 
	 *                   △      △ 
	 *                   |      | 
	 *              ExtEuIdentifiedObject
	 * </pre>
	 * 
	 * The second manner in which to model a shadow class is to name the shadow
	 * class the same as the CIM class it is shadowing but with the caveat that 
	 * it be namespaced (via the CIMTool baseuri tagged value) in a non-CIM 
	 * namespace. Using this technique you can have multiple sets of extensions 
	 * from different sources each defining their own IdentifiedObject shadow 
	 * class with attributes and associations that will be merged into the 
	 * normative IdentifiedObject class.
	 * 
	 * <pre>
	 *  IdentifiedObject   IdentifiedObject 
	 * (http://my1.com#)   (http://my2.com#) 
	 *              △       △ 
	 *              |       |      
	 *           IdentifiedObject    
	 *      (http://iec.ch/TC57/CIM100#)
	 * </pre>
	 * 
	 * @param baseURI The baseURI of the CIM schema. Needed for processing.
	 */
	private void processShadowExtensions(String baseURI) {
		Map<String, OntResource> shadowClasses = getShadowClasses(baseURI);

		// Validate each shadow class for any modeling violations...
		shadowClasses.values().forEach(shadowClass -> {
			validateShadowExtensionsModeling(baseURI, shadowClass);
		});

		List<OntResource> normativeClasses = new LinkedList<OntResource>();
		ResIterator allClasses = model.listSubjectsBuffered(RDF.type, OWL2.Class);
		allClasses.forEachRemaining(resource -> {
			OntResource aClass = (OntResource) resource;
			if (aClass.getURI().startsWith(baseURI) && aClass.hasProperty(RDFS.subClassOf)) {
				normativeClasses.add(aClass);
			}
		});

		normativeClasses.forEach(normativeClass -> {
			System.out.println("Processing normative CIM class:  " + normativeClass.describe());
			
			List<OntResource> parentShadowClasses = new ArrayList<OntResource>();
			ResIterator parentClasses = normativeClass.listSuperClasses(false);
			while (parentClasses.hasNext()) {
				OntResource superClass = parentClasses.nextResource();
				if (shadowClasses.containsKey(superClass.getURI())) {
					parentShadowClasses.add(superClass);					
				}
			}
			parentShadowClasses.forEach(aParentShadowClass -> {
				mergeShadowClass(aParentShadowClass, normativeClass, shadowClasses, baseURI, true);
			});
		});
		
//		normativeClasses.forEach(normativeClass -> {
//			System.out.println("Pass 2 - processing normative CIM class:  " + normativeClass.describe());
//			
//			List<OntResource> parentShadowClasses = new ArrayList<OntResource>();
//			ResIterator parentClasses = normativeClass.listSuperClasses(false);
//			while (parentClasses.hasNext()) {
//				OntResource superClass = parentClasses.nextResource();
//				if (shadowClasses.containsKey(superClass.getURI())) {
//					parentShadowClasses.add(superClass);					
//				}
//			}
//			parentShadowClasses.forEach(aParentShadowClass -> {
//				ResIterator it = model.listSubjectsWithProperty(OWL2.inverseOf, aParentShadowClass);
//				while (it.hasNext()) {
//					OntResource subject = it.nextResource();
//					System.err.println("BEFORE inverseof replacement:  " + subject.describe());
//					subject.removeProperty(OWL2.inverseOf, aParentShadowClass);
//					subject.addProperty(OWL2.inverseOf, normativeClass);
//					System.err.println("AFTER inverseof replacement:  " + subject.describe());
//				}
//				normativeClass.removeSuperClass(aParentShadowClass);
//			});
//		});

		shadowClasses.values().forEach(shadowClass -> {
//			ResIterator it = model.listSubjectsWithProperty(OWL2.inverseOf, shadowClass);
//			while (it.hasNext()) {
//				OntResource subject = it.nextResource();
//				System.err.println("OUTSTANDING inverseof:  " + subject.describe());
//			}
//			// The final step is to remove each shadow class from the model.
//			// The below call removes it both as a Subject and an Object.
			shadowClass.remove();
		});
	}

	/**
	 * Method responsible for merging the attributes and/or associations from a
	 * specified "shadow class" to a specified target CIM class.
	 * 
	 * @param shadowClass The shadow class containing the attributes and/or
	 *                    associations to be merged.
	 * @param targetClass The target CIM class to which the shadow class extensions
	 *                    will be merged into.
	 */
	private void mergeShadowClass(OntResource shadowClass, OntResource normativeClass,
			Map<String, OntResource> shadowClasses, String baseURI, boolean isShadowClassDirectParent) {
		
		System.out.println("Merging shadow class:  " + shadowClass.describe());
		
		ArrayList<OntResource> props = new ArrayList<OntResource>();
		
		if (shadowClass.hasProperty(UML.hasStereotype, UML.enumeration)) {
			ResIterator funcProps = model.listSubjectsBuffered(RDF.type, shadowClass);
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
			}
			for (OntResource prop : props) {
				// Remap the RDF type on the enum literal from
				// the extension to the normative class.
				prop.removeProperty(RDF.type, shadowClass);
				prop.addRDFType(normativeClass);
				if (prop.getIsDefinedBy() != null)
					prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
				if (normativeClass.getIsDefinedBy() != null)
					prop.addIsDefinedBy(normativeClass.getIsDefinedBy());
			}
		} else if (shadowClass.hasProperty(UML.hasStereotype, UML.primitive)) {
			/** This is a placeholder for now. Currently just prints. Must be implemented/tested */
			ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
				if (TRACE)
					System.err.println("Shadow class <<Primitive>>:  " + prop.describe());
			}
		} else if (shadowClass.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
			/** This is a placeholder for now. Currently just prints. Must be implemented/tested */
			ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
				if (TRACE)
					System.err.println("Shadow class <<CIMDatatype>>:  " + prop.describe());
			}
		} else {
			ResIterator funcProps = model.listSubjectsWithProperty(RDFS.domain, shadowClass);
			
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
				System.err.println("Including property:   " + prop.getURI());
			}
			
			for (OntResource prop : props) {
				if (prop.getRange() != null && prop.getRange().equals(shadowClass)) {
					// We've reached this point because we've identified that the
					// association is a "self-reference" meaning that both the domain
					// and range are the same class. Therefore we must also remap the
					// range to be the normative class.
					prop.removeProperty(RDFS.range, shadowClass);
					prop.addRange(normativeClass);
				}
				// Remap the domain class from the extension to the normative class.
				prop.removeProperty(RDFS.domain, shadowClass);
				prop.addDomain(normativeClass);
				if (prop.getIsDefinedBy() != null)
					prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
				if (normativeClass.getIsDefinedBy() != null)
					prop.addIsDefinedBy(normativeClass.getIsDefinedBy());
				
				if (prop.getInverseOf() != null) {
					System.err.println(prop.describe());
					OntResource inverseOf = prop.getInverseOf();
					System.err.println(inverseOf.describe());
				}
			}
		}
		
		/**
		 * Once the shadow class is been processed we remove its  
		 * super class reference in the normative CIM class...
		 */
		if (isShadowClassDirectParent) {
			normativeClass.removeSuperClass(shadowClass);
		}

		/**
		 * Iterate through the immediate super classes for the current 
		 * "shadow class" being processed...
		 */
		ResIterator superClasses = shadowClass.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			/**
			 * Starting at the CIM class we now recursively navigate up the inheritance
			 * hierarchy and merge the extensions from each level into the specified
			 * normative class.
			 */
			if (superClass.hasProperty(UML.hasStereotype, UML.shadowextension)
					&& shadowClasses.containsKey(superClass.getURI())) {
				mergeShadowClass(superClass, normativeClass, shadowClasses, baseURI, false);
			} else {
				if (TRACE)
					System.err.println("ERROR:  " + superClass.describe());
			}
		}
	}
	
	/**
	 * Perform extensions modeling validation checks and log all violations.
	 *
	 * @param baseURI
	 * @param shadowClass
	 */
	private void validateShadowExtensionsModeling(String baseURI, OntResource shadowClass) {

		String shadowClassURI = shadowClass.getURI().substring(0, shadowClass.getURI().indexOf("#") + 1);

		boolean isValidModeling = true;
		int totalSuperClasses = 0;
		int superClassesWithBaseURI = 0;
		int superClassesWithShadowExtension = 0;
		int superClassesWithMatchingURI = 0;
		int superClassesWithNonMatchingURI = 0;

		ResIterator shadowClassSuperClasses = shadowClass.listProperties(RDFS.subClassOf);
		while (shadowClassSuperClasses.hasNext()) {
			OntResource superclass = shadowClassSuperClasses.nextResource();
			String superClassURI = superclass.getURI().substring(0, superclass.getURI().indexOf("#") + 1);

			totalSuperClasses++;
			if (superclass.hasProperty(UML.hasStereotype, UML.shadowextension))
				superClassesWithShadowExtension++;

			if (!superClassURI.equals(shadowClassURI))
				superClassesWithNonMatchingURI++;
			else
				superClassesWithMatchingURI++;

			if (superClassURI.startsWith(baseURI)) {
				isValidModeling = false;
				System.err.println(String.format(
						"[ERROR] Invalid extensions modeling. Shadow class '%s' may not extend the normative CIM class '%s'.",
						shadowClass.getURI(), superclass.getURI()));
			} else if (!superclass.hasProperty(UML.hasStereotype, UML.shadowextension)) {
				isValidModeling = false;
				System.err.println(String.format(
						"[ERROR] Invalid extensions modeling. Shadow class '%s' may not extend a class that is not another shadow class CIM class '%s'.",
						shadowClass.getURI(), superclass.getURI()));
			} else if (!superClassURI.equals(shadowClassURI)) {
				isValidModeling = false;
				System.err.println(String.format(
						"[ERROR] Invalid extensions modeling. The parent shadow class '%s' of shadow class '%s' is not in the same namespace.",
						superclass.getURI(), shadowClass.getURI()));
			}
		}

		int totalSubClasses = 0;
		int subClassesWithBaseURI = 0;
		int subClassesWithShadowExtension = 0;
		int subClassesWithMatchingURI = 0;
		int subClassesWithNonMatchingURI = 0;

		ResIterator shadowClassSubClasses = model.listSubjectsWithProperty(RDFS.subClassOf, shadowClass);
		while (shadowClassSubClasses.hasNext()) {
			OntResource subclass = shadowClassSubClasses.nextResource();
			String subClassURI = subclass.getURI().substring(0, subclass.getURI().indexOf("#") + 1);

			totalSubClasses++;
			if (subclass.hasProperty(UML.hasStereotype, UML.shadowextension))
				subClassesWithShadowExtension++;

			if (subClassURI.equals(baseURI))
				subClassesWithBaseURI++;
			else if (!subClassURI.equals(shadowClassURI))
				subClassesWithNonMatchingURI++;
			else
				subClassesWithMatchingURI++;

			if (subclass.hasProperty(UML.hasStereotype, UML.shadowextension) && !subClassURI.equals(baseURI)
					&& !subClassURI.equals(shadowClassURI)) {
				isValidModeling = false;
				System.err.println(String.format(
						"[ERROR] Invalid extensions modeling. The child shadow class '%s' of shadow class '%s' is not in the same namespace.",
						subclass.getURI(), shadowClass.getURI()));
			} else if (!subclass.hasProperty(UML.hasStereotype, UML.shadowextension) && !subClassURI.equals(baseURI)) {
				isValidModeling = false;
				System.err.println(String.format(
						"[ERROR] Invalid extensions modeling. The child class '%s' of shadow class '%s' is not a normative CIM class.",
						subclass.getURI(), shadowClass.getURI()));
			}
		}

		if (totalSubClasses > 1 && subClassesWithBaseURI > 1) {
			isValidModeling = false;
			System.err.println(String.format(
					"[ERROR] Invalid extensions modeling. Shadow class '%s' is the parent of %d normative CIM classes when only one is allowed.",
					shadowClass.getURI(), subClassesWithBaseURI));
		} else if ((!((totalSubClasses == 1 && subClassesWithBaseURI == 1) && (totalSuperClasses == 0))) //
				&& (!((totalSubClasses == 1 && subClassesWithBaseURI == 1)
						&& (totalSuperClasses > 0 && superClassesWithMatchingURI == totalSuperClasses)))) {
			isValidModeling = false;
			System.err.println(String.format(
					"[ERROR] Invalid extensions modeling. Shadow class '%s' and its parent and child classes should be reviewed.",
					shadowClass.getURI()));
		}

		if (isValidModeling) {
			/**
			 * Final validation checks on modeled associations are needed for the current
			 * "shadow class".
			 */
			ResIterator props = model.listSubjectsWithProperty(RDFS.domain, shadowClass);
			while (props.hasNext()) {
				OntResource property = props.nextResource();
				if (property.hasProperty(RDF.type, OWL.ObjectProperty)) {
					//System.out.println("Property:  " + property.describe());
					OntResource range = property.getRange(); // The "other end" of the association
					String rangeURI = range.getURI().substring(0, range.getURI().indexOf("#") + 1);
					if (!shadowClassURI.equals(rangeURI) && !rangeURI.equals(baseURI)) {
						isValidModeling = false;
						System.err.println(String.format(
								"[ERROR] Invalid extensions modeling. Shadow class '%s' has association '%s' that does not have the same namespace on both ends ['%s']. This is not allowed and the modeling should be reviewed.",
								shadowClass.getURI(), property.getURI(), range.getURI()));
					} else if (!shadowClassURI.equals(rangeURI)) {
						isValidModeling = false;
						System.err.println(String.format(
								"[ERROR] Invalid extensions modeling. Shadow class '%s' has association '%s' that does not have the same namespace on both ends ['%s']. This is not allowed and the modeling should be reviewed.",
								shadowClass.getURI(), property.getURI(), range.getURI()));
					}
				}
			}
		}
		/*
		 * else if (totalSuperClasses == 0 && (totaltotalSubClasses = ()))) { // //
		 * validation must be checked... ResIterator props =
		 * model.listSubjectsWithProperty(RDFS.domain, shadowClass); while
		 * (props.hasNext()) { OntResource property = props.nextResource(); if
		 * (property.hasProperty(RDF.type, OWL.ObjectProperty)) {
		 * System.out.println("Property:  " + property.describe()); OntResource range =
		 * property.getRange(); // The "other end" of the association String rangeURI =
		 * range.getURI().substring(0, range.getURI().indexOf("#") + 1); if
		 * (!shadowClassURI.equals(rangeURI) && !rangeURI.equals(baseURI)) {
		 * isValidModeling = false; System.err.println(String.
		 * format("[ERROR] Invalid extensions modeling. Shadow class '%s' has association '%s' that does not have the same namespace on both ends ['%s']. This is not allowed and the modeling should be reviewed."
		 * , shadowClass.getURI(), property.getURI(), range.getURI())); } else if
		 * (!shadowClassURI.equals(rangeURI)) { isValidModeling = false;
		 * System.err.println(String.
		 * format("[ERROR] Invalid extensions modeling. Shadow class '%s' has association '%s' that does not have the same namespace on both ends ['%s']. This is not allowed and the modeling should be reviewed."
		 * , shadowClass.getURI(), property.getURI(), range.getURI())); } } } }
		 */
	}

	/**
	 * This method is responsible for retrieving the complete list of "shadow
	 * classes" that exists in the full CIM model this is being used as the schema
	 * for the project.
	 * 
	 * The list is determined by combining both a "bottom-up" and "top-down" query.
	 * This is necessary to retrieve a complete list.
	 * 
	 * @param baseURI The base URI of the CIM schema (e.g.
	 *                http://iec.ch/TC57/CIM100#)
	 * @return A map contained the list of shadow classes with the key being the
	 *         absolute URI of the mapped shadow class.
	 */
	private Map<String, OntResource> getShadowClasses(String baseURI) {
		/**
		 * The "bottom-up" step is performed first whereby all classes are queried and
		 * iterated through to locate each of the normative CIM classes. When one is
		 * found that has parent classes, each parent classes is checked to determine
		 * whether it is a "shadow class" and if so it is added to the list.
		 */
		Map<String, OntResource> shadowClasses = new HashMap<String, OntResource>();
		ResIterator classesIterator = model.listSubjectsBuffered(RDF.type, OWL2.Class);
		while (classesIterator.hasNext()) {
			OntResource aClass = classesIterator.nextResource();
			if (aClass.getURI().startsWith(baseURI) && aClass.hasProperty(RDFS.subClassOf)) {
				if (TRACE)
					System.out.println("CIM classes:  " + aClass.describe());
				ResIterator it = aClass.listProperties(RDFS.subClassOf);
				while (it.hasNext()) {
					OntResource aSuperClass = it.nextResource();
					if (TRACE)
						System.out.println("CIM superclass:  " + aSuperClass.describe());
					/**
					 * <pre>
					 * Check for parent classes that are shadow extensions. There are 
					 * two types of checks needed:
					 * 
					 * 1. If there is an explicit <<ShadowExtension>> stereotype on the 
					 *    parent class.
					 * 
					 * 2. If the parent is named identically to the CIM class but is 
					 *    defined in a different namespace (i.e. by definition a 
					 *    "shadow class").
					 * </pre>
					 */
					if ((aSuperClass.hasProperty(UML.hasStereotype, UML.shadowextension) && (!aSuperClass.getURI().startsWith(baseURI))) || //
							(!aSuperClass.getURI().startsWith(baseURI)
									&& aSuperClass.getURI().endsWith("#" + aClass.getLabel()))) {
						if (TRACE)
							System.err.println("Shadow class:  " + aSuperClass.describe());
						shadowClasses.put(aSuperClass.getURI(), aSuperClass);
					}
				}
			}
		}

		/**
		 * The "top-down" step is performed second whereby just classes with a
		 * <<ShadowExtension>> stereotype are queried and iterated through and a check
		 * performed to determined if they were NOT identified during the "bottom-up"
		 * pass. 
		 * 
		 * Note that we also check to ensure that namespace is not that of baseURI 
		 * (i.e. the namespace for the normative CIM). Since <<ShadowExtension>> 
		 * stereotypes have no relevance on normative CIM classes it would then indicate
		 * a scenario where a baseuri tagged value was not specified at an appropriate place 
		 * in the model and this instead would have been logged as a CIM modeling violation
		 * during the model validation process. If the above criteria is met then the "shadow 
		 * class" is added to the list of shadow classes. This second step is needed since 
		 * the first step only verifies the immediate parents. That pass does not take into 
		 * consideration that a "shadow class" itself can potentially have a parent "shadow 
		 * class" but with the caveat that such classes MUST HAVE the <<ShadowExtension>> 
		 * stereotype defined on it.
		 */
		ResIterator shadowClassesIterator = model.listSubjectsBuffered(UML.hasStereotype, UML.shadowextension);
		while (shadowClassesIterator.hasNext()) {
			OntResource aShadowClass = shadowClassesIterator.nextResource();
			if (!shadowClasses.containsKey(aShadowClass.getURI()) && !aShadowClass.getURI().startsWith(baseURI)) {
				if (TRACE)
					System.err.println("Shadow class:  " + aShadowClass.describe());
				shadowClasses.put(aShadowClass.getURI(), aShadowClass);
			}
		}
		return shadowClasses;
	}

}
