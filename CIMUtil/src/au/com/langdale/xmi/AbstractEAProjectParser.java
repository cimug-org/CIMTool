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

import au.com.langdale.kena.OntResource;

public abstract class AbstractEAProjectParser extends XMIModel implements EAProjectParser {

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

	protected File file;

	protected IDList packageIDs = new IDList(100);
	protected IDList objectIDs = new IDList(2000);

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

	public AbstractEAProjectParser(File file) {
		this.file = file;
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
		
		if (card.equals("1") || card.endsWith("..1"))
			role.upper = 1;
		
		// Keeping this around...need to test to see if it is needed and should be uncommented...
		// if so we'd need to also look at the XMIModel / XMIParser classes for initializing upper bound..

		// else if (!card.endsWith("..1") && !card.endsWith("..*"))
		// 	role.upper = Integer.parseInt(card.substring(card.lastIndexOf(".") + 1));
		
		if (card.equals("*") || card.startsWith("0.."))
			role.lower = 0;
		else
			role.lower = 1;

		role.baseuri = getTaggedValueForAssociation(UML.baseuri, connectorId);
		role.baseprefix = getTaggedValueForAssociation(UML.baseprefix, connectorId);
		
		/**
		 * NOTE: We can and should support Stereotypes on connectors. We just need to
		 * determine what subject that they should be assigned to. Unsure of this.
		 * 
		 * select Description from t_xref where Client = 'ea_guid of t_connector' and Name = 'Stereotypes' and Type = 'connector property';
		 * 
		 * Stereotypes on connectors: 
		 * 
		 * addStereotypes(subject, row.getEAGUID());
		 */

		return role;
	}

}
