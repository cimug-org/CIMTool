/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.OWL2;

import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

public abstract class AbstractEAProjectParser extends XMIModel implements EAProjectParser {
	
	// Default logger to Standard.out
	protected static SchemaImportLogger logger = new SchemaImportLoggerImpl(); 
	
	// Table name constants corresponding to the EA project file database tables.
	protected static final String TABLE_t_package = "t_package";
	protected static final String TABLE_t_object = "t_object";
	protected static final String TABLE_t_connector = "t_connector";
	protected static final String TABLE_t_xref = "t_xref";
	protected static final String TABLE_t_objectproperties = "t_objectproperties";
	protected static final String TABLE_t_attribute = "t_attribute";
	protected static final String TABLE_t_attributetag = "t_attributetag";
	protected static final String TABLE_t_connectortag = "t_connectortag";

	// Column name constants corresponding to the EA project file database columns.
	// These names are case sensitive by design and should not be modified.
	protected static final String COL_Client = "Client";
	protected static final String COL_Description = "Description";
	protected static final String COL_Parent_ID = "Parent_ID";
	protected static final String COL_Connector_ID = "Connector_ID";
	protected static final String COL_Note = "Note";
	protected static final String COL_Object_Type = "Object_Type";
	protected static final String COL_Package_ID = "Package_ID";
	protected static final String COL_PDATA1 = "PDATA1";
	protected static final String COL_VALUE = "VALUE";
	protected static final String COL_Value = "Value";
	protected static final String COL_Property = "Property";
	protected static final String COL_SourceRole = "SourceRole";
	protected static final String COL_DestRole = "DestRole";
	protected static final String COL_SourceCard = "SourceCard";
	protected static final String COL_DestCard = "DestCard";
	protected static final String COL_SourceRoleNote = "SourceRoleNote";
	protected static final String COL_End_Object_ID = "End_Object_ID";
	protected static final String COL_Start_Object_ID = "Start_Object_ID";
	protected static final String COL_DestRoleNote = "DestRoleNote";
	protected static final String COL_DestIsAggregate = "DestIsAggregate";
	protected static final String COL_SourceIsAggregate = "SourceIsAggregate";
	protected static final String COL_Connector_Type = "Connector_Type";
	protected static final String COL_ea_guid = "ea_guid";
	protected static final String COL_ID = "ID";
	protected static final String COL_Default = "Default";
	protected static final String COL_Name = "Name";
	protected static final String COL_Notes = "Notes";
	protected static final String COL_Object_ID = "Object_ID";
	protected static final String COL_ElementID = "ElementID";
	protected static final String COL_Classifier = "Classifier";
	protected static final String COL_Type = "Type";
	protected static final String COL_Stereotype = "Stereotype"; // ONLY used to check for "enum" stereotype on attributes...
	protected static final String COL_LowerBound = "LowerBound"; // t_attribute min card
	protected static final String COL_UpperBound = "UpperBound"; // t_attribute max card
	//
	protected static final String STEREO_ENUMERATION = "enumeration";
	protected static final String STEREO_ENUM = "enum";
	//
	protected static final String OBJ_TYPE_CLASS = "Class";
	protected static final String OBJ_TYPE_PACKAGE = "Package";
	protected static final String OBJ_TYPE_ENUMERATION = "Enumeration";
	//
	protected static final String CONN_TYPE_AGGREGATION = "Aggregation";
	protected static final String CONN_TYPE_ASSOCIATION = "Association";
	protected static final String CONN_TYPE_GENERALIZATION = "Generalization";
	//
	protected static final String PKG_MODEL = "Model";

	protected File file;
	protected boolean selfHealOnImport;

	protected IDList packageIDs = new IDList(200);
	protected IDList objectIDs = new IDList(3000);

	private static String stereoPattern = "@STEREO;(.+?)@ENDSTEREO;";
	private static String name = "(.+?)=(.+?);";

	private static Pattern pattern = Pattern.compile(stereoPattern);
	private static Pattern namePattern = Pattern.compile(name);

	protected Map<String, List<String>> stereotypesMap = new HashMap<String, List<String>>();
	protected Map<Integer, List<TaggedValue>> packagesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
	protected Map<Integer, List<TaggedValue>> classesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
	protected Map<Integer, List<TaggedValue>> attributesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
	protected Map<Integer, List<TaggedValue>> associationsTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();

	protected abstract void dbInit() throws EAProjectParserException;

	protected abstract void dbShutdown() throws EAProjectParserException;

	public void parse() throws EAProjectParserException {
		// NOTE: Connection and Statement are AutoClosable. These should 
		// be closed within the dbShutdown method to avoid leaks.
		try {
			// We call dbInit for those databases that may need to perform
			// JDBC or database related initialization such as registering
			// a JDBC driver...
			dbInit();
			//
			loadStereotypesCache();
			loadTaggedValuesCaches();
			gatherPackageIDs();
			parsePackages();
			parseClasses();
			parseAssociations();
			parseAttributes();
			validateExtensions();
		} catch (EAProjectParserException eapException) {
			eapException.printStackTrace(System.err);
			throw eapException;
		} catch (Exception exception) {
			exception.printStackTrace(System.err);
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					exception);
		} finally {
			dbShutdown();
		}
	}
	
	public AbstractEAProjectParser(File file, boolean selfHealOnImport, SchemaImportLogger logger) {
		this.file = file;
		this.selfHealOnImport = selfHealOnImport;
		if (logger != null)
			this.importLogger = logger;
	}

	protected class TaggedValue {
		@Override
		public String toString() {
			return "TaggedValue [name=" + name + ", value=" + value + "]";
		}

		public String name;
		public String value;

		public TaggedValue(String name, String value) {
			this.name = name;
			this.value = (value != null ? value.trim() : value);
		}
	}

	@SuppressWarnings("serial")
	protected class IDList extends ArrayList {

		public IDList(int size) {
			super(size);
		}

		public void putID(int index, OntResource id) {
			while (index >= size()) {
				add(null);
			}
			set(index, id);
		}

		public OntResource getID(int index) {
			if (index >= size()) {
				return null;
			}
			OntResource res = (OntResource) get(index);
			return res;
		}
	}

	protected abstract void loadStereotypesCache() throws EAProjectParserException;

	protected abstract void loadTaggedValuesCaches() throws EAProjectParserException;

	protected abstract void gatherPackageIDs() throws EAProjectParserException;

	protected abstract void parsePackages() throws EAProjectParserException;

	protected abstract void parseClasses() throws EAProjectParserException;

	protected abstract void parseAssociations() throws EAProjectParserException;

	protected abstract void parseAttributes() throws EAProjectParserException;

	protected String getPackageHierarchy(OntResource parent) {
		String packageHierarchy = null;
		while (parent != null && !parent.equals(UML.global_package)) {
			String parentPackageName = parent.getLabel();
			packageHierarchy = (packageHierarchy != null ? parentPackageName + "::" + packageHierarchy : parentPackageName);
			parent = parent.getIsDefinedBy();
		}
		return (packageHierarchy == null ? "<Unknown Package>" : packageHierarchy);
		//return "";
	}
	
	protected String getPackageHierarchy(int packageId) {
		OntResource parent = packageIDs.getID(packageId);
		return getPackageHierarchy(parent);
	}
	
	protected String validatedCardinality(String cardFromUML) {
		String card = ""; // Defaults to empty string if cardFromUML is invalid...
		if (cardFromUML != null && cardFromUML.length() > 0) {
			if (cardFromUML.contains("..")) {
				if (!cardFromUML.startsWith("..") && !cardFromUML.endsWith("..")) {
					String minCard = cardFromUML.substring(0, cardFromUML.indexOf("."));
					String maxCard = cardFromUML.substring(cardFromUML.lastIndexOf(".") + 1);
					try {
						int parsedMinCard = Integer.parseInt(minCard);
						int parsedMaxCard = Integer.MAX_VALUE; // Default to unbounded
						if (!maxCard.toLowerCase().equals("n") && !maxCard.toLowerCase().equals("*")) {
							parsedMaxCard = Integer.parseInt(maxCard); 
						}
						if (parsedMinCard <= parsedMaxCard) {
							card = cardFromUML;
						}
					} catch (Exception e) {
					}
				}
			} else {
				try {
					// If we reach this point we assume that cardinality is declared as
					// a valid number such as "1" or "3" for example in which case we
					// simply parse the value to ensure it's a valid integer.
					Integer.valueOf(cardFromUML);
					card = cardFromUML;
				} catch (Exception e) {
				}
			}
		}
		return card;
	}
	
	protected void validateExtensions() {
		/**
		 * Pass one here addresses approach one described in the method comments.
		 */
		List<OntResource> shadowClasses = new ArrayList<OntResource>();
		ResIterator shadowExtensions = model.listSubjectsBuffered(UML.hasStereotype, UML.shadowextension);
		while (shadowExtensions.hasNext()) {
			OntResource shadowClass = shadowExtensions.nextResource();
			if (shadowClass.hasProperty(UML.hasStereotype, UML.cimdatatype) || shadowClass.hasProperty(UML.hasStereotype, UML.primitive)) {
				shadowClasses.add(shadowClass);
				//System.err.println("Shadow Extension:  " + shadowClass.describe());
			}
		}
	}
	
	protected void annotate(OntResource subject, String note) {
		if (note == null)
			return;

		note = note.trim();
		if (note.length() == 0)
			return;

		subject.addComment(note, LANG);
	}

	protected void addStereotypes(OntResource subject, String eaGUID) {
		if (stereotypesMap.containsKey(eaGUID)) {
			List<String> stereotypesList = stereotypesMap.get(eaGUID);
			for (String stereotypes : stereotypesList) {
				Matcher matcher = pattern.matcher(stereotypes);
				while (matcher.find()) {
					String description = matcher.group(1);
					Matcher stereoNameMatcher = namePattern.matcher(description);
					while (stereoNameMatcher.find()) {
						String groupName = stereoNameMatcher.group(1);
						if ("Name".equals(groupName)) {
							String stereoName = stereoNameMatcher.group(2);
							subject.addProperty(UML.hasStereotype, createStereotypeByName(stereoName));
						}
					}
				}
			}
		}
	}

	protected void addTaggedValuesToPackage(OntResource subject, int packageId) {
		if (packagesTaggedValuesMap.containsKey(packageId)) {
			List<TaggedValue> taggedValuesList = packagesTaggedValuesMap.get(packageId);
			for (TaggedValue taggedValue : taggedValuesList) {
				FrontsNode property = Translator.annotationResource(taggedValue.name);
				if (subject != null && property != null && taggedValue.value != null) {
					subject.addProperty(property, taggedValue.value);
				}
			}
		}
	}
	
	protected void addTaggedValuesToClass(OntResource subject, int objectId) {
		if (classesTaggedValuesMap.containsKey(objectId)) {
			List<TaggedValue> taggedValuesList = classesTaggedValuesMap.get(objectId);
			for (TaggedValue taggedValue : taggedValuesList) {
				FrontsNode property = Translator.annotationResource(taggedValue.name);
				if (subject != null && property != null && taggedValue.value != null) {
					subject.addProperty(property, taggedValue.value);
				}
			}
		}
	}

	protected void addTaggedValuesToAttribute(OntResource subject, int id) {
		if (attributesTaggedValuesMap.containsKey(id)) {
			List<TaggedValue> taggedValuesList = attributesTaggedValuesMap.get(id);
			for (TaggedValue taggedValue : taggedValuesList) {
				FrontsNode property = Translator.annotationResource(taggedValue.name);
				if (subject != null && property != null && taggedValue.value != null) {
					subject.addProperty(property, taggedValue.value);
				}
			}
		}
	}

	protected String getTaggedValueForAssociation(au.com.langdale.kena.Property prop, int connectorId) {
		if (associationsTaggedValuesMap.containsKey(connectorId)) {
			List<TaggedValue> taggedValuesList = associationsTaggedValuesMap.get(connectorId);
			for (TaggedValue taggedValue : taggedValuesList) {
				if (taggedValue.name.equals(prop.getLocalName())) {
					return taggedValue.value;
				}
			}
		}
		return null;
	}

	protected Role extractProperty(String xuid, OntResource source, OntResource destin, String name, String note,
			String card, int aggregate, boolean sideA, int connectorId) {
		Role role = new Role();
		role.sideA = sideA;
		role.property = createObjectProperty(xuid, sideA, name);
		annotate(role.property, note);
		role.property.addIsDefinedBy(source.getIsDefinedBy()); // FIXME: the package of an association is not always
																// that of the source class
		role.range = destin;
		
		switch (aggregate) {
		case 1: // Aggregate
			role.aggregate = true;
			role.composite = false;
			break;
		case 2: // Composite
			role.aggregate = false;
			role.composite = true;
			break;
		default:
			role.aggregate = false;
			role.composite = false;
			break;
		}
		
		// Determine and set min cardinality 
		if (card.equals("*") || card.equals("n") || card.startsWith("0..")) {
			role.lower = 0;
		} else if (card.equals("1") || card.startsWith("1..")) {
			role.lower = 1;
		} else if (card.contains("..")) {
			// We know that the lower bounds is a numerical value > 1
			try {
				role.lower = Integer.parseInt(card.substring(0, card.indexOf(".")));
			} catch (Exception e1) {
			}
		}
		
		// Determine and set max cardinality 
		if (card.equals("1") || card.endsWith("..1")) {
			role.upper = 1;
		} else if (card.contains("..") && !card.endsWith("..*") && !card.endsWith("..n")) {
			// We know that the upper bounds is a numerical value > 1
			try {
				String maxCard = card.substring(card.lastIndexOf(".") + 1);
				role.upper = Integer.parseInt(maxCard);
			} catch (Exception e1) {
			}
		} else if (!card.contains("..") && !card.equals("*") && !card.equals("n")) {
			try {
				// We know that the upper bounds is a numerical value > 1
				role.upper = Integer.parseInt(card.substring(card.lastIndexOf(".") + 1));
			} catch (Exception e1) {
			}
		}
		
		role.baseuri = getTaggedValueForAssociation(UML.baseuri, connectorId);
		role.baseprefix = getTaggedValueForAssociation(UML.baseprefix, connectorId);
		
		return role;
	}

	public OntResource createCardinalityRestriction(OntResource domain, String uri, FrontsNode prop, FrontsNode cardinalityType, int card) {
		OntResource result = model.createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(cardinalityType, card);
		domain.addSuperClass(result);
		return result;
	}

}
