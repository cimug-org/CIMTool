/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.sax.XMLElement;
import au.com.langdale.sax.XMLInterpreter;
import au.com.langdale.sax.XMLMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.RDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMIParser extends XMIModel {
	private static final Logger log = LoggerFactory.getLogger(XMIParser.class);
	
	private static final String STEREOTYPE = "Stereotype";
	private static final String DIAGRAM = "Diagram";
	private static final String SUBSYSTEM = "Subsystem";
	private static final String TAG_DEFINITION = "TagDefinition";
	private static final String GENERALIZATION = "Generalization";
	private static final String COMPONENT = "Component";
	private static final String CLASSIFIER_ROLE = "ClassifierRole";
	private static final String ENUMERATION = "Enumeration";
	private static final String PACKAGE = "Package";
	private static final String ATTRIBUTE = "Attribute";
	private static final String ASSOCIATION = "Association";
	private static final String MULTIPLICITY = "Multiplicity";
	private static final String ASSOCIATION_END = "AssociationEnd";
	private static final String CLASS = "Class";
	private static final String EXPRESSION = "Expression";
	private static final String DATA_TYPE = "DataType";
	private static final String CLASSIFIER = "Classifier";
	private static final String ASSOCIATION_CLASS = "AssociationClass";
	private static final String OPERATION = "Operation";
	private static final String COMMENT = "Comment";
	private static final String TAGGED_VALUE = "TaggedValue";
	private static final String MODEL_ELEMENT = "ModelElement";
	private static final String MULTIPLICITY_RANGE = "MultiplicityRange";
	private static final String ENUMERATION_LITERAL = "EnumerationLiteral";
	private static final String UPPER = "upper";
	private static final String LOWER = "lower";
	private static final String BODY = "body";
	private static final String PARENT = "parent";
	private static final String CHILD = "child";
	private static final String GENERALIZATION_PARENT = "Generalization.parent";
	private static final String GENERALIZATION_CHILD = "Generalization.child";
	private static final String NAME = "name";
	private static final String TAG = "tag";
	private static final String VALUE = "value";
	private static final String XMI_ID = "xmi.id";
	private static final String XMI_IDREF = "xmi.idref";
	private static final String TAGGED_VALUE_VALUE = "TaggedValue.value";
	private static final String TAGGED_VALUE_DATAVALUE = "TaggedValue.dataValue";
	private static final String EXTENDED_ELEMENT = "extendedElement";
	private static final String model_Element = "modelElement";
	
	/**
	 * Parse the given file as XMI producing an OWL model.
	 */
	public void parse(String fileName)
			throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
		parse(new InputSource(fileName));
	}

	public void parse(InputStream stream)
			throws IOException, SAXException, ParserConfigurationException, FactoryConfigurationError {
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
			}
		});

		// kick off in global package mode - can vacuum up elements that
		// slip outside the Model element in some XMI dialects
		reader.setContentHandler(new XMLInterpreter(new PackageMode()));
		reader.parse(source);
	}

	/**
	 * Base class for all parser modes.
	 */
	private abstract class BaseMode implements XMLMode {

		static Map<String, OntResource> taggedValueNamesMap = new HashMap<String, OntResource>();
		static Map<String, Map<OntResource, String>> packageTaggedValues = new HashMap<String, Map<OntResource, String>>();

		static int indentLevel = 0;
		static boolean debug = false;

		protected BaseMode() {
			enter();
		}

		protected final void enter() {
			if (debug) {
				indentLevel++;
				if (log.isDebugEnabled()) {
					StringBuilder spaces = new StringBuilder();
					for (int i = 1; i < indentLevel; i++)
						spaces.append("   ");
					String name = getClass().getName().contains("$")
							? getClass().getName().substring(getClass().getName().lastIndexOf("$") + 1)
							: getClass().getName();
					log.debug("{}Start: {}", spaces, name);
				}
			}
		}

		protected void exit() {
			if (debug) {
				if (log.isDebugEnabled()) {
					StringBuilder spaces = new StringBuilder();
					for (int i = 1; i < indentLevel; i++)
						spaces.append("   ");
					String name = getClass().getName().contains("$")
							? getClass().getName().substring(getClass().getName().lastIndexOf("$") + 1)
							: getClass().getName();
					appendText(spaces + "End: " + name + "  ");
				}
				indentLevel--;
			}
		}

		protected void appendText() {
			// default is just a carriage return — subclasses override to append text content
		}

		protected void appendText(String message) {
			log.debug("{}", message);
		}

		/**
		 * Common part of visit() for most model elements collects annotations and
		 * stereotypes.
		 */
		protected XMLMode visit(XMLElement element, OntResource resource) {

			if (element.matches(TAGGED_VALUE)) {
				return new TaggedValueMode(element, resource);
			} else if (element.matches(STEREOTYPE)) {
				return new StereotypeMode(element, resource);
			} else if (element.matches(COMMENT)) {
				String comment = element.getAttributes().getValue(BODY);
				if (comment != null)
					resource.addComment(comment, null);
				return null;
			} else if (element.matches(OPERATION))
				return null;
			else
				return this;

		}

		public void visit(XMLElement element, String text) {
			// ignore text nodes
		}

		/** Default implementation */
		public void leave() {
			exit();
		}

	}

	private class ClassifierRoleMode extends BaseMode {

		String name;

		ClassifierRoleMode(XMLElement element) {
			name = element.getAttributes().getValue(NAME);
			if (name != null && !packageTaggedValues.containsKey(name))
				packageTaggedValues.put(name, new HashMap<OntResource, String>());
		}

		@Override
		public XMLMode visit(XMLElement element) {
			if (element.matches(TAGGED_VALUE)) {
				return new TaggedValueMode(element, (OntResource) null);
			} else
				return visit(element, (OntResource) null);
		}

		public void leave() {
			super.leave();
		}

		private class TaggedValueMode extends BaseMode {
			
			OntResource subject;
			String tagValue;
			FrontsNode property;

			TaggedValueMode(XMLElement element) {
				this(element, null);
			}

			TaggedValueMode(XMLElement element, OntResource resource) {
				super();
				subject = resource;
				tagValue = element.getAttributes().getValue(VALUE);

				// in UML 1.3 documentation tags carry the comments
				String type = element.getAttributes().getValue(TAG);

				// in UML 1.4 the type is in the name attribute
				if (type == null)
					type = element.getAttributes().getValue(NAME);

				if (type != null)
					property = Translator.annotationResource(type);

				// we override the default subject here and in visit()
				OntResource ref = createUnknown(element.getAttributes().getValue(model_Element));
				if (ref != null)
					subject = ref;
			}

			public XMLMode visit(XMLElement element) {
				// handle a tag definition or a reference to one
				if (element.matches(TAG_DEFINITION)) {
					OntResource prop = createAnnotationProperty(element);
					/**
					 * The call to Translator.annotationResource() filters out unused tagged values.
					 */
					if (prop != null) {
						if (prop.getLabel() != null) {
							if (Translator.annotationResource(prop.getLabel()) != null)
								property = prop;
						} else if (prop.getLocalName() != null) {
							property = prop;
						}
					}
					return null;
				} else if (element.matches(TAGGED_VALUE_DATAVALUE) || element.matches(TAGGED_VALUE_VALUE)) {
					return new TaggedValueDataValueMode();
				} else if (element.matches(MODEL_ELEMENT)) {
					subject = createUnknown(element.getAttributes().getValue(XMI_IDREF));
					return null;
				} else
					return this;
			}

			private class TaggedValueDataValueMode extends BaseMode {

				@Override
				public void visit(XMLElement element, String text) {
					if (tagValue == null)
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
				if (name != null && property != null && tagValue != null) {
					packageTaggedValues.get(name).put((OntResource) property, tagValue);
				}
				super.leave();
			}
		}
	}

	/**
	 * Interpret a UML tag instance as an owl annotation.
	 */
	private class TaggedValueMode extends BaseMode {
		OntResource subject;
		String tagValue;
		FrontsNode property;

		/**
		 * Constructor for an unknown subject (expect a modelElement reference)
		 */
		TaggedValueMode(XMLElement element) {
			this(element, null);
		}

		/**
		 * Construct for a known subject.
		 */
		TaggedValueMode(XMLElement element, OntResource resource) {
			super();
			subject = resource;
			tagValue = element.getAttributes().getValue(VALUE);

			// in UML 1.3 documentation tags carry the comments
			String type = element.getAttributes().getValue(TAG);

			// in UML 1.4 the type is in the name attribute
			if (type == null)
				type = element.getAttributes().getValue(NAME);

			if (type != null)
				property = Translator.annotationResource(type);

			// we override the default subject here and in visit()
			OntResource ref = createUnknown(element.getAttributes().getValue(model_Element));
			if (ref != null)
				subject = ref;
		}

		public XMLMode visit(XMLElement element) {

			// handle a tag definition or a reference to one
			if (element.matches(TAG_DEFINITION)) {
				OntResource prop = createAnnotationProperty(element);
				/**
				 * The call to translator.annotationResource() filters out unneeded tagged
				 * values.
				 */
				if (prop != null) {
					if (prop.getLabel() != null) {
						if (Translator.annotationResource(prop.getLabel()) != null)
							property = prop;
					} else if (prop.getLocalName() != null) {
						property = prop;
					}
				}
				return null;
			} else if (element.matches(TAGGED_VALUE_DATAVALUE) || element.matches(TAGGED_VALUE_VALUE)) {
				return new TaggedValueDataValueMode();
			} else if (element.matches(MODEL_ELEMENT)) {
				subject = createUnknown(element.getAttributes().getValue(XMI_IDREF));
				return null;
			} else
				return this;
		}

		private class TaggedValueDataValueMode extends BaseMode {

			@Override
			public void visit(XMLElement element, String text) {
				if (tagValue == null)
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
			if (subject != null && property != null && tagValue != null) {
				Translator.addTagValue(subject, property, tagValue.trim());
			}
			super.leave();
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
		protected final void extend(XMLElement element) {
			String list = element.getAttributes().getValue(EXTENDED_ELEMENT);
			if (stereo != null && list != null) {
				subject = null; // forget subject

				// parse list of subjects
				String[] xuids = list.split(" +");
				for (int ix = 0; ix < xuids.length; ix++) {
					OntResource given = createUnknown(xuids[ix]);
					if (given != null)
						given.addProperty(UML.hasStereotype, stereo);
				}
			}
		}

		public XMLMode visit(XMLElement element) {
			if (element.matches(MODEL_ELEMENT)) {
				subject = null; // forget the given subject
				OntResource given = createUnknown(element.getAttributes().getValue(XMI_IDREF));
				if (stereo != null && given != null) {
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
			if (stereo != null && subject != null) {
				subject.addProperty(UML.hasStereotype, stereo);
			}
			super.leave();
		}

		protected void appendText() {
			if (stereo != null && subject != null) {
				log.debug("{}:  stereotype -> {}", subject.getLabel(), stereo);
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
			packResource = createGlobalPackage();
			packageTaggedValues.clear(); // Reset when global package initialized.
		}

		/**
		 * Construct for a subordinate package.
		 */
		PackageMode(XMLElement element, PackageMode parent) {
			packResource = createPackage(element);
			parent.packageDefines(packResource);
			/**
			 * Initialize with an empty taggedValues map keyed off of the package's name.
			 */
			if (packResource.getLabel() != null && !packageTaggedValues.containsKey(packResource.getLabel())) {
				packageTaggedValues.put(packResource.getLabel(), new HashMap<OntResource, String>());
			}
		}

		final void packageDefines(OntResource res) {
			if (!packResource.equals(UML.global_package))
				res.addIsDefinedBy(packResource);
		}

		public XMLMode visit(XMLElement element) {
			if (element.matches(CLASSIFIER_ROLE)) {
				return new ClassifierRoleMode(element);
			} else if (element.matches(ASSOCIATION)) {
				return new AssociationMode(element);
			} else if (matchDef(element, CLASS)) {
				return new ClassMode(element);
			} else if (matchDef(element, ENUMERATION)) {
				return new EnumerationMode(element);
			} else if (matchDef(element, PACKAGE)) {
				return new PackageMode(element, this);
			} else if (matchDef(element, COMPONENT)) {
				createUnknown(element);
				return null;
			} else if (matchDef(element, ASSOCIATION_CLASS)) {
				// TODO: implement association classes
				return null;
			} else if (matchDef(element, DATA_TYPE)) {
				if (packResource.equals(UML.global_package)) {
					createUnknown(element); // top level datatypes are generally garbage
					return null;
				} else
					return new DatatypeMode(element);
			} else if (element.matches(GENERALIZATION)) {
				return new GeneralizationMode(element);
			} else if (matchDef(element, TAG_DEFINITION)) {
				OntResource prop = createAnnotationProperty(element);
				String name = element.getAttributes().getValue(NAME);
				if (name != null && Translator.annotationResource(name) != null) {
					if (!taggedValueNamesMap.containsKey(name))
						taggedValueNamesMap.put(name, prop);
				}
				return null;
			} else if (element.matches(STEREOTYPE)) {
				// pick up stereotype and apply to package
				return new StereotypeMode(element, packResource);
			} else if (element.matches(DIAGRAM)) {
				return null; // chop off any diagrams
			} else if (element.matches(SUBSYSTEM)) {
				return null; // chop off subsystem definitions
			} else
				return visit(element, packResource);
		}

		public void leave() {
			if (packResource.equals(UML.global_package)) {
				ResIterator packages = model.listSubjectsWithProperty(RDF.type, UML.Package);
				packages.forEachRemaining((p) -> {
					OntResource aPackage = (OntResource) p;
					String packageName = aPackage.getLabel();
					Map<OntResource, String> map = packageTaggedValues.get(packageName);
					if (map != null && map.size() > 0) {
						map.forEach((property, tagValue) -> {
							if (property.getLabel() != null) {
								Translator.addTagValue(aPackage, property, tagValue);
							} else {
								property.setLabel(tagValue, LANG);
							}
						});
					}
				});
			}
			super.leave();
		}

		/**
		 * Interpret an XMI generalisation as an OWL subClass.
		 */
		private class GeneralizationMode extends BaseMode {
			OntResource ontChild;
			OntResource ontParent;

			GeneralizationMode(XMLElement element) {
				ontChild = findClass(element, CHILD);
				ontParent = findClass(element, PARENT);
			}

			public XMLMode visit(XMLElement element) {
				if (element.matches(GENERALIZATION_CHILD))
					return new GeneralizationChildMode();
				else if (element.matches(GENERALIZATION_PARENT))
					return new GeneralizationParentMode();
				else
					return this;
			}

			private class GeneralizationChildMode extends BaseMode {
				public XMLMode visit(XMLElement element) {
					if (element.matches(CLASS)) {
						ontChild = findClass(element);
						return null;
					}
					return this;
				}
			}

			private class GeneralizationParentMode extends BaseMode {

				public XMLMode visit(XMLElement element) {
					if (element.matches(CLASS)) {
						ontParent = findClass(element);
						return null;
					}
					return this;
				}
			}

			@Override
			public void leave() {
				if (ontChild != null && ontParent != null) {
					ontParent.addSubClass(ontChild);
				}
				super.leave();
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
				associd = element.getAttributes().getValue(XMI_ID);
				assoc = createAssocation(associd);
			}

			public XMLMode visit(XMLElement element) {
				if (element.matches(TAGGED_VALUE)) {
					return new TaggedValueMode(element, assoc) {
						@Override
						public void leave() {
							if (subject != null && property != null && tagValue != null) {
								Translator.addTagValue(subject, property, tagValue.trim());
							}
							super.leave();
						}
					};
				} else if (element.matches(ASSOCIATION_END)) {
					AssociationEndMode mode = new AssociationEndMode(element, endA != null);
					if (endA == null)
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
				if (endA != null && endB != null) {
					String baseprefix = assoc.getString(UML.baseprefix);
					if (baseprefix != null) {
						endA.role.baseprefix = baseprefix;
						endB.role.baseprefix = baseprefix;
					}
					String baseuri = assoc.getString(UML.baseuri);
					if (baseuri != null) {
						endA.role.baseuri = baseuri;
						endB.role.baseuri = baseuri;
					}
					endA.role.mate(endB.role);
					endB.role.mate(endA.role);
				}
				super.leave();
			}

			/**
			 * Interpret a UML AssociationEnd as an OWL ObjectProperty.
			 */
			private class AssociationEndMode extends BaseMode {
				Role role = new Role();

				AssociationEndMode(XMLElement element, boolean sideA) {
					role.sideA = sideA;
					role.property = createObjectProperty(element);
					if (role.property == null)
						role.property = createObjectProperty(element, associd, sideA);
					if (role.property != null) {
						packageDefines(role.property);
						if (assoc != null)
							role.property.addProperty(sideA ? UML.roleAOf : UML.roleBOf, assoc);
					}
					role.range = findClass(element, "type");
					if (role.range == null)
						role.range = findClass(element, "participant");

					String agg = element.getAttributes().getValue("aggregation");
					if (agg != null) {
						role.composite = agg.equals("composite");
						role.aggregate = agg.equals("aggregate");
					}
				}

				public XMLMode visit(XMLElement element) {
					if (element.matches(MULTIPLICITY))
						return new MultiplicityMode();

					else if (element.matches(CLASS)) {
						role.range = findClass(element);
						return null;
					} else if (role.property != null)
						return visit(element, role.property);
					else
						return null;
				}

				/**
				 * Collect multiplicity information for one association end.
				 */
				private class MultiplicityMode extends BaseMode {
					public XMLMode visit(XMLElement element) {
						if (element.matches(MULTIPLICITY_RANGE)) {
							Attributes attrs = element.getAttributes();
							role.lower = numberLowerBound(attrs, "lower");
							role.upper = numberUpperBound(attrs, "upper");
							return null;
						}
						return this;
					}
				}
			}
		}

		/**
		 * Interpret a UML enumeration as an OWL Class plus individuals.
		 */
		private class EnumerationMode extends BaseMode {
			OntResource classResource;

			public EnumerationMode(XMLElement element) {
				classResource = createClass(element);
				classResource.addProperty(UML.hasStereotype, UML.enumeration);
				packageDefines(classResource);
			}

			public XMLMode visit(XMLElement element) {
				if (matchDef(element, ENUMERATION_LITERAL))
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
			
			OntResource classResource;

			ClassMode(XMLElement element) {
				classResource = createClass(element);
				Attributes atts = element.getAttributes();
				String sxuid = atts.getValue("stereotype");
				if (sxuid != null) {
					classResource.addProperty(UML.hasStereotype, createStereotype(sxuid));
				}
				packageDefines(classResource);
			}

			public XMLMode visit(XMLElement element) {
				if (matchDef(element, ATTRIBUTE))
					return new AttributeMode(element);

				else if (element.matches(GENERALIZATION))
					return new GeneralizationMode(element);

				else
					return visit(element, classResource);
			}

			/**
			 * Interpret a UML attribute as an OWL property.
			 * 
			 * Make this a datatype property if the object is marked as a UML data type.
			 * Otherwise, leave the propertype open for later assignment in stereotype
			 * processing.
			 */
			private class AttributeMode extends BaseMode {
				
				OntResource attrResource;

				AttributeMode(XMLElement element) {
					attrResource = createAttributeProperty(element);
					attrResource.addDomain(classResource);
					packageDefines(attrResource);
					OntResource type = findResource(element, "type");
					if (type != null)
						attrResource.addRange(type);
				}

				public XMLMode visit(XMLElement element) {
					if (element.matches(CLASS) || element.matches(CLASSIFIER) || element.matches(DATA_TYPE)) {
						OntResource type = findResource(element);
						if (type != null)
							attrResource.addRange(type);
						if (element.matches(DATA_TYPE))
							attrResource.convertToDatatypeProperty();
						return null;
					} else if (element.matches(EXPRESSION)) {
						String value = element.getAttributes().getValue(BODY);
						if (value != null) {
							value = value.trim();
							while (value.startsWith("\"") && value.endsWith("\"")
									|| value.endsWith("'") && value.endsWith("'")) {
								value = value.substring(1, value.length() - 1).trim();
							}
							if (value.length() > 0)
								attrResource.addProperty(UML.hasInitialValue, value);
						}
						return null;
					} else if (element.matches(MULTIPLICITY)) {
						return new MultiplicityMode();
					} else
						return visit(element, attrResource);
				}
				
				/**
				 * Collect multiplicity information for one association end.
				 */
				private class MultiplicityMode extends BaseMode {
					public XMLMode visit(XMLElement element) {
						if (element.matches(MULTIPLICITY_RANGE)) {
							Attributes attrs = element.getAttributes();
							int lower = numberLowerBound(attrs, LOWER);
							int upper = numberUpperBound(attrs, UPPER);
							attrResource.addProperty(UML.schemaMin, lower);
							attrResource.addProperty(UML.schemaMax, upper);
							return null;
						}
						return this;
					}

					/**
					 * Interpret lower bound multiplicity attribute as an integer.
					 */
					int numberLowerBound(Attributes atts, String name) {
						String value = atts.getValue(name);
						if (value != null) {
							try {
								return Integer.parseInt(value);
							} catch (NumberFormatException e) {
								return 0;
							}
						} else
							return 0;
					}

					/**
					 * Interpret upper bound multiplicity attribute as a decimal.
					 */
					int numberUpperBound(Attributes atts, String name) {
						String value = atts.getValue(name);
						if (value != null) {
							try {
								return Integer.parseInt(value);
							} catch (NumberFormatException e) {
								return 1;
							}
						} else
							return 1;
					}
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