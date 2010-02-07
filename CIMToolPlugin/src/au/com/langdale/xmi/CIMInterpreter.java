/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

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

		Translator translator = new Translator(getModel(), baseURI, usePackageNames);
		translator.run();
		setModel(translator.getModel());
		
		System.out.println("Stage 3 XMI model size: " + getModel().size());

		propagateComments();
		applyStereotypes();
		classifyAttributes();
		removeUntyped();
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
		ResIterator ht = model.listSubjectsBuffered(UML.hasStereotype, UML.datatype);
		while( ht.hasNext()) {
			applyPrimitiveStereotype(ht.nextResource(), true);
		}
		ResIterator it = model.listSubjectsBuffered(UML.hasStereotype, UML.primitive);
		while( it.hasNext()) {
			applyPrimitiveStereotype(it.nextResource(), true); // in future, change to false
		}
		ResIterator gt = model.listSubjectsBuffered(UML.hasStereotype, UML.base);
		while( gt.hasNext()) {
			applyPrimitiveStereotype(gt.nextResource(), true); // in future, change to false
		}
		ResIterator kt = model.listSubjectsBuffered(UML.hasStereotype, UML.union);
		while( kt.hasNext()) {
			applyPrimitiveStereotype(kt.nextResource(), false);
		}
		ResIterator lt = model.listSubjectsBuffered(UML.hasStereotype, UML.extendedBy);
		while( lt.hasNext()) {
			convertAssocToSubClassOf(lt.nextResource());
		}
	}
	
	/**
	 * Covert primitive or union stereotyped class as a datatype.
	 */
	private void applyPrimitiveStereotype(OntResource clss, boolean interpret_value) {

		OntResource truetype = null;
		String units = null;
		String multiplier = null;
		
		// strip the classes properties, record the value and units information
		ResIterator it = model.listSubjectsBuffered(RDFS.domain, clss);
		while(it.hasNext()) {
			OntResource m = it.nextResource();
			
			if( interpret_value ) {
				String name = m.getLabel();
				if(name != null) {
					// this is a CIM-style annotation to indicate the primitive datatype 
					if( name.equals("value"))
						truetype = m.getResource(RDFS.range);
					if( name.equals("unit") || name.equals("units"))
						units = m.getString(UML.hasInitialValue);
					if( name.equals("multiplier"))
						multiplier = m.getString(UML.hasInitialValue);
				}
			}
			// remove spurious property attached to datatype
			m.removeProperties();
		}

		// for XML Schema datatypes remove all definitions
		if( clss.getNameSpace().equals(XSD.getURI())){
			clss.removeProperties();
		}
		
		// for defined datatypes, establish an RDFS datatype
		else {
			clss.removeAll(RDF.type);
			clss.addProperty(RDF.type, RDFS.Datatype);

			if( truetype != null && ! clss.equals(truetype)) 
				clss.addProperty( OWL.equivalentClass, truetype );
			if( units != null )
				clss.addProperty( UML.hasUnits, units);
			if( multiplier != null )
				clss.addProperty( UML.hasMultiplier, multiplier);
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
