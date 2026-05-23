/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.easyrules.engine.CIMModellingGuideRulesValidator;
import au.com.langdale.easyrules.engine.ModelRulesValidator;
import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.kena.NodeIterator;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Extends the generic XMI interpreter to apply IEC CIM modelling conventions.
 *
 */
class CIMInterpreterImpl extends UMLInterpreter implements CIMInterpreter {

	private static final Logger log = LoggerFactory.getLogger(CIMInterpreterImpl.class);

	CIMInterpreterImpl() {
		super();
	}

	CIMInterpreterImpl(StereotypedNamespaces stereotypedNamespaces) {
		super(stereotypedNamespaces);
	}

	@Override
	public CIMInterpreterResult interpret( //
			OntModel raw, //
			String baseURI, //
			OntModel annote, //
			boolean usePackageNames, //
			boolean mergeShadowExtensions, //
			boolean validateModel) {

		if (annote != null)
			raw.add(annote);

		return postProcess(raw, baseURI, usePackageNames, mergeShadowExtensions, validateModel);
	}

	private CIMInterpreterResult postProcess(OntModel raw, String baseURI, boolean usePackageNames,
			boolean mergeShadowExtensions, boolean validateModel) {
		setModel(raw);

		log.debug("Raw XMI model size: {}", getModel().size());

		UML.loadOntology(getModel());
		pruneIncomplete();
		labelRoles();

		log.debug("Stage 1 XMI model size: {}", getModel().size());

		Translator translator = new Translator(getModel(), stereotypedNamespaces, baseURI, usePackageNames);
		translator.run();
		setModel(translator.getModel());

		log.debug("Stage 3 XMI model size: {}", getModel().size());

		propagateComments();
		applyStereotypes(baseURI);
		classifyAttributes();
		removeUntyped();

		/**
		 * At this precise point we want to perform model validation for compliance with
		 * the CIM Modelling Guide. We do this before any potential merging of shadow
		 * class extensions into the model.
		 */
		List<RuleViolation> violations = new ArrayList<>();
		if (validateModel) {
			ModelRulesValidator modelValidator = new CIMModellingGuideRulesValidator(baseURI);
			violations = modelValidator.validate(model);
		}

		if (mergeShadowExtensions) {
			processShadowExtensions(baseURI);
			ExtensionsTranslator extensionsTranslator = new ExtensionsTranslator(getModel(), baseURI);
			extensionsTranslator.run();
			setModel(extensionsTranslator.getModel());
		}

//		convertToSubClassOf("extensionMSITE");
		createOntologyHeader(baseURI);

		log.debug("Stage 4 XMI model size: {}", getModel().size());

		return new CIMInterpreterResult(getModel(), violations);
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
		ResIterator jt = model.listSubjectsBuffered(UML.hasStereotype, UML.enumeration);
		while (jt.hasNext()) {
			applyEnumerationStereotype(jt.nextResource());
		}

		ResIterator cp = model.listSubjectsBuffered(UML.hasStereotype, UML.constrainedprimitive);
		while (cp.hasNext()) {
			applyConstrainedPrimitiveStereotype(cp.nextResource());
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
			 * with other resources declared as type OWL.Class. This is still not a "first
			 * class citizen" from an ontological perspective as we desire in the long term,
			 * but it is the interim path we've chosen until we can implement the future
			 * path.
			 */
			// clss.removeProperties();
			clss.removeProperty(RDF.type, OWL.Class);
		} else {
			// for defined datatypes, establish an RDFS datatype
			clss.removeAll(RDF.type);
			clss.addProperty(RDF.type, RDFS.Datatype);

			if (log.isDebugEnabled()) {
				log.debug("Converted '{}' to rdfs:Datatype (stereotype: CIMDatatype/primitive/datatype)",
						clss.getLabel() != null ? clss.getLabel() : clss.getLocalName());
			}
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
	 * Covert primitive or union stereotyped class as a datatype.
	 */
	private void applyConstrainedPrimitiveStereotype(OntResource clss) {

		NodeIterator cardIter = clss.listLiteralProperties(UML.baseType);
		while (cardIter.hasNext()) {
			Object o = cardIter.next();
		}
		//
		NodeIterator lengthIter = clss.listLiteralProperties(XSDFacets.length);
		while (lengthIter.hasNext()) {
			Object o = lengthIter.next();
		}
		//
		NodeIterator it = clss.listLiteralProperties(XSDFacets.minLength);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.maxLength);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.minInclusive);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.maxInclusive);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.minExclusive);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.maxExclusive);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.whiteSpace);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.pattern);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.enumeration);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.totalDigits);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}
		it = clss.listLiteralProperties(XSDFacets.fractionDigits);
		while (it.hasNext()) {
			Node n = it.nextNode();
			log.debug("{}", n);
		}

		// for defined datatypes, establish an RDFS datatype
		// clss.removeAll(RDF.type);
		// clss.addProperty(RDF.type, RDFS.Datatype);
		//
//		if (truetype != null && !clss.equals(truetype)) {
//			clss.addProperty(OWL.equivalentClass, truetype);
//		}
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
		model.remove(clss, UML.hasStereotype, UML.constrainedprimitive);

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

	/**
	 * Method for special processing of any "shadow extensions" to the CIM (i.e. all
	 * non-normative CIM classes, attributes, and associations).
	 * 
	 * Merges all attributes and associations defined on "shadow classes" into the
	 * normative class that they shadow. Note the following rules related to these
	 * classes:
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
	 * evaluating all of them as if they were all defined on the normative CIM class
	 * for which they are shadowing. In this way it can be assured that role ends
	 * are not duplicated, etc.
	 * 
	 * The OWL representation of a UML class is modified in place below.
	 * 
	 * The first manner in which a "shadow class" can be modeled is with a name
	 * different than the class it "shadows" (e.g. ExtMyIdentifiedObject) but with a
	 * corresponding <<ShadowExtension>> stereotype declared on the class.
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
	 * class the same as the CIM class it is shadowing but with the caveat that it
	 * be namespaced (via the CIMTool baseuri tagged value) in a non-CIM namespace.
	 * Using this technique you can have multiple sets of extensions from different
	 * sources each defining their own IdentifiedObject shadow class with attributes
	 * and associations that will be merged into the normative IdentifiedObject
	 * class.
	 * 
	 * <pre>
	 *  IdentifiedObject   IdentifiedObject 
	 * (http://my1.com#)   (http://my2.com#) 
	 *              △       △ 
	 *              |       |      
	 *           IdentifiedObject    
	 *      (http://www.ucaiug.org/CIM100#)
	 * </pre>
	 * 
	 * @param baseURI The baseURI of the CIM schema. Needed for processing.
	 */
	private void processShadowExtensions(String baseURI) {
		Map<String, OntResource> shadowClasses = ShadowClassUtils.getShadowClasses(model, baseURI);

		List<OntResource> normativeClasses = new LinkedList<OntResource>();
		ResIterator allClasses = model.listSubjectsBuffered(RDF.type, OWL2.Class);
		allClasses.forEachRemaining(resource -> {
			OntResource aClass = (OntResource) resource;
			if (aClass.getURI().startsWith(baseURI) && aClass.hasProperty(RDFS.subClassOf)) {
				normativeClasses.add(aClass);
			}
		});

		normativeClasses.forEach(normativeClass -> {
			if (log.isTraceEnabled())
				log.trace("Processing normative CIM class:  {}", normativeClass.describe());

			List<OntResource> parentShadowClasses = new ArrayList<OntResource>();
			ResIterator parentClasses = normativeClass.listSuperClasses(false);
			while (parentClasses.hasNext()) {
				OntResource superClass = parentClasses.nextResource();
				if (shadowClasses.containsKey(superClass.getString(UML.id))) {
					parentShadowClasses.add(superClass);
				}
			}
			parentShadowClasses.forEach(aParentShadowClass -> {
				mergeShadowClass(aParentShadowClass, normativeClass, shadowClasses, baseURI, true);
			});
		});

		shadowClasses.values().forEach(shadowClass -> {
			// The final step is to remove each shadow class from the model.
			// The below call removes it both as a Subject and an Object.

			List<OntResource> axiomsToRemove = new ArrayList<>();

			Property[] axiomRefs = { OWL2.annotatedSource, OWL2.annotatedTarget };

			// We must first remove all axioms. Axioms are how stereotypes (assigned to a
			// generalization between two classes in the UML model) are expressed in OWL.
			for (Property prop : axiomRefs) {
				ResIterator axioms = model.listSubjectsWithProperty(prop, shadowClass);
				while (axioms.hasNext()) {
					axiomsToRemove.add(axioms.nextResource());
				}
			}

			// Now remove them
			for (OntResource axiom : axiomsToRemove) {
				axiom.removeProperties();
			}

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

		if (log.isTraceEnabled())
			log.trace("Merging shadow class:  {}", shadowClass.describe());

		if (log.isDebugEnabled()) {
			log.debug("Merging shadow class '{}' into normative class '{}'",
					shadowClass.getLabel() != null ? shadowClass.getLabel() : shadowClass.getLocalName(),
					normativeClass.getLabel() != null ? normativeClass.getLabel() : normativeClass.getLocalName());
		}

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
		} else if (shadowClass.hasProperty(UML.hasStereotype, UML.primitive)
				|| shadowClass.hasProperty(UML.hasStereotype, UML.constrainedprimitive)) {
			/**
			 * This is a placeholder for now. Currently just prints. Must be determine if we
			 * need to support extensions of Primtives. The initial thought is no.
			 */
			ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
				if (log.isTraceEnabled())
					log.trace("Shadow class <<Primitive>>:  {}", prop.describe());
			}
		} else if (shadowClass.hasProperty(UML.hasStereotype, UML.cimdatatype)) {
			/**
			 * This is a placeholder for now. Currently just prints. Must be determine if we
			 * need to support extensions of CIMDatatypes. The initial thought is no.
			 */
			ResIterator funcProps = model.listSubjectsWithProperty(RDF.type, shadowClass);
			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
				if (log.isTraceEnabled())
					log.trace("Shadow class <<CIMDatatype>>:  {}", prop.describe());
			}
		} else {
			ResIterator funcProps = model.listSubjectsWithProperty(RDFS.domain, shadowClass);

			while (funcProps.hasNext()) {
				OntResource prop = funcProps.nextResource();
				props.add(prop);
			}

			for (OntResource prop : props) {
				OntResource range = prop.getRange();
				if (log.isDebugEnabled()) {
					log.debug("  -> merging property '{}' from '{}' into '{}' (range: '{}')",
							prop.getLabel() != null ? prop.getLabel() : prop.getLocalName(),
							shadowClass.getLabel() != null ? shadowClass.getLabel() : shadowClass.getLocalName(),
							normativeClass.getLabel() != null ? normativeClass.getLabel()
									: normativeClass.getLocalName(),
							range != null ? (range.getLabel() != null ? range.getLabel() : range.getLocalName())
									: "<none>");
				}
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

				if (prop.getIsDefinedBy() != null) {
					prop.removeProperty(RDFS.isDefinedBy, prop.getIsDefinedBy());
				}

				if (normativeClass.getIsDefinedBy() != null) {
					prop.addIsDefinedBy(normativeClass.getIsDefinedBy());
				}

				if (prop.getInverseOf() != null) {
					OntResource inverse = prop.getInverseOf();
					OntResource inverseRange = inverse.getRange();
					if (inverseRange != null && !inverseRange.getURI().equals(normativeClass.getURI())) {
						inverse.removeProperty(RDFS.range, inverseRange);
						inverse.addProperty(RDFS.range, normativeClass);
					}
				}
			}
		}

		if (isShadowClassDirectParent) {
			// Once processed we remove the super class reference from the normative CIM
			// class.
			normativeClass.removeSuperClass(shadowClass);
		}

		/**
		 * Iterate through the immediate super classes for the current "shadow class"
		 * being processed...
		 */
		ResIterator superClasses = shadowClass.listSuperClasses(false);
		while (superClasses.hasNext()) {
			OntResource superClass = superClasses.nextResource();
			// merge the extensions from shadow class into the normative class...
			if (superClass.hasProperty(UML.hasStereotype, UML.shadowextension)
					&& shadowClasses.containsKey(superClass.getString(UML.id))) {

				int totalClasses = 0;
				ResIterator shadowClassSubClasses = model.listSubjectsWithProperty(RDFS.subClassOf, superClass);
				while (shadowClassSubClasses.hasNext()) {
					shadowClassSubClasses.nextResource();
					totalClasses++;
				}
				if (totalClasses > 1) {
					shadowClassSubClasses = model.listSubjectsWithProperty(RDFS.subClassOf, superClass);
					while (shadowClassSubClasses.hasNext()) {
						OntResource subclass = shadowClassSubClasses.nextResource();
						shadowClass.removeSubClass(subclass);
					}
					// Here we simply fall through after removing the shadowClass from the model.
				} else {
					mergeShadowClass(superClass, normativeClass, shadowClasses, baseURI, false);
				}
			}
		}
	}

}
