/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

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
	public final static Resource cimdatatype = ResourceFactory.createResource(NS + "cimdatatype");
	public final static Resource enumeration = ResourceFactory.createResource(NS + "enumeration");
	public final static Resource union = ResourceFactory.createResource(NS + "union");
	public final static Resource extendedBy = ResourceFactory.createResource(NS + "extendedby");
	public final static Resource extension = ResourceFactory.createResource(NS + "extension");
	public final static Resource xmlelement = ResourceFactory.createResource(NS + "xmlelement");
	public final static Resource xmlattribute = ResourceFactory.createResource(NS + "xmlattribute");
	public final static Resource byreference = ResourceFactory.createResource(NS + "byreference");
	public final static Resource concrete = ResourceFactory.createResource(NS + "concrete");
	public final static Resource compound = ResourceFactory.createResource(NS + "compound");
	public final static Resource preserve = ResourceFactory.createResource(NS + "preserve");
	
	// stereotype the two forms of aggregation, each in two directions
	public final static Resource ofComposite = ResourceFactory.createResource(NS + "ofComposite");
	public final static Resource ofAggregate = ResourceFactory.createResource(NS + "ofAggregate");
	public final static Resource compositeOf = ResourceFactory.createResource(NS + "compositeOf");
	public final static Resource aggregateOf = ResourceFactory.createResource(NS + "aggregateOf");
	
	// the stereotype of a model element
	public final static Property hasStereotype = ResourceFactory.createProperty(NS + "hasStereotype");
	
	// the initial value of an attribute
	public final static Property hasInitialValue = ResourceFactory.createProperty(NS + "hasInitialValue");

	// units and multiplier are not really a UML concepts, we infer these under CIM conventions
	public final static Property hasUnits = ResourceFactory.createProperty(NS + "hasUnits");
	public final static Property hasMultiplier = ResourceFactory.createProperty(NS + "hasMultiplier");
	
	// tags we recognise that aid conversion to RDFS/OWL
	public final static Property baseuri = ResourceFactory.createProperty(NS + "baseuri");
	public final static Property baseprefix = ResourceFactory.createProperty(NS + "baseprefix");
	public final static Property roleALabel = ResourceFactory.createProperty(NS + "roleALabel");
	public final static Property roleBLabel = ResourceFactory.createProperty(NS + "roleBLabel");
	public final static Property roleAOf = ResourceFactory.createProperty(NS + "roleAOf");
	public final static Property roleBOf = ResourceFactory.createProperty(NS + "roleBOf");
	
	// declare a prefix to namespace mapping. can be used in annotation files to 
	// associate namespaces with elements via their baseprefix tags.
	public final static Property uriHasPrefix = ResourceFactory.createProperty(NS + "uriHasPrefix");
	
	// the XMI id of a model element can be preserved in the graph for debugging
	public final static Property id = ResourceFactory.createProperty(NS + "id");

	// the cardinality of a class (as opposed to a property)
	public final static Property hasMaxCardinality = ResourceFactory.createProperty(NS + "hasMaxCardinality");
	public final static Property hasMinCardinality = ResourceFactory.createProperty(NS + "hasMinCardinality");
	
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
		model.createIndividual(extendedBy.getURI(), Stereotype).addLabel("Extended By", null);
		model.createIndividual(extension.getURI(), Stereotype).addLabel("Extension Class", null);
		model.createIndividual(primitive.getURI(), Stereotype).addLabel("Primitive", null);
		model.createIndividual(base.getURI(), Stereotype).addLabel("Base", null);
		model.createIndividual(datatype.getURI(), Stereotype).addLabel("Datatype", null);
		model.createIndividual(cimdatatype.getURI(), Stereotype).addLabel("CIMDatatype", null);
		model.createIndividual(attribute.getURI(), Stereotype).addLabel("Attribute", null);
		model.createIndividual(xmlelement.getURI(), Stereotype).addLabel("XML Element", null);
		model.createIndividual(xmlattribute.getURI(), Stereotype).addLabel("XML Attribute", null);		
		model.createIndividual(byreference.getURI(), Stereotype).addLabel("By Reference", null);
		model.createIndividual(concrete.getURI(), Stereotype).addLabel("Concrete", null);
		model.createIndividual(compound.getURI(), Stereotype).addLabel("Compound Datatype", null);
		model.createIndividual(preserve.getURI(), Stereotype).addLabel("Preserve", null);
		model.createIndividual(ofComposite.getURI(), Stereotype).addLabel("Of Composite", null);
		model.createIndividual(ofAggregate.getURI(), Stereotype).addLabel("Of Aggregate", null);
		model.createIndividual(compositeOf.getURI(), Stereotype).addLabel("Composite Of", null);
		model.createIndividual(aggregateOf.getURI(), Stereotype).addLabel("Aggregate Of", null);
		
		model.createAnnotationProperty(baseuri.getURI());
		model.createAnnotationProperty(baseprefix.getURI());
		model.createAnnotationProperty(uriHasPrefix.getURI());
		model.createAnnotationProperty(roleALabel.getURI());
		model.createAnnotationProperty(roleBLabel.getURI());
		model.createAnnotationProperty(roleAOf.getURI());
		model.createAnnotationProperty(roleBOf.getURI());
		model.createAnnotationProperty(hasStereotype.getURI());
		model.createAnnotationProperty(hasInitialValue.getURI());
		model.createAnnotationProperty(hasUnits.getURI());
		model.createAnnotationProperty(hasMultiplier.getURI());
		model.createAnnotationProperty(id.getURI());
		model.createAnnotationProperty(hasMaxCardinality.getURI());
		model.createAnnotationProperty(hasMinCardinality.getURI());
	}
}
