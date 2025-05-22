/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	
	public EAPParser(File file, boolean selfHealOnImport, SchemaImportLogger logger) {
		super(file, selfHealOnImport, logger);
	}

	protected void dbInit() throws EAProjectParserException {
		try {
			db = DatabaseBuilder.open(file);
		} catch (IOException ioException) {
			importLogger.logException(ioException);
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
		 * Jacksess doesn't support joins. This is a "poor man's join" to associate the
		 * package id to the object id...
		 */
		Map<Integer, Integer> objectIdToPackageMap = new HashMap<Integer, Integer>();
		Set<Integer> classIds = new TreeSet<Integer>();
		Iterator it = getObjectTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			switch (row.getObjectType()) {
			case OBJ_TYPE_ENUMERATION:
			case OBJ_TYPE_CLASS:
				classIds.add(row.getObjectID());
				break;
			case OBJ_TYPE_PACKAGE:
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
			if (row.getName().equals(PKG_MODEL))
				subject = top;
			else
				subject = createIndividual(row.getXUID(), row.getName(), UML.Package);
			packageIDs.putID(row.getPackageID(), subject);
		}
	}

	protected void parsePackages() throws EAProjectParserException {
		System.out.println("Processing all packages in the model:");
		Iterator it = getPackageTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			int packageId = row.getPackageID();
			OntResource subject = packageIDs.getID(packageId);
			if (!subject.equals(UML.global_package)) {	
				int parentPackageId = row.getParentID();
				OntResource parent = packageIDs.getID(parentPackageId);
				if (parent != null) {
					if (!parent.equals(UML.global_package))
						subject.addIsDefinedBy(parent);
				} else {
					importLogger.logInvalidParentPackage(subject, parentPackageId);
				}
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
		importLogger.log("Processing all classes:");
		/**
		 * The following Set(s) are used to simulate a WHERE clause and 
		 * to flag any classes not actually used in the model.
		 */
		Set<Integer> enumLiteralsObjectIDs = new HashSet<Integer>();
		Set<Integer> attributesObjectIDs = new HashSet<Integer>();
		Set<Integer> connectorsStartObjectIDs = new HashSet<Integer>();
		Set<Integer> connectorsEndObjectIDs = new HashSet<Integer>();
		//
		// Collect IDs for all enumerations used as the declared type of 
		// an enum literal attribute.
		Iterator iterator = getAttributeTable().iterator();	
		iterator.forEachRemaining(r -> {
			Row row = new Row(r);
			// the t_attribute.Classifier column contains the t_object.Object_ID 
			// of the enumeration of the enum literal attribute...
			if (STEREO_ENUM.equals(row.getStereotype()))
				enumLiteralsObjectIDs.add(row.getObjectID());
		});
		//
		// Collect IDs for all classes used as the declared type of an attribute.
		iterator = getAttributeTable().iterator();	
		iterator.forEachRemaining(r -> {
			Row row = new Row(r);
			// the t_attribute.Classifier column contains the t_object.Object_ID 
			// of the class of the declare type of a standard attribute (i.e. a
			// non-enum literal...
			if (!STEREO_ENUM.equals(row.getStereotype()))
				attributesObjectIDs.add(row.getClassifier());
		});
		//
		// Collect IDs for all classes used as the source side of an association.
		iterator = getConnectorTable().iterator();
		iterator.forEachRemaining(r -> {
			Row row = new Row(r);
			String type = row.getConnectorType();
			if (type.equals(CONN_TYPE_GENERALIZATION) || type.equals(CONN_TYPE_ASSOCIATION) || type.equals(CONN_TYPE_AGGREGATION)) 
				connectorsStartObjectIDs.add(row.getStartObjectID());
		});
		//
		// Collect IDs for all classes used as the destination side of an association.
		iterator = getConnectorTable().iterator();
		iterator.forEachRemaining(r -> {
			Row row = new Row(r);
			String type = row.getConnectorType();
			if (type.equals(CONN_TYPE_GENERALIZATION) || type.equals(CONN_TYPE_ASSOCIATION) || type.equals(CONN_TYPE_AGGREGATION)) 
				connectorsEndObjectIDs.add(row.getEndObjectID());
		});
		//
		Iterator it = getObjectTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			if (row.getObjectType().equals(OBJ_TYPE_CLASS)) {
				int objectID = row.getObjectID();
				if (objectID > -1) {
					if (!enumLiteralsObjectIDs.contains(objectID) && !attributesObjectIDs.contains(objectID) && !connectorsStartObjectIDs.contains(objectID) && !connectorsEndObjectIDs.contains(objectID)) {
						OntResource parentPackage = packageIDs.getID(row.getPackageID());
						String packageHierarchy = getPackageHierarchy(parentPackage);
						if (parentPackage != null && !parentPackage.equals(UML.global_package)) {
							if (row.getStereotype() != null && row.getStereotype().equals(STEREO_ENUMERATION)) {
								importLogger.logClassUnusedInAttribute(packageHierarchy, row.getName());
							} else {
								importLogger.logClassUnusedInAttributeOrAssociation(packageHierarchy, row.getName());
							}
						} else if (parentPackage == null){
							importLogger.logOrphanedClass(packageHierarchy, row.getName());
						}
					}
					OntResource subject = createClass(row.getXUID(), row.getName());
					objectIDs.putID(objectID, subject);
					annotate(subject, row.getNote());
					OntResource parent = packageIDs.getID(row.getPackageID());
					if (parent != null) {
						if (!parent.equals(UML.global_package))
							subject.addIsDefinedBy(parent);
					} else {
						importLogger.logInvalidPackageForClass(subject, row.getPackageID());
					}
					//
					addStereotypes(subject, row.getEAGUID());
					addTaggedValuesToClass(subject, row.getObjectID());
				}
			} else if (row.getObjectType().equals(OBJ_TYPE_ENUMERATION)) {
				logger.logInvalidEnumerationDefinition(getPackageHierarchy(row.getPackageID()), row.getName());
			}
		}
	}

	protected void parseAttributes() throws EAProjectParserException {
		importLogger.log("Processing all attributes:");
		Iterator it = getAttributeTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			int objectID = row.getObjectID();
			OntResource domain = objectIDs.getID(objectID);
			if (domain != null) {
				OntResource attribute = createAttributeProperty(row.getXUID(), row.getName());
				attribute.addDomain(domain);
				annotate(attribute, row.getNotes());
				attribute.addIsDefinedBy(domain.getIsDefinedBy());
				if (row.hasClassifier()) {
					OntResource range = objectIDs.getID(row.getClassifier());
					if (range != null) {
						attribute.addRange(range);
					} else {
						String packageHierarchy = getPackageHierarchy(domain.getIsDefinedBy());
						String className = domain.getLabel();
						String attributeName = row.getName();
						importLogger.logAttributeMissingRange(packageHierarchy, className, attributeName, row.getType(), row.getClassifier());
					}
				}
				if (row.hasDefault()) {
					attribute.addProperty(UML.hasInitialValue, row.getDefault());
				}
				attribute.addProperty(UML.schemaMin, row.getLowerBound());
				attribute.addProperty(UML.schemaMax, row.getUpperBound());
				//
				addStereotypes(attribute, row.getEAGUID());
				addTaggedValuesToAttribute(attribute, row.getID());
			} else {
				/**
				 * Handle missing domain (i.e. the class the attribute is defined in)...
				 */
				String attributeName = row.getName();
				int domainObjectId = row.getObjectID();
				importLogger.logAttributeMissingDomain(attributeName, domainObjectId);
			}
		}
	}

	protected void parseAssociations() throws EAProjectParserException {
		importLogger.log("Processing all associations:");
		Iterator it = getConnectorTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			String type = row.getConnectorType();
			if (type.equals(CONN_TYPE_GENERALIZATION) || type.equals(CONN_TYPE_ASSOCIATION) || type.equals(CONN_TYPE_AGGREGATION)) {
				OntResource source = objectIDs.getID(row.getStartObjectID());
				OntResource destin = objectIDs.getID(row.getEndObjectID());
				if (source != null && destin != null) {
					if (type.equals(CONN_TYPE_GENERALIZATION)) {
						source.addSuperClass(destin);
					} else {
						/**
						 * Both source and destination cardinalities must be specified on an association 
						 * in the CIM. If not specified we default to "0..1" and log an error message.
						 */
						String sourceCard = validatedCardinality(row.getSourceCard());
						String sourceRole = (row.getSourceRole() == null ? "" : row.getSourceRole());
						String destCard = validatedCardinality(row.getDestCard());
						String destRole = (row.getDestRole() == null ? "" : row.getDestRole());
						/**
						 *  Confirm both source and destination cardinalities are valid else 
						 *  log an error and default the invalid cardinality to "0..1" to allow
						 *  processing to continue...
						 */
						if ("".equals(sourceCard)) {
							importLogger.logAssociationInvalidCardinality(true, type, row.getSourceCard(), source, sourceRole, destin, destRole);
							sourceCard = "0..1";
						}
						if ("".equals(destCard)) {
							importLogger.logAssociationInvalidCardinality(false, type, row.getDestCard(), source, sourceRole, destin, destRole);
							destCard = "0..1";
						}
						Role roleA = extractProperty( //
								row.getXUID(), //
								source, //
								destin, //
								destRole, //
								row.getDestRoleNote(), //
								destCard, //
								row.getDestIsAggregate(), //
								true, //
								row.getConnectorID());
						Role roleB = extractProperty( //
								row.getXUID(), //
								destin, //
								source, //
								sourceRole, //
								row.getSourceRoleNote(), //
								sourceCard, //
								row.getSourceIsAggregate(), //
								false, //
								row.getConnectorID());
						roleA.mate(roleB);
						roleB.mate(roleA);
						//
						addStereotypes(roleA.property, row.getEAGUID());
						addStereotypes(roleB.property, row.getEAGUID());
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
		
		String getType() {
			return getString(COL_Type);
		}
		
		int getLowerBound() {
			Object raw = fields.get(COL_LowerBound);
			if (raw != null) {
				try {
					return Integer.parseInt(raw.toString());
				} catch (NumberFormatException nfe) {
				}
			} 
			return 0;
		}
		
		int getUpperBound() {
			Object raw = fields.get(COL_UpperBound);
			if (raw != null) {
				try {
					return Integer.parseInt(raw.toString());
				} catch (NumberFormatException nfe) {
				}
			} 
			return 1;
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

		String getStereotype() {
			return getString(COL_Stereotype);
		}

		int getInt(String name) {
			Object raw = fields.get(name);
			return (raw != null && raw instanceof Integer) ? ((Integer) raw).intValue() : 0;
		}

		String getString(String name) {
			Object raw = fields.get(name);
			return raw != null ? raw.toString() : "";
		}
	}

}
