/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 *  Provisional namespace to represent UML concepts in OWL ontologies. 
 *  This is used for concepts with no near equivalent in OWL.
 */

public class UML {

	public final static String NS = "http://langdale.com.au/2005/UML#";
	
	// UML meta-classes
	public final static Resource Stereotype = ResourceFactory.createResource(NS + "Stereotype");
	public final static Resource Package = ResourceFactory.createResource(NS + "Package");
	
	//public final static Resource Component = ResourceFactory.createResource(NS + "Component");
	//public final static Resource Attribute = ResourceFactory.createResource(NS + "Attribute");
	//public final static Resource Class = ResourceFactory.createResource(NS + "Class");
	//public final static Resource Association = ResourceFactory.createResource(NS + "Association");
	
	// the global Package that encloses everything
	public final static Resource global_package = ResourceFactory.createResource(NS + "global_package"); 
	
	// stereotypes we recognise
	public final static Resource attribute = ResourceFactory.createResource(NS + "attribute");
	public final static Resource primitive = ResourceFactory.createResource(NS + "primitive");	
	public final static Resource base = ResourceFactory.createResource(NS + "base");	
	public final static Resource datatype = ResourceFactory.createResource(NS + "datatype");
	public final static Resource enumeration = ResourceFactory.createResource(NS + "enumeration");
	public final static Resource union = ResourceFactory.createResource(NS + "union");
	public final static Resource xmlelement = ResourceFactory.createResource(NS + "xmlelement");
	public final static Resource xmlattribute = ResourceFactory.createResource(NS + "xmlattribute");
	public final static Resource byreference = ResourceFactory.createResource(NS + "byreference");
	public final static Resource concrete = ResourceFactory.createResource(NS + "concrete");
	public final static Resource preserve = ResourceFactory.createResource(NS + "preserve");
	
	// the stereotype of a model element
	public final static Property hasStereotype = ResourceFactory.createProperty(NS + "hasStereotype");
	
	// the initial value of an attribute
	public final static Property hasInitialValue = ResourceFactory.createProperty(NS + "hasInitialValue");

	// units and multiplier are not really a UML concepts, we infer these under CIM conventions
	public final static Property hasUnits = ResourceFactory.createProperty(NS + "hasUnits");
	public final static Property hasMultiplier = ResourceFactory.createProperty(NS + "hasMultiplier");
	
	// tags we recognise that aid conversion to RDFS/OWL
	public final static Property baseuri = ResourceFactory.createProperty(NS + "baseuri");
	public final static Property roleALabel = ResourceFactory.createProperty(NS + "roleALabel");
	public final static Property roleBLabel = ResourceFactory.createProperty(NS + "roleBLabel");
	public final static Property roleAOf = ResourceFactory.createProperty(NS + "roleAOf");
	public final static Property roleBOf = ResourceFactory.createProperty(NS + "roleBOf");
	
	// the XMI id of a model element can be preserved in the graph for debugging
	public final static Property id = ResourceFactory.createProperty(NS + "id");
	
	public static void loadOntology( OntModel model ) {
		model.createClass(Stereotype.getURI());
		model.createClass(Package.getURI());

		//model.createClass(Component.getURI());
		//model.createClass(Attribute.getURI());
		//model.createClass(Class.getURI());
		//model.createClass(Association.getURI());
		
		model.createIndividual(global_package.getURI(), Package);
		
		// well known stereotypes
		model.createIndividual(enumeration.getURI(), Stereotype).addLabel("Enumeration", null);
		model.createIndividual(union.getURI(), Stereotype).addLabel("Union", null);
		model.createIndividual(primitive.getURI(), Stereotype).addLabel("Primitive", null);
		model.createIndividual(base.getURI(), Stereotype).addLabel("Base", null);
		model.createIndividual(datatype.getURI(), Stereotype).addLabel("Datatype", null);
		model.createIndividual(attribute.getURI(), Stereotype).addLabel("Attribute", null);
		model.createIndividual(xmlelement.getURI(), Stereotype).addLabel("XML Element", null);
		model.createIndividual(xmlattribute.getURI(), Stereotype).addLabel("XML Attribute", null);		
		model.createIndividual(byreference.getURI(), Stereotype).addLabel("By Reference", null);
		model.createIndividual(concrete.getURI(), Stereotype).addLabel("Concrete", null);
		model.createIndividual(preserve.getURI(), Stereotype).addLabel("Preserve", null);
		
		model.createAnnotationProperty(baseuri.getURI());
		model.createAnnotationProperty(roleALabel.getURI());
		model.createAnnotationProperty(roleBLabel.getURI());
		model.createAnnotationProperty(roleAOf.getURI());
		model.createAnnotationProperty(roleBOf.getURI());
		model.createAnnotationProperty(hasStereotype.getURI());
		model.createAnnotationProperty(hasInitialValue.getURI());
		model.createAnnotationProperty(hasUnits.getURI());
		model.createAnnotationProperty(hasMultiplier.getURI());
		model.createAnnotationProperty(id.getURI());
	}
}
