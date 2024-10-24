/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;

import au.com.langdale.kena.OntResource;

public class EAPParser extends AbstractEAProjectParser {

	private Database db;

	public EAPParser(File file) {
		super(file);
	}

	protected void dbInit() throws EAProjectParserException {
		try {
			db = DatabaseBuilder.open(file);
		} catch (IOException ioException) {
			ioException.printStackTrace(System.err);
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					ioException);
		}
	}

	protected void dbShutdown() throws EAProjectParserException {
		try {
			db.close();
		} catch (IOException e) {
			// Do nothing...
		}
	}

	protected void loadStereotypesCache() throws EAProjectParserException {
		stereotypesMap = new HashMap<String, List<String>>();
		Iterator it = getXRefTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			if (row.getName().equals("Stereotypes")) {
				if (stereotypesMap.containsKey(row.getClient())) {
					List<String> stereotypesList = stereotypesMap.get(row.getClient());
					stereotypesList.add(row.getDescription());
				} else {
					List<String> stereotypesList = new ArrayList<String>();
					stereotypesList.add(row.getDescription());
					stereotypesMap.put(row.getClient(), stereotypesList);
				}
			}
		}
	}

	protected void loadTaggedValuesCaches() throws EAProjectParserException {
		/**
		 * Jackess doesn't support joins. This is a "poor man's join" to associate the
		 * package id to the object id...
		 */
		Map<Integer, Integer> objectIdToPackageMap = new HashMap<Integer, Integer>();
		Set<Integer> classIds = new TreeSet<Integer>();
		Iterator it = getObjectTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			switch (row.getObjectType()) {
			case "Class":
				classIds.add(row.getObjectID());
				break;
			case "Package":
				objectIdToPackageMap.put(row.getObjectID(), Integer.parseInt(row.getPDATA1()));
				break;
			}
		}

		packagesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		classesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		it = getObjectPropertiesTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			int objectId = row.getObjectID();
			if (classIds.contains(objectId)) {
				if (classesTaggedValuesMap.containsKey(objectId)) {
					List<TaggedValue> taggedValuesList = classesTaggedValuesMap.get(objectId);
					taggedValuesList.add(new TaggedValue(row.getProperty(), row.getValue()));
				} else {
					List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
					taggedValuesList.add(new TaggedValue(row.getProperty(), row.getValue()));
					classesTaggedValuesMap.put(objectId, taggedValuesList);
				}
			} else if (objectIdToPackageMap.containsKey(objectId)) {
				int packageId = objectIdToPackageMap.get(objectId);
				if (packagesTaggedValuesMap.containsKey(packageId)) {
					List<TaggedValue> taggedValuesList = packagesTaggedValuesMap.get(packageId);
					taggedValuesList.add(new TaggedValue(row.getProperty(), row.getValue()));
				} else {
					List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
					taggedValuesList.add(new TaggedValue(row.getProperty(), row.getValue()));
					packagesTaggedValuesMap.put(packageId, taggedValuesList);
				}
			}
		}

		/** This query retrieves all tagged values on attributes */
		attributesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		it = getAttributeTagTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			int elementId = row.getElementID();
			if (attributesTaggedValuesMap.containsKey(elementId)) {
				List<TaggedValue> taggedValuesList = attributesTaggedValuesMap.get(elementId);
				taggedValuesList.add(new TaggedValue(row.getProperty(), row.getVALUE()));
			} else {
				List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
				taggedValuesList.add(new TaggedValue(row.getProperty(), row.getVALUE()));
				attributesTaggedValuesMap.put(elementId, taggedValuesList);
			}
		}

		/** This query retrieves all tagged values on associations */
		associationsTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		it = getConnectorTagTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			int elementId = row.getElementID();
			if (associationsTaggedValuesMap.containsKey(elementId)) {
				List<TaggedValue> taggedValuesList = associationsTaggedValuesMap.get(elementId);
				taggedValuesList.add(new TaggedValue(row.getProperty(), row.getVALUE()));
			} else {
				List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
				taggedValuesList.add(new TaggedValue(row.getProperty(), row.getVALUE()));
				associationsTaggedValuesMap.put(elementId, taggedValuesList);
			}
		}
	}

	protected void gatherPackageIDs() throws EAProjectParserException {
		OntResource top = createGlobalPackage();

		Table table = getPackageTable();
		Iterator it = table.iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			OntResource subject;
			if (row.getName().equals("Model"))
				subject = top;
			else
				subject = createIndividual(row.getXUID(), row.getName(), UML.Package);
			packageIDs.putID(row.getPackageID(), subject);
		}
	}

	protected void parsePackages() throws EAProjectParserException {
		Iterator it = getPackageTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			int packageId = row.getPackageID();
			OntResource subject = packageIDs.getID(packageId);
			if (!subject.equals(UML.global_package)) {	
				int parentPackageId = row.getParentID();
				OntResource parent = packageIDs.getID(parentPackageId);
				if (!parent.equals(UML.global_package))
					subject.addIsDefinedBy(parent);
				annotate(subject, row.getNotes());
				/**
				 * Should we ever need to add support for Stereotypes on Packages simply uncomment the line 
				 * below to add them. Additionally, the XMIParser.PackageMode.visit() method would need to
				 * have the following code uncommented: return new StereotypeMode(element, packResource);
				 * as well as code in the AbstractEAProjectDBExtractor.extractPackages() method.
				 */
				// addStereotypes(subject, row.getEAGUID());
				addTaggedValuesToPackage(subject, packageId);
			}
		}
	}

	protected void parseClasses() throws EAProjectParserException {
		Iterator it = getObjectTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			if (row.getObjectType().equals("Class")) {
				OntResource subject = createClass(row.getXUID(), row.getName());
				objectIDs.putID(row.getObjectID(), subject);
				annotate(subject, row.getNote());
				OntResource parent = packageIDs.getID(row.getPackageID());
				if (!parent.equals(UML.global_package))
					subject.addIsDefinedBy(parent);
				//
				addStereotypes(subject, row.getEAGUID());
				addTaggedValuesToClass(subject, row.getObjectID());
			}
		}
	}

	protected void parseAttributes() throws EAProjectParserException {
		Iterator it = getAttributeTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			OntResource id = objectIDs.getID(row.getObjectID());
			if (id != null) {
				OntResource subject = createAttributeProperty(row.getXUID(), row.getName());
				subject.addDomain(id);
				annotate(subject, row.getNotes());
				subject.addIsDefinedBy(id.getIsDefinedBy());
				if (row.hasClassifier()) {
					OntResource range = objectIDs.getID(row.getClassifier());
					if (range != null)
						subject.addRange(range);
					else
						System.out.println("Could not find the range of attribute " + row.getName() + ". Range ID = "
								+ row.getClassifier());
				}
				if (row.hasDefault()) {
					subject.addProperty(UML.hasInitialValue, row.getDefault());
				}
				//
				addStereotypes(subject, row.getEAGUID());
				addTaggedValuesToAttribute(subject, row.getID());
			} else
				System.out.println("Could not find the domain of attribute " + row.getName() + ". Domain ID = "
						+ row.getObjectID());
		}
	}

	protected void parseAssociations() throws EAProjectParserException {
		Iterator it = getConnectorTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			String type = row.getConnectorType();
			if (type.equals("Generalization") || type.equals("Association") || type.equals("Aggregation")) {
				OntResource source = objectIDs.getID(row.getStartObjectID());
				OntResource destin = objectIDs.getID(row.getEndObjectID());
				if (source != null && destin != null) {
					if (type.equals("Generalization")) {
						source.addSuperClass(destin);
					} else {
						Role roleA = extractProperty( //
								row.getXUID(), //
								source, //
								destin, //
								row.getDestRole(), //
								row.getDestRoleNote(), //
								row.getDestCard(), //
								row.getDestIsAggregate(), //
								true, //
								row.getConnectorID());
						Role roleB = extractProperty( //
								row.getXUID(), //
								destin, //
								source, //
								row.getSourceRole(), //
								row.getSourceRoleNote(), //
								row.getSourceCard(), //
								row.getSourceIsAggregate(), //
								false, //
								row.getConnectorID());
						roleA.mate(roleB);
						roleB.mate(roleA);
					}
				}
			}
		}
	}

	private Table getPackageTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_package);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getObjectTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_object);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getObjectPropertiesTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_objectproperties);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getAttributeTagTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_attributetag);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getConnectorTagTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_connectortag);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getConnectorTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_connector);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getXRefTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_xref);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private Table getAttributeTable() throws EAProjectParserException {
		Table table = null;
		try {
			table = db.getTable(TABLE_t_attribute);
		} catch (IOException ioe) {
			throw new EAProjectParserException(ioe);
		}
		return table;
	}

	private class Row {
		private Map fields;

		Row(Object raw) {
			fields = (Map) raw;
		}

		public String getDescription() {
			return getString(COL_Description);
		}

		int getObjectID() {
			return getInt(COL_Object_ID);
		}

		int getElementID() {
			return getInt(COL_ElementID);
		}

		int getPackageID() {
			return getInt(COL_Package_ID);
		}

		String getPDATA1() {
			return getString(COL_PDATA1);
		}

		int getParentID() {
			return getInt(COL_Parent_ID);
		}

		int getConnectorID() {
			return getInt(COL_Connector_ID);
		}

		int getStartObjectID() {
			return getInt(COL_Start_Object_ID);
		}

		int getEndObjectID() {
			return getInt(COL_End_Object_ID);
		}

		int getClassifier() {
			Object raw = fields.get(COL_Classifier);
			return raw != null ? Integer.parseInt(raw.toString()) : 0;
		}

		boolean hasClassifier() {
			return getClassifier() != 0;
		}

		int getID() {
			return getInt(COL_ID);
		}

		String getEAGUID() {
			String eaGUID = fields.get(COL_ea_guid).toString();
			return eaGUID;
		}

		String getXUID() {
			String xuid = fields.get(COL_ea_guid).toString();
			return "_" + xuid.substring(1, xuid.length() - 1);
		}

		public String getClient() {
			return getString(COL_Client);
		}

		String getName() {
			return getString(COL_Name);
		}

		String getObjectType() {
			return getString(COL_Object_Type);
		}

		String getConnectorType() {
			return getString(COL_Connector_Type);
		}

		String getNote() {
			return getString(COL_Note);
		}

		String getNotes() {
			return getString(COL_Notes);
		}

		boolean hasDefault() {
			return fields.get(COL_Default) != null;
		}

		String getDefault() {
			return fields.get(COL_Default).toString();
		}

		String getDestRole() {
			return getString(COL_DestRole);
		}

		String getDestRoleNote() {
			return getString(COL_DestRoleNote);
		}

		String getDestCard() {
			return getString(COL_DestCard);
		}

		int getDestIsAggregate() {
			return getInt(COL_DestIsAggregate);
		}

		String getSourceRole() {
			return getString(COL_SourceRole);
		}

		String getSourceRoleNote() {
			return getString(COL_SourceRoleNote);
		}

		String getSourceCard() {
			return getString(COL_SourceCard);
		}

		int getSourceIsAggregate() {
			return getInt(COL_SourceIsAggregate);
		}

		public String getProperty() {
			return getString(COL_Property);
		}

		/**
		 * Yes, this looks wonky having two getValueXxxxCase() methods. The reason is
		 * that the Jackcess APIs that we utilize for accessing the EA 15.x project
		 * files are case sensitive when retrieving the column name as the key into the
		 * Row's column/value map. This causes an issue between two separate tables that
		 * manage tag values. Specifically, the t_attributetag.VALUE and
		 * t_objectproperties.Value columns in the EA database. Therefore, we have,
		 * oddly, two distinct methods to retrieve them from the respective tables.
		 */
		public String getVALUE() {
			return getString(COL_VALUE);
		}

		public String getValue() {
			return getString(COL_Value);
		}

		int getInt(String name) {
			Object raw = fields.get(name);
			return (raw instanceof Integer) ? ((Integer) raw).intValue() : 0;
		}

		String getString(String name) {
			Object raw = fields.get(name);
			return raw != null ? raw.toString() : "";
		}
	}

}
