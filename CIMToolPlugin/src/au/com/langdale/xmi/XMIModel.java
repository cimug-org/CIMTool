/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import org.xml.sax.Attributes;

import au.com.langdale.sax.XMLElement;

import com.hp.hpl.jena.graph.FrontsNode;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntResource;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A base for the XMI2OWL interpretor that wraps a Jena OWL model
 * and provides the low level operations for interpreting XMI elements
 * as resource in that model.
 */
public class XMIModel {

	/** the ontology under construction */
	protected OntModel model = ModelFactory.createMem();
	
	/** a debug flag that causes xmi:id's to be preserved as annotations */
	public boolean keepID = true;

	/**
	 * Return the underlying Jena OWL model. 
	 */
	public OntModel getModel() {
		return model;
	}

	/**
	 * Utility to find OWL class for XMI reference.
	 * 
	 * @param element in XMI referring to a class.
	 * @return resource representing the class or null.
	 */
	protected OntResource findClass(XMLElement element) {
		return findClass(element, "xmi.idref");
	}

	/**
	 * Utility to find OWL class for XMI reference by attribute.
	 * 
	 * @param element in XMI referring to a class.
	 * @return resource representing the class or null.
	 */
	protected OntResource findClass(XMLElement element, String refattr) {
		String xuid = element.getAttributes().getValue(refattr);
		if( xuid != null && xuid.length() != 0) {
			OntResource subject = model.createClass(XMI.NS + xuid);
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		else
			return null;
	}

	/**
	 * Utility to find OWL resource for XMI reference.
	 * 
	 * @param element the element containing the reference
	 * @param name the name of attribute containing the reference
	 * @return resource in the model or null.
	 */
	protected OntResource findResource(XMLElement element, String name) {
		return createUnknown(element.getAttributes().getValue(name));
	}

	/**
	 * Utility to find OWL resource for XMI reference.
	 * @param element the element containing the reference
	 * @return resource in the model or null.
	 */
	protected OntResource findResource(XMLElement element) {
		return findResource(element, "xmi.idref");
	}

	/**
	 * Utility to create and label an OWL class for an XMI declaration.
	 * @param element in XMI declaring a class.
	 * @return resource representing the class or null.
	 */
	protected OntResource createClass(XMLElement element) {
		Attributes atts = element.getAttributes();
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null && name != null ) { 
			OntResource subject = model.createClass(XMI.NS + xuid);
			subject.addLabel(name, "en");
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		return null;
	}

	/**
	 * Utility to create and label an OWL datatype for an XMI declaration.
	 * @param element in XMI declaring a datatype.
	 * @return resource representing the datatype or null.
	 */
	protected OntResource createDatatype(XMLElement element) {
		Attributes atts = element.getAttributes();
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null && name != null ) { 
			OntResource subject = model.createIndividual(XMI.NS + xuid, RDFS.Datatype);
			subject.addLabel(name, "en");
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		return null;
	}

	/**
	 * Utility to create and label an OWL object property for an XMI 
	 * association end declaration.
	 * 
	 * @param element in XMI declaring a class.
	 * @return resource representing the property or null.
	 */
	protected OntResource createObjectProperty(XMLElement element) {
		Attributes atts = element.getAttributes();
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null ) { 
			OntResource subject = model.createObjectProperty(XMI.NS + xuid);
			if(name != null)
				subject.addLabel(name, "en");
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		return null;
	}

	/**
	 * Utility to create and label an OWL object property for an XMI 
	 * association end declaration, where the latter has no id.  An
	 * id is synthesised from the parent element's id and the role name.
	 * 
	 * @param element in XMI declaring a class.
	 * @return resource representing the property or null.
	 */
	protected OntResource createObjectProperty(XMLElement element, String xuid, boolean sideA) {
		Attributes atts = element.getAttributes();
		String name = atts.getValue("name");
		if( xuid != null ) { 
			String synth = xuid + "-" + (sideA? "A": "B");
			OntResource subject = model.createObjectProperty(XMI.NS + synth);
			if( name != null)
				subject.addLabel(name, "en");	
			if(keepID)
				subject.addProperty(UML.id, synth);
			return subject;
		}
		return null;
	}

	/**
	 *Create and label an OWL annotation property 
	 * for an XMI tag declaration.
	 * 
	 * @param element in XMI declaring the property.
	 * @return resource representing the property or null.
	 */
	protected OntResource createAnnotationProperty(XMLElement element) {
		Attributes atts = element.getAttributes();

		// handle a reference
		String tagXuid = atts.getValue("xmi.idref");
        if( tagXuid != null )
        	return model.createAnnotationProperty(XMI.NS + tagXuid);

        // create a new annotation property
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null && name != null ) { 
			OntResource subject = model.createAnnotationProperty(XMI.NS + xuid);
			subject.addLabel(name, "en");
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		return null;
	}
	
	/**
	 * Create or reference a stereotype
	 */
	protected OntResource createStereotype(XMLElement element) {
		String xuid = element.getAttributes().getValue("xmi.idref");
        if( xuid != null ) {
         	return createStereoType(xuid); 
        }
        else
        	return createIndividual(element, UML.Stereotype);
	}

	/**
	 * Reference a stereotype by id string.
	 */
	protected OntResource createStereoType(String xuid) {
       	OntResource subject = model.createIndividual(XMI.NS + xuid, UML.Stereotype);
		if(keepID)
			subject.addProperty(UML.id, xuid);
    	return subject; 
	}
	
	/**
	 * Utility to create and label a generic OWL property 
	 * for an XMI tag declaration.
	 * 
	 * @param element in XMI declaring property.
	 * @return resource representing the property or null.
	 */
	protected OntResource createAttributeProperty(XMLElement element) {
		Attributes atts = element.getAttributes();
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null && name != null ) { 
			OntResource subject = model.createOntProperty(XMI.NS + xuid);
			subject.addProperty(UML.hasStereotype, UML.attribute);
			subject.addLabel(name, "en");
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		return null;
	}

	/**
	 * Utility to create an OWL resource of given typefor XMI element.
	 * @param element the element containing the reference
	 * @param type the class of the resource
	 * @return resource in the model or null.
	 */
	protected OntResource createIndividual(XMLElement element, FrontsNode type) {
		Attributes atts = element.getAttributes();
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null && name != null ) { 
			OntResource subject = model.createIndividual(XMI.NS + xuid, type);
			subject.addLabel(name, "en");
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject;
		}
		return null;
	}

	/**
	 * Utility to create a labeled resource of (as yet) unknown species.
	 */
	protected OntResource createUnknown(XMLElement element) {
		Attributes atts = element.getAttributes();
		String xuid = atts.getValue("xmi.id");
		String name = atts.getValue("name");
		if( xuid != null && name != null ) { 
			OntResource subject = model.createResource(XMI.NS + xuid);
			subject.addLabel(name, null);
			if(keepID)
				subject.addProperty(UML.id, xuid);
				return subject; 
		}
		return null;
	}
	
	/**
	 * Utility to create a resource of (as yet) unknown species.
	 */
	protected OntResource createUnknown(String xuid) {
		if( xuid != null && xuid.length() > 0) {
			OntResource subject = model.createResource(XMI.NS + xuid);
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject; 
		}
		else
			return null;
	}
	
	/**
	 * Utility to create a resource representing a UML association. 
	 * The association links two Object Properties (the roles) during
	 * model interpretation but is not required in the final model. 
	 */
	protected OntResource createAssocation(String xuid) {
		if( xuid != null && xuid.length() > 0) {
			OntResource subject = model.createResource(XMI.NS + xuid);
			if(keepID)
				subject.addProperty(UML.id, xuid);
			return subject; 
		}
		else
			return null;
	}

	/**
	 * Utility to create and label an resource for an XMI 
	 * package declaration.
	 * 
	 * @param element in XMI declaring a class.
	 * @return resource representing the package or null.
	 */
	protected OntResource createPackage(XMLElement element) {
		return createIndividual(element, UML.Package);
	}

	/**
	 * Recognise an model declaration.
	 * 
	 * @param element candidate element
	 * @param type the element type name
	 * @return true if the element is the correct type 
	 * and has the required attributes.
	 */
	protected boolean matchDef(XMLElement element, String type) {
		Attributes atts = element.getAttributes();
		return element.matches(type) 
				&& atts.getValue("xmi.id") != null 
				&& atts.getValue("name") != null;
	}
}
