/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;

/**
 * Extends the generic XMI interpreter to apply IEC CIM
 * modelling conventions.
 *
 */
public class CIMInterpreter extends UMLInterpreter {
	
	public static OntModel interpret(OntModel raw, String baseURI,
			OntModel annote, boolean usePackageNames) {
		if( annote != null)
			raw.add(annote);
		CIMInterpreter interpreter = new CIMInterpreter();
		return interpreter.postProcess(raw, baseURI, usePackageNames);
	}

	private OntModel postProcess(OntModel raw, String baseURI, boolean usePackageNames) {
		setModel(raw);
		
		System.out.println("Raw XMI model size: " + getModel().size());
		
		UML.loadOntology(getModel());
		pruneIncomplete();
		labelRoles();
		
		System.out.println("Stage 1 XMI model size: " + getModel().size());

		/**
		ResIterator classes1 = raw.listSubjectsWithProperty(RDF.type, OWL2.Class);
		TreeMap<String, OntResource> classMap1 = new TreeMap<>();
		classes1.forEachRemaining((c) -> {
			if (((OntResource)c).getLabel() != null)
				classMap1.put(((OntResource)c).getLabel(), (OntResource)c);
		});
		classMap1.forEach((name, aClass) -> {
			OntResource aParentPackage = aClass.getIsDefinedBy();
			System.out.println(String.format("1 - Class:  %s;     Package: %s", name, (aParentPackage != null ? aParentPackage.getLabel() : aParentPackage)));
		}); */
		
		Translator translator = new Translator(getModel(), baseURI, usePackageNames);
		translator.run();
		setModel(translator.getModel());
		
		System.out.println("Stage 3 XMI model size: " + getModel().size());
		
		/**
		ResIterator classes3 = raw.listSubjectsWithProperty(RDF.type, OWL2.Class);
		TreeMap<String, OntResource> classMap3 = new TreeMap<>();
		classes3.forEachRemaining((c) -> {
			if (((OntResource)c).getLabel() != null)
				classMap3.put(((OntResource)c).getLabel(), (OntResource)c);
		});
		classMap3.forEach((name, aClass) -> {
			OntResource aParentPackage = aClass.getIsDefinedBy();
			System.out.println(String.format("3 - Class:  %s;     Package: %s", name, (aParentPackage != null ? aParentPackage.getLabel() : aParentPackage)));
		}); */
		
		propagateComments();
		applyStereotypes();
		
		classifyAttributes();
		removeUntyped();
//		convertToSubClassOf("extensionMSITE");
		createOntologyHeader(baseURI);
		
		System.out.println("Stage 4 XMI model size: " + getModel().size());
		
		/**
		ResIterator classes4 = raw.listSubjectsWithProperty(RDF.type, OWL2.Class);
		TreeMap<String, OntResource> classMap4 = new TreeMap<>();
		classes4.forEachRemaining((c) -> {
			if (((OntResource)c).getLabel() != null)
				classMap4.put(((OntResource)c).getLabel(), (OntResource)c);
		});
		classMap4.forEach((name, aClass) -> {
			OntResource aParentPackage = aClass.getIsDefinedBy();
			System.out.println(String.format("4 - Class:  %s;     Package: %s", name, (aParentPackage != null ? aParentPackage.getLabel() : aParentPackage)));
		}); */

		return getModel();
	}

	private void createOntologyHeader(String baseURI) {
		model.createIndividual(Translator.stripHash(baseURI), OWL.Ontology);
		model.setNsPrefix("", baseURI);
	}

	private void propagateComments() {
		ResIterator it = model.listObjectProperties();
		while( it.hasNext()) {
			OntResource prop = it.nextResource();
			if(prop.getComment() == null) {
				OntResource node = prop.getResource(UML.roleAOf);
				if( node == null) 
					node = prop.getResource(UML.roleBOf);
				if( node != null) {
					String comment = node.getComment();
					if(comment != null)
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
		while( it.hasNext()) {
			OntResource prop = it.nextResource();
			if(! reLabel(prop, UML.roleAOf, UML.roleALabel))
				reLabel(prop, UML.roleBOf, UML.roleBLabel);
		}
	}

	private boolean reLabel(OntResource prop, FrontsNode roleOf, FrontsNode hasLabel) {
		OntResource node = prop.getResource(roleOf);
		if( node != null) {
			OntResource assoc = node;
			Node label = assoc.getNode(hasLabel);
			if( label != null ) {
				prop.addLabel(label.getLiteralLexicalForm(), XMIModel.LANG);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Apply CIM stereotypes and special tags to the model.  
	 * 
	 * The OWL representation of a UML class is modified in place to match
	 * the stereotype. Recognise stereotypes 'primitive' and 'enumeration'.
	 *
	 */
	private void applyStereotypes() {
		ResIterator jt = model.listSubjectsBuffered(UML.hasStereotype, UML.enumeration);
		while( jt.hasNext()) {
			applyEnumerationStereotype(jt.nextResource());
		}

		applyPrimitiveStereotype(UML.cimdatatype, true);
		applyPrimitiveStereotype(UML.datatype, true);
		applyPrimitiveStereotype(UML.primitive, true); // in future, change to false
		applyPrimitiveStereotype(UML.base, true); // in future, change to false
		applyPrimitiveStereotype(UML.union, false);


		ResIterator lt = model.listSubjectsBuffered(UML.hasStereotype, UML.extendedBy);
		while( lt.hasNext()) {
			convertAssocToSubClassOf(lt.nextResource());
		}
	}

	private void applyPrimitiveStereotype(Resource stereo, boolean interpret_value) {
		ResIterator gt = model.listSubjectsBuffered(UML.hasStereotype, stereo);
		while( gt.hasNext()) {
			applyPrimitiveStereotype(gt.nextResource(), interpret_value);
		}
	}
	
	/**
	 * Covert primitive or union stereotyped class as a datatype.
	 */
	private void applyPrimitiveStereotype(OntResource clss, boolean interpret_value) {

		OntResource truetype = null;
		String unit = null;
		String valueDataType = null;
		String valuePrimitiveDataType = null;
		String valueEAGUID = null;
		String unitDataType = null;
		String unitEAGUID = null;
		String multiplier = null;
		String multiplierDataType = null;
		String multiplierEAGUID = null;
		
		// strip the classes properties, record the value, units, and multiplier information
		ResIterator it = model.listSubjectsBuffered(RDFS.domain, clss);
		while(it.hasNext()) {
			OntResource m = it.nextResource();
			
			if( interpret_value ) {
				String name = m.getLabel();
				if(name != null) {
					// this is a CIM-style annotation to indicate the primitive datatype 
					if( name.equals("value")) {
						truetype = m.getResource(RDFS.range);
						valueDataType = m.getRange().getURI();
						valuePrimitiveDataType = m.getRange().getString(UML.primitiveDataType);
						valueEAGUID = m.getString(UML.id);
					}
					if( name.equals("unit") || name.equals("units")) {
						unit = m.getString(UML.hasInitialValue);
						unitDataType = m.getRange().getURI();
						unitEAGUID = m.getString(UML.id);
					}
					if( name.equals("multiplier")) {
						multiplier = m.getString(UML.hasInitialValue);
						multiplierDataType = m.getRange().getURI();
						multiplierEAGUID = m.getString(UML.id);
					}
				}
			}
			// remove spurious property attached to datatype
			m.removeProperties();
		}

		// for XML Schema datatypes remove all definitions
		if(clss.hasProperty(UML.hasStereotype, UML.primitive) || clss.getNameSpace().equals(XSD.getURI())){
			/**
			 * Some history on the below change introduced in 2.3.0. The context for the original 
			 * implementation in 2010/2011 was that the implementation of CIM <<Primitive>> UML
			 * classes (e.g. Decimal, Integer, Boolean, Date, Float, DateTime, Month, etc.)
			 * was originally from the perspective that the intent in the UML was that the are 
			 * representative of XSD schema types. Based on that assumption, the implementation
			 * resulted in the removing them as RDF Classes from the *.OWL profiles generated
			 * by CIMTool. This occurs via a two step process during the import of a CIM schema 
			 * (i.e. an *.xmi, *.eap, *.qea, etc. file) into CIMTool. During the execution of the
			 * CIMInterpreter.postProcess() method the Translator class runs two passes as part 
			 * if its translation processing. It is during "pass 2" that it does a rename in the 
			 * Translator.renameResource(OntResource r, String l) method and renames/transforms the 
			 * CIM <<Primitive>> class itself to it's XSD schema type. That translation leaves the
			 * OntResource looking like the following if you were to perform a OntResource.desribe():
			 * 
			 * http://www.w3.org/2001/XMLSchema#boolean
			 *   http://langdale.com.au/2005/UML#hasStereotype = http://langdale.com.au/2005/UML#primitive
			 *   http://www.w3.org/2000/01/rdf-schema#isDefinedBy = http://iec.ch/TC57/CIM100#Package_Domain
			 *   http://langdale.com.au/2005/UML#id = "EAID_9F8964F1_6C32_465b_A83D_F5A201A291C3"
			 *   http://www.w3.org/2000/01/rdf-schema#label = "Boolean"
			 *   http://www.w3.org/2000/01/rdf-schema#comment = "A type with the value space "true" and "false"."
			 * 
			 * Thus, the translation step was a preparatory step that essentially was completed in
			 * this method where the call to removeProperites(), commented out below, would remove
			 * the additional properties above. 
			 * 
			 * However, as things have evolved in the broader CIM community and as RDFS2020 and other
			 * representations have been introduced, those representations explicitly represent the 
			 * CIM <<Primitive>> classes as RDF Classes in those newer formats (such as in the CGMES
			 * RDFS profiles). However, to preserve backwards compatibility we have chosen (for now) 
			 * not to update the core representation but rather to limit changes to only the generated
			 * XML internal profile representation produced by the ProfileSerializer class. Thus by 
			 * changing the below to remove only the RDF.type properties it filters it still "filters
			 * out" the <<Primitive>> classes from the core *.OWL profile (which only includes valid
			 * OntResources defined with a property of:   
			 * 
			 * http://www.w3.org/1999/02/22-rdf-syntax-ns#type = http://www.w3.org/2002/07/owl#Class
			 * 
			 * Thus, the change below preserves the remaining 4 properties (for package name, comments, 
			 * stereotypes, etc.) that we can use "downstream" when generating the XML internal profile
			 * used by the XSLT RDFS2020 builder (and others). for further details, refer to the 
			 * ProfileSerializer.emitPrimitive() method on that class.
			 */
			//clss.removeProperties();
			clss.removeAll(RDF.type);
			//
			String localName = clss.getLocalName();
			FrontsNode xsdType = XSDTypeUtils.selectXSDType(localName);
			if (xsdType != null) {
				clss.addProperty(OWL.equivalentClass, xsdType);
			}
		}
		
		// for defined datatypes, establish an RDFS datatype
		else {
			clss.removeAll(RDF.type);
			clss.addProperty(RDF.type, RDFS.Datatype);
			//
			if( truetype != null && !clss.equals(truetype) ) 
				clss.addProperty( OWL.equivalentClass, truetype );
			if( valueDataType != null )
				clss.addProperty( UML.valueDataType, valueDataType);
			if ( valuePrimitiveDataType != null)
				clss.addProperty( UML.valuePrimitiveDataType, valuePrimitiveDataType);
			if( valueEAGUID != null )
				clss.addProperty( UML.valueEAGUID, valueEAGUID);
			//
			if( unit != null )
				clss.addProperty( UML.hasUnits, unit);
			if( unitDataType != null )
				clss.addProperty( UML.unitDataType, unitDataType);
			if( unitEAGUID != null )
				clss.addProperty( UML.unitEAGUID, unitEAGUID);
			//
			if( multiplier != null )
				clss.addProperty( UML.hasMultiplier, multiplier);
			if( multiplierDataType != null )
				clss.addProperty( UML.multiplierDataType, multiplierDataType);
			if( multiplierEAGUID != null )
				clss.addProperty( UML.multiplierEAGUID, multiplierEAGUID);
		}
	}

	/**
	 * Convert enumeration stereotyped class and attributes to 
	 * a class and its individuals.
	 * 
	 * The properties of the class are re-interpreted as 
	 * members of the enumeration. Note that OWL EnumeratedClass
	 * is not used here because that would create a closed enumeration. 
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
		while(it.hasNext()) {
			OntResource m = it.nextResource();
			if( m.hasProperty(UML.hasStereotype, UML.attribute)) {
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
		if( prop != null )
			convertPropToSubClassOf(prop);
	}
	
	private void convertPropToSubClassOf(OntResource prop) {
		OntResource range = prop.getRange();
		OntResource domain = prop.getDomain();
		if( range != null && domain != null) {
			OntResource inv = prop.getInverseOf();
			prop.remove();
			
			if( model.contains(domain, UML.hasStereotype, UML.extension))
				range.addSuperClass(domain);
			else if( model.contains(range, UML.hasStereotype, UML.extension))
				domain.addSuperClass(range);
			else
				return;
			
			if( inv != null)
				inv.remove();
		}
	}
}	
