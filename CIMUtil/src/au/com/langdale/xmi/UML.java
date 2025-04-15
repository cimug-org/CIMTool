/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.HashMap;
import java.util.Map;

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
	public final static Resource enumliteral = ResourceFactory.createResource(NS + "enum");
	public final static Resource enumeration = ResourceFactory.createResource(NS + "enumeration");
	public final static Resource union = ResourceFactory.createResource(NS + "union");
	public final static Resource extendedBy = ResourceFactory.createResource(NS + "extendedby");
	public final static Resource shadowextension = ResourceFactory.createResource(NS + "shadowextension");
	public final static Resource extension = ResourceFactory.createResource(NS + "extension");
	public final static Resource xmlelement = ResourceFactory.createResource(NS + "xmlelement");
	public final static Resource xmlattribute = ResourceFactory.createResource(NS + "xmlattribute");
	public final static Resource byreference = ResourceFactory.createResource(NS + "byreference");
	public final static Resource concrete = ResourceFactory.createResource(NS + "concrete");
	public final static Resource compound = ResourceFactory.createResource(NS + "compound");
	public final static Resource preserve = ResourceFactory.createResource(NS + "preserve");
	public final static Resource description = ResourceFactory.createResource(NS + "description");
	public final static Resource hideOnDiagrams = ResourceFactory.createResource(NS + "hideondiagrams");
	public final static Resource schemaMin = ResourceFactory.createResource(NS + "schemaMin");
	public final static Resource schemaMax = ResourceFactory.createResource(NS + "schemaMax");
	
	// Interim solution to support backward compatibility for primitives until nextgen implementation... 
	public final static Resource cimdatatypeMapping = ResourceFactory.createResource(NS + "cimdatatypeMapping");
	
	// Interim solution to support backward compatibility for CIMDatatypes until nextgen implementation... 
	public final static Resource valueEAGUID = ResourceFactory.createResource(NS + "valueEAGUID");	
    public final static Resource valueComment = ResourceFactory.createResource(NS + "valueComment");	
	public final static Resource unitEAGUID = ResourceFactory.createResource(NS + "unitEAGUID");	
    public final static Resource unitConstant = ResourceFactory.createResource(NS + "unitConstant");	
    public final static Resource unitComment = ResourceFactory.createResource(NS + "unitComment");	
	public final static Resource multiplierEAGUID = ResourceFactory.createResource(NS + "multiplierEAGUID");	
    public final static Resource multiplierConstant = ResourceFactory.createResource(NS + "multiplierConstant");
    public final static Resource multiplierComment = ResourceFactory.createResource(NS + "multiplierComment");	

	// stereotype the two forms of aggregation, each in two directions
	public final static Resource ofComposite = ResourceFactory.createResource(NS + "ofComposite");
	public final static Resource ofAggregate = ResourceFactory.createResource(NS + "ofAggregate");
	public final static Resource compositeOf = ResourceFactory.createResource(NS + "compositeOf");
	public final static Resource aggregateOf = ResourceFactory.createResource(NS + "aggregateOf");
	
	public final static Map<String, Resource> stereotypes = new HashMap<String, Resource>();
	
	static {
		// stereotypes we recognise
		stereotypes.put(attribute.getLocalName(), attribute);
		stereotypes.put(primitive.getLocalName(), primitive);
		stereotypes.put(base.getLocalName(), base);
		stereotypes.put(datatype.getLocalName(), datatype);
		stereotypes.put(cimdatatype.getLocalName(), cimdatatype);
		stereotypes.put(enumliteral.getLocalName(), enumliteral);
		stereotypes.put(enumeration.getLocalName(), enumeration);
		stereotypes.put(union.getLocalName(), union);
		stereotypes.put(extendedBy.getLocalName(), extendedBy);
		stereotypes.put(extension.getLocalName(), extension);
		stereotypes.put(xmlelement.getLocalName(), xmlelement);
		stereotypes.put(xmlattribute.getLocalName(), xmlattribute);
		stereotypes.put(byreference.getLocalName(), byreference);
		stereotypes.put(concrete.getLocalName(), concrete);
		stereotypes.put(compound.getLocalName(), compound);
		stereotypes.put(preserve.getLocalName(), preserve);
		stereotypes.put(description.getLocalName(), description);
		stereotypes.put(hideOnDiagrams.getLocalName(), hideOnDiagrams);
		stereotypes.put(ofComposite.getLocalName(), ofComposite);
		stereotypes.put(ofAggregate.getLocalName(), ofAggregate);
		stereotypes.put(compositeOf.getLocalName(), compositeOf);
		stereotypes.put(aggregateOf.getLocalName(), aggregateOf);
	}
	
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
		model.createIndividual(enumeration.getURI(), Stereotype).addLabel("enumeration", null);
		model.createIndividual(enumliteral.getURI(), Stereotype).addLabel("enum", null);
		model.createIndividual(union.getURI(), Stereotype).addLabel("Union", null);
		model.createIndividual(extendedBy.getURI(), Stereotype).addLabel("ExtendedBy", null);
		model.createIndividual(shadowextension.getURI(), Stereotype).addLabel("ShadowExtension", null);
		model.createIndividual(extension.getURI(), Stereotype).addLabel("Extension", null);
		model.createIndividual(primitive.getURI(), Stereotype).addLabel("Primitive", null);
		model.createIndividual(base.getURI(), Stereotype).addLabel("Base", null);
		model.createIndividual(cimdatatype.getURI(), Stereotype).addLabel("CIMDatatype", null);
		model.createIndividual(datatype.getURI(), Stereotype).addLabel("Datatype", null);
		model.createIndividual(attribute.getURI(), Stereotype).addLabel("Attribute", null);
		model.createIndividual(xmlelement.getURI(), Stereotype).addLabel("XMLElement", null);
		model.createIndividual(xmlattribute.getURI(), Stereotype).addLabel("XMLAttribute", null);		
		model.createIndividual(byreference.getURI(), Stereotype).addLabel("ByReference", null);
		model.createIndividual(concrete.getURI(), Stereotype).addLabel("Concrete", null);
		model.createIndividual(compound.getURI(), Stereotype).addLabel("Compound", null);
		model.createIndividual(preserve.getURI(), Stereotype).addLabel("Preserve", null);
		model.createIndividual(hideOnDiagrams.getURI(), Stereotype).addLabel("HideOnDiagrams", null);
		model.createIndividual(ofComposite.getURI(), Stereotype).addLabel("OfComposite", null);
		model.createIndividual(ofAggregate.getURI(), Stereotype).addLabel("OfAggregate", null);
		model.createIndividual(compositeOf.getURI(), Stereotype).addLabel("CompositeOf", null);
		model.createIndividual(aggregateOf.getURI(), Stereotype).addLabel("AggregateOf", null);
		model.createIndividual(description.getURI(), Stereotype).addLabel("Description", null);
		
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
		model.createAnnotationProperty(schemaMin.getURI());
		model.createAnnotationProperty(schemaMax.getURI());
		// Set of temporary annotations until the next gen implementation...
		model.createAnnotationProperty(cimdatatypeMapping.getURI());
		model.createAnnotationProperty(valueEAGUID.getURI());
		model.createAnnotationProperty(valueComment.getURI());
		model.createAnnotationProperty(unitEAGUID.getURI());
		model.createAnnotationProperty(unitConstant.getURI());
		model.createAnnotationProperty(unitComment.getURI());
		model.createAnnotationProperty(multiplierEAGUID.getURI());
		model.createAnnotationProperty(multiplierConstant.getURI());
		model.createAnnotationProperty(multiplierComment.getURI());

	}
}
