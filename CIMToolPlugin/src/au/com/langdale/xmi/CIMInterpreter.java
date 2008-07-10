/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
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
	
	/**
	 * Utility to parse an XMI file, apply CIM conventions, and return a Jena OWL model.
	 */
	public static OntModel parse(String filename, String baseURI) throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		XMIParser parser = new XMIParser();
		parser.parse(filename);
		OntModel raw = parser.getModel();
		
		return postProcess(raw, baseURI);
	}
	
	public static OntModel parse(InputStream stream, String baseURI, Model annote) throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		XMIParser parser = new XMIParser();
		parser.parse(stream);
		OntModel raw = parser.getModel();
		if( annote != null)
			raw.add(annote, true);
		return postProcess(raw, baseURI);
	}

	private static OntModel postProcess(OntModel raw, String baseURI) {
		CIMInterpreter interpreter = new CIMInterpreter();
		interpreter.setModel(raw);

		UML.loadOntology(interpreter.getModel());
		interpreter.pruneIncomplete();
		interpreter.labelRoles();

		Translator translator = new Translator(interpreter.getModel(), baseURI);
		translator.run();
		interpreter.setModel(translator.getResult());

		interpreter.removeUntyped();
		interpreter.applyStereotypes();
		interpreter.classifyAttributes();
		return interpreter.getModel();
	}
	
	/**
	 * Find labels for association roles.
	 */
	public void labelRoles() {
		ExtendedIterator it = model.listObjectProperties();
		while( it.hasNext()) {
			ObjectProperty prop = (ObjectProperty) it.next();
			if(! reLabel(prop, UML.roleAOf, UML.roleALabel))
				reLabel(prop, UML.roleBOf, UML.roleBLabel);
		}
	}

	private boolean reLabel(ObjectProperty prop, Property roleOf, Property hasLabel) {
		RDFNode node = prop.getPropertyValue(roleOf);
		if( node != null) {
			OntResource assoc = (OntResource) node.as(OntResource.class);
			RDFNode label = assoc.getPropertyValue(hasLabel);
			if( label != null ) {
				prop.setLabel(label.toString(), "en");
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
	public void applyStereotypes() {
		ResIterator jt = model.listSubjectsWithProperty(UML.hasStereotype, UML.enumeration);
		while( jt.hasNext()) {
			applyEnumerationStereotype(jt.nextResource());
		}
		ResIterator ht = model.listSubjectsWithProperty(UML.hasStereotype, UML.datatype);
		while( ht.hasNext()) {
			applyPrimitiveStereotype(ht.nextResource(), true);
		}
		ResIterator it = model.listSubjectsWithProperty(UML.hasStereotype, UML.primitive);
		while( it.hasNext()) {
			applyPrimitiveStereotype(it.nextResource(), true); // in future, change to false
		}
		ResIterator gt = model.listSubjectsWithProperty(UML.hasStereotype, UML.base);
		while( gt.hasNext()) {
			applyPrimitiveStereotype(gt.nextResource(), true); // in future, change to false
		}
		ResIterator kt = model.listSubjectsWithProperty(UML.hasStereotype, UML.union);
		while( kt.hasNext()) {
			applyPrimitiveStereotype(kt.nextResource(), false);
		}
	}
	
	/**
	 * Covert primitive or union stereotyped class as a datatype.
	 */
	protected void applyPrimitiveStereotype(Resource clss, boolean interpret_value) {

		Resource truetype = null;
		String units = null;
		String multiplier = null;
		
		// strip the classes properties, record the value and units information
		ResIterator it = model.listSubjectsWithProperty(RDFS.domain, clss);
		while(it.hasNext()) {
			Resource m = it.nextResource();
			
			if( interpret_value ) {
					String name = m.getRequiredProperty(RDFS.label).getString();

					// this is a CIM-style annotation to indicate the primitive datatype 
					if( name.equals("value") && m.hasProperty(RDFS.range))
						truetype = m.getRequiredProperty(RDFS.range).getResource();
					if( name.equals("unit") && m.hasProperty(UML.hasInitialValue))
						units = m.getRequiredProperty(UML.hasInitialValue).getString();
					if( name.equals("multiplier") && m.hasProperty(UML.hasInitialValue))
						multiplier = m.getRequiredProperty(UML.hasInitialValue).getString();
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
				clss.addProperty( OWL.sameAs, truetype );
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
	protected void applyEnumerationStereotype(Resource clss) {
		clss.removeAll(RDF.type);
		clss.addProperty(RDF.type, OWL.Class);
		
		// some UML models have inconsistent stereotypes
		model.remove(clss, UML.hasStereotype, UML.datatype);
		model.remove(clss, UML.hasStereotype, UML.primitive);
		ResIterator it = model.listSubjectsWithProperty(RDFS.domain, clss);
		while(it.hasNext()) {
			Resource m = it.nextResource();
			if( m.hasProperty(UML.hasStereotype, UML.attribute)) {
				m.removeAll(RDF.type);
				m.removeAll(RDFS.range);
				m.removeAll(RDFS.domain);
				m.removeAll(UML.hasStereotype);
				m.addProperty(RDF.type, clss);
			}
		}
	}
}	
