/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.NodeIterator;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Extends the generic XMI interpreter to apply IEC CIM modelling conventions.
 *
 */
public class LegacyCIMInterpreterImpl extends UMLInterpreter implements CIMInterpreter {

	private static final Logger log = LoggerFactory.getLogger(LegacyCIMInterpreterImpl.class);

	LegacyCIMInterpreterImpl() {
		super();
	}
	
	LegacyCIMInterpreterImpl(StereotypedNamespaces stereotypedNamespaces) {
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
		
		return postProcess(raw, baseURI, usePackageNames);
	}

	private CIMInterpreterResult postProcess(OntModel raw, String baseURI, boolean usePackageNames) {
		setModel(raw);

		log.debug("Raw XMI model size: {}", getModel().size());

		UML.loadOntology(getModel());
		pruneIncomplete();
		labelRoles();

		log.debug("Stage 1 XMI model size: {}", getModel().size());

		LegacyTranslator translator = new LegacyTranslator(getModel(), baseURI, usePackageNames);
		translator.run();
		setModel(translator.getModel());

		log.debug("Stage 3 XMI model size: {}", getModel().size());

		propagateComments();
		applyStereotypes();
		classifyAttributes();
		removeUntyped();
//		convertToSubClassOf("extensionMSITE");
		createOntologyHeader(baseURI);

		log.debug("Stage 4 XMI model size: {}", getModel().size());

		return new CIMInterpreterResult(getModel(), new ArrayList<>());
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
	private void applyStereotypes() {
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

}
