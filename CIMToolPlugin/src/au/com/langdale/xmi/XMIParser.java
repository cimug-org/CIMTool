/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.sax.XMLElement;
import au.com.langdale.sax.XMLInterpreter;
import au.com.langdale.sax.XMLMode;

public class XMIParser extends XMIModel {

	public boolean powercc = true;;

	/**
	 * Parse the given file as XMI producing an OWL model.
	 */
	public void parse(String fileName) throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		parse(new InputSource(fileName));
	}

	public void parse(InputStream stream) throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		parse(new InputSource(stream));
	}
	
	public void parse(InputSource source) throws ParserConfigurationException, SAXException, IOException {
	    
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		
		// trying to stop parser read DTD's
//		factory.setValidating(false);
//		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		
		// still trying to kill attempts to read DTD
		reader.setEntityResolver(new EntityResolver() {
			public InputSource resolveEntity(String arg0, String arg1) throws SAXException, IOException {
				return new InputSource(new StringReader(""));
			}});
		
		// kick off in global package mode - can vacumn up elements that 
		// slip outside the Model element in some xmi dialects
		reader.setContentHandler( new XMLInterpreter( new PackageMode() ));
		reader.parse(source);
	}

	/**
  	 * Base class for all parser modes.
	 */
	private abstract class BaseMode implements XMLMode {
		/**
		 * Common part of visit() for most model elements
		 * collects annotations and stereotypes.
		 */
		protected XMLMode visit(XMLElement element, OntResource resource) {
			
			if ( element.matches("TaggedValue")) {
				return new TaggedValueMode(element, resource);
			}
			else if( element.matches("Stereotype")) {
				return new StereotypeMode(element, resource);
			}
			else if( element.matches("Comment")) {
				String comment = element.getAttributes().getValue("body");
				if( comment != null)
					resource.addComment(comment, null);
				return null;
			}
			else if( element.matches("Operation"))
				return null;   
			else
				return this;
			
		}

		public void visit(XMLElement element, String text) {
			// ignore text nodes
		}
		
		public void leave() {
			// no action
		}
	}

	/**
	 * Interpret a UML tag instance as an owl annotation. 
	 */
	private class TaggedValueMode extends BaseMode {
	    Resource subject;
	    String tagValue;
	    Property property;
	    
	    /**
	     * Construct for an unknown subject (expect a modelElement reference)
	     */
	    TaggedValueMode(XMLElement element) {
	    	this(element, null);
	    }
	    
	    /**
	     * Construct for a known subject.
	     */
	    TaggedValueMode(XMLElement element, Resource resource) {
	        subject = resource;
	        tagValue = element.getAttributes().getValue("value");
	        
	        // in UML 1.3 documentation tags carry the comments
			String type = element.getAttributes().getValue("tag");
			
			// in UML 1.4 the type is in the name attribute
			if(type == null)
				type = element.getAttributes().getValue("name");

			if( type != null ) {
				if(type.equals("documentation"))
					property = RDFS.comment;
				else if(powercc) {
					if(type.equals("RationalRose$PowerCC:RdfRoleA"))
						property = UML.roleALabel;
					else if(type.equals("RationalRose$PowerCC:RdfRoleB"))
						property = UML.roleBLabel;
				}
			}

	        
	        // we overide the default subject here and in visit()
	    	Resource ref = createUnknown(element.getAttributes().getValue("modelElement"));
	    	if( ref != null)
	    		subject = ref;
	    }
	    
	    public XMLMode visit(XMLElement element) {

	    	// handle a tag definition or a reference to one
	    	if ( element.matches("TagDefinition")) {
            	property = createAnnotationProperty(element);
	            return null;
	        }
	        else if ( element.matches("TaggedValue.dataValue")
	        			|| element.matches("TaggedValue.value")) {
	            return new TaggedValueDataValueMode();
	        }
	        else if( element.matches("ModelElement")) {
	        	subject = createUnknown(element.getAttributes().getValue("xmi.idref"));
	        	return null;
	        }
	        else 
	        	return this;
	    }
	    
	    private class TaggedValueDataValueMode extends BaseMode {
	        
	        @Override
			public void visit(XMLElement element, String text) {
	        	if( tagValue == null )
	        		tagValue = text;
	        	else
	        		tagValue += text;
	        }
	        public XMLMode visit(XMLElement element) {
	            return this;
	        }
	    }
	    
	    @Override
		public void leave() {
	        if ( tagValue != null && subject != null && property != null) {
	        		subject.addProperty(property, tagValue.trim());
	        }
	    }
	}
	
	/**
	 * Interpret an UML stereotype
	 */
	private class StereotypeMode extends BaseMode {
		private OntResource subject;
		private OntResource stereo;
		
		private StereotypeMode(XMLElement element) {
			this(element, null);
		}
		
		private StereotypeMode(XMLElement element, OntResource subject) {
			this.subject = subject;
			stereo = createStereotype(element);
			extend(element);
		}
		
		/**
		 * Multiple stereotyped elements
		 */
		protected void extend(XMLElement element) {
			String list = element.getAttributes().getValue("extendedElement");
			if( stereo != null && list != null) {
				subject = null; // forget subject
				
				// parse list of subjects
				String[] xuids = list.split(" +");
				for(int ix = 0; ix < xuids.length; ix++) {
					Resource given = createUnknown(xuids[ix]);
					if( given != null)
						given.addProperty(UML.hasStereotype, stereo);
				}
			}
		}
		
        public XMLMode visit(XMLElement element) {
        	if( element.matches("ModelElement")) {
        		subject = null; // forget the given subject
        		Resource given = createUnknown(element.getAttributes().getValue("xmi.idref")); 
    			if(stereo != null && given != null) {
    				given.addProperty(UML.hasStereotype, stereo);
    			}
        	}
        	return this;
        }
        
        /**
         * Apply the stereotype to the default subject, if possible. 
         */
		@Override
		public void leave() {
			if(stereo != null && subject != null) {
				subject.addProperty(UML.hasStereotype, stereo);
			}
		}		
	}

	/**
	 * Parse a UML package and the classes and associations it contains.
	 */
	private class PackageMode extends BaseMode {
	    OntResource packResource;
	    
	    /**
	     * Construct for the top-level package.
	     */
	    PackageMode() {
	    	packResource = model.createIndividual(UML.global_package.getURI(), UML.Package);
	    	packResource.setLabel("Global", null);
	    }
	    
	    /**
	     * Construct for a subordinate package.
	     */
	    PackageMode(XMLElement element, PackageMode parent) {
	    	packResource = createPackage(element);
	    	parent.packageDefines(packResource);
	    }
	    
	    void packageDefines(OntResource res) {
	    	if( ! packResource.equals(UML.global_package))
	    		res.addIsDefinedBy(packResource);
	    }
		
		public XMLMode visit(XMLElement element) {
		    if ( element.matches("Association")) {
				return new AssociationMode(element);
			}
			else if ( matchDef( element, "Class")) {
				return new ClassMode(element);
			}
    		else if ( matchDef( element, "Enumeration")) {
		        return new EnumerationMode(element);
    		}
			else if ( matchDef( element, "Package")) {
				return new PackageMode(element, this);
			}
			else if ( matchDef( element, "Component")) {
				createUnknown(element);
				return null;
			}
			else if( matchDef( element, "AssociationClass")) {
				// TODO: implement association classes
				return null;
			}
			else if ( matchDef(element, "DataType")) {
    	    	// TODO: pick up Datatype in package named by typename for MagicDraw
    	    	// createDatatype(element);
    	    	//return null;
				return new DatatypeMode( element );
			}
    		else if ( element.matches("Generalization")) {
		        return new GeneralizationMode(element);
    		}
    		else if ( matchDef( element, "TagDefinition")) { 
    			createAnnotationProperty(element);
    			return null;
    		}
			else if( element.matches("Stereotype")) {
				return new StereotypeMode(element); // pick up stereotype but don't apply to package
			}
		    else if( element.matches("Diagram")) {
		    	return null; //chop off any diagrams
		    }
    	    else if( element.matches("Subsystem")) {
    	    	return null; // chop off subsystem definitions
    	    }
    		else
    			return visit(element, packResource);
		}

		/**
		 * Interpret an XMI generalisation as an OWL subClass.
		 */
		private class GeneralizationMode extends BaseMode {
		    OntClass ontChild;
		    OntClass ontParent;
		    
		    GeneralizationMode(XMLElement element) {
		    	ontChild = findClass(element, "child");
		    	ontParent = findClass(element, "parent");
		    }
		
		    public XMLMode visit(XMLElement element) {
		        if ( element.matches("Generalization.child"))
			        return new GeneralizationChildMode();
			    else if ( element.matches("Generalization.parent")) 
			        return new GeneralizationParentMode();
			    else
			    	return this;
			}
		    
		    private class GeneralizationChildMode extends BaseMode {
			    public XMLMode visit(XMLElement element) {
				    if ( element.matches("Class")) {
				        ontChild = findClass(element);
				        return null;
					}
					return this;
				}
		    }		
		    
		    private class GeneralizationParentMode extends BaseMode {
			    
			    public XMLMode visit(XMLElement element) {
				    if ( element.matches("Class")) {
				        ontParent = findClass(element);
				        return null;
					}
					return this;
				}
			}
		
		    @Override
			public void leave() {;
		    	if ( ontChild != null && ontParent != null)  {
		    	    ontParent.addSubClass(ontChild);
		    	}
		    }
		}
		
		/**
		 * Interpret a UML association as a pair of OWL ObjectProperties. 
		 */
		private class AssociationMode extends BaseMode {
		    AssociationEndMode endA, endB;
		    String associd;
		    OntResource assoc;
		    
		    public AssociationMode(XMLElement element) {
		    	associd = element.getAttributes().getValue("xmi.id");
	    		assoc = createAssocation(associd);
		    }
		    
			public XMLMode visit(XMLElement element) {
				if ( element.matches("AssociationEnd")) {
					AssociationEndMode mode = new AssociationEndMode(element, endA == null);
				    if( endA == null ) 
				        endA = mode;
				    else
				        endB = mode;
				    return mode;
				}
				return visit(element, assoc);
			}
			
			/**
			 * Once recognised, mate the OWL properties.
			 */
			@Override
			public void leave() {
				if( endA != null && endB != null) {
					endA.mate(endB);
					endB.mate(endA);
				}
			}

			/**
			 * Interpret a UML AssociationEnd as an OWL ObjectProperty.
			 */
			private class AssociationEndMode extends BaseMode {
			    OntClass range;
			    ObjectProperty property;
			    int lower =-1, upper=-1;
				boolean composite, aggregate;
	
			    AssociationEndMode( XMLElement element, boolean sideA ) {
			    	property = createObjectProperty(element);
			    	if( property == null) 
			    		property = createObjectProperty(element, associd, sideA);
			    	if( property != null ) {
			    		packageDefines(property);
			    		if( assoc != null )
			    			property.addProperty(sideA? UML.roleAOf: UML.roleBOf, assoc);
			    	}
			    	range = findClass( element, "type");
			    	if( range == null )
			    		range = findClass( element, "participant");
			    	
			    	String agg = element.getAttributes().getValue("aggregation");
			    	if( agg != null) {
			    		composite = agg.equals("composite");
			    		aggregate = agg.equals("aggregate");
			    		if( composite )
			    			property.addProperty(UML.hasStereotype, UML.ofComposite);
			    		if( aggregate )
			    			property.addProperty(UML.hasStereotype, UML.ofAggregate);
			    	}
			    }
			    
				public XMLMode visit(XMLElement element) {
				    if ( element.matches("Multiplicity")) 
						return new MultiplicityMode();
					
				    else if ( element.matches("Class")) {
				        range = findClass(element);
				        return null;
				    }
				    else if( property != null)
				    	return visit(element, property);
				    else
				    	return null;
				}
				
				/**
				 * Interpret this association end in the context of its mate.
				 * Establish the OWL domain and rage.  Interpret the UML
				 * multiplicity as OWL functional and inverse functional
				 * property types.
				 * 
				 * @param other the other end of this association.
				 */
				public void mate(AssociationEndMode other) {
					if( property == null)
						return;
					if( other.range != null)
						property.addDomain(other.range);
					if( range != null ) 
						property.addRange(range);
					if( other.property != null )
						property.addInverseOf(other.property);
					if( upper == 1 )
						property.convertToFunctionalProperty();
					if( other.upper == 1)
						property.convertToInverseFunctionalProperty();
		    		if( other.composite )
		    			property.addProperty(UML.hasStereotype, UML.compositeOf);
		    		if( other.aggregate )
		    			property.addProperty(UML.hasStereotype, UML.aggregateOf);

				}
				
				/**
				 * Collect multiplicity information for one association end.
				 */
				private class MultiplicityMode extends BaseMode {
					public XMLMode visit(XMLElement element) {
						if ( element.matches("MultiplicityRange")) {
						    Attributes attrs = element.getAttributes();
					        lower = number(attrs, "lower");
					        upper = number(attrs, "upper");
							return null;
						}
						return this;
					}
					
					/**
					 *  Interpret a multiplicity attribute as a decimal.
					 */
					int number(Attributes atts, String name) {
						String value = atts.getValue(name);
						if( value != null ) {
							try {
								return Integer.parseInt(value);
							} catch( NumberFormatException e) {
								return -1;
							}
						}
						else
							return -1;
					}
				}	
			}
		}

		/**
		 * Interpret a UML enumeration as an OWL Class plus individuals.
		 */
		private class EnumerationMode extends BaseMode {
		    OntClass classResource;
			public EnumerationMode(XMLElement element) {
				classResource = createClass(element);
				classResource.addProperty(UML.hasStereotype, UML.enumeration);
				packageDefines(classResource);
			}
			
			public XMLMode visit(XMLElement element) {
				if ( matchDef( element, "EnumerationLiteral")) 
					return new IndividualMode(element);

				else 
					return visit(element, classResource);
			}
			
			private class IndividualMode extends BaseMode {
				OntResource indivResource;
				
				public IndividualMode(XMLElement element) {
					indivResource = createIndividual(element, classResource);
				}

				public XMLMode visit(XMLElement element) {
					return visit(element, indivResource);
				}
			}
		}
	
		/**
		 * Interpret a UML class as an OWL class.
		 */
		private class ClassMode extends BaseMode {
		    OntClass classResource;
			
			ClassMode (XMLElement element) {
				classResource = createClass(element);
				Attributes atts = element.getAttributes();
				String sxuid = atts.getValue("stereotype");
				if( sxuid != null ) {
					classResource.addProperty(UML.hasStereotype, createStereoType(sxuid));
				}
				packageDefines(classResource);
			}
				
			public XMLMode visit(XMLElement element) {
				if ( matchDef( element, "Attribute")) 
					return new AttributeMode(element);

	    		else if ( element.matches("Generalization"))
			        return new GeneralizationMode(element);

				else 
					return visit(element, classResource);
			}

			/**
			 * Interpret a UML attribute as an OWL property.
			 * 
			 * Make this a datatype property if the 
			 * object is marked as a UML data type.
			 * Otherwise, leave the propertype open 
			 * for later assignment in stereotype processing.
			 */
			private class AttributeMode extends BaseMode {
			    OntProperty attrResource;
					
				AttributeMode(XMLElement element) {
					attrResource =createAttributeProperty(element);
					attrResource.addDomain(classResource);
					packageDefines(attrResource);
					Resource type = findResource(element, "type");
					if( type != null)
						attrResource.addRange(type);
				}
						
				public XMLMode visit(XMLElement element) {
				    if ( element.matches("Class") || element.matches("Classifier") || element.matches("DataType")) {
						Resource type = findResource(element);
						if( type != null)
							attrResource.addRange(type);
						if ( element.matches("DataType"))
							attrResource.convertToDatatypeProperty();
						return null;
					}
				    else if( element.matches("Expression")) {
				    	String value = element.getAttributes().getValue("body");
				    	if( value != null ) {
				    		value = value.trim();
				    		while( value.startsWith("\"") && value.endsWith("\"") 
				    					||  value.endsWith("'") && value.endsWith("'")) {
				    				value = value.substring(1, value.length()-1).trim();
				    		}
				    		if(value.length() > 0) 
				    			attrResource.addProperty(UML.hasInitialValue, value);
				    	}
				    	return null;
				    }
					else
						return visit(element, attrResource);
				}
			}
		}
		
		private class DatatypeMode extends ClassMode {

			DatatypeMode(XMLElement element) {
				super(element);
				classResource.addProperty(UML.hasStereotype, UML.datatype);
			}
			
		}
	}

}
