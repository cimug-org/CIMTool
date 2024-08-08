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

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;

import au.com.langdale.kena.OntResource;

public class EAPExtractor extends AbstractEAProjectExtractor {

	private Database db;

	public EAPExtractor(File file) {
		super(file);
	}

	public void run() throws EAProjectExtractorException {
		try {
			db = DatabaseBuilder.open(file);
			//
			loadStereotypesCache();
			loadTaggedValuesCache();
			gatherPackageIDs();
			extractPackages();
			extractClasses();
			extractAssociations();
			extractAttributes();
		} catch (IOException ioException) {
			ioException.printStackTrace(System.err);
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					ioException);
		} finally {
			try {
				db.close();
			} catch (IOException e) {
				// Do nothing...
			}
		}
	}

	protected void loadStereotypesCache() throws EAProjectExtractorException {
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

	protected void loadTaggedValuesCache() throws EAProjectExtractorException {
		taggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		Iterator it = getObjectPropertiesTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			if (taggedValuesMap.containsKey(row.getObjectID())) {
				List<TaggedValue> taggedValuesList = taggedValuesMap.get(row.getObjectID());
				taggedValuesList.add(new TaggedValue(row.getProperty(), row.getValue()));
			} else {
				List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
				taggedValuesList.add(new TaggedValue(row.getProperty(), row.getValue()));
				taggedValuesMap.put(row.getObjectID(), taggedValuesList);
			}
		}
	}

	protected void gatherPackageIDs() throws EAProjectExtractorException {
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

	protected void extractPackages() throws EAProjectExtractorException {
		Iterator it = getPackageTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			OntResource subject = packageIDs.getID(row.getPackageID());
			if (!subject.equals(UML.global_package)) {
				OntResource parent = packageIDs.getID(row.getParentID());
				subject.addIsDefinedBy(parent == null ? UML.global_package : parent);
				annotate(subject, row.getNotes());
				//
				addStereotypes(subject, row.getEAGUID());
				// For packages the Object_ID is actually located in the t_object table so we
				// must obtain that first...
				// addTaggedValues(subject, objectId);
			}
		}
	}

	protected void extractClasses() throws EAProjectExtractorException {
		Iterator it = getObjectTable().iterator();
		while (it.hasNext()) {
			Row row = new Row(it.next());
			if (row.getObjectType().equals("Class")) {
				OntResource subject = createClass(row.getXUID(), row.getName());
				objectIDs.putID(row.getObjectID(), subject);
				annotate(subject, row.getNote());
				OntResource parent = packageIDs.getID(row.getPackageID());
				subject.addIsDefinedBy(parent == null ? UML.global_package : parent);
				//
				addStereotypes(subject, row.getEAGUID());
				addTaggedValues(subject, row.getObjectID());
			}
		}
	}

	protected void extractAttributes() throws EAProjectExtractorException {
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
				addTaggedValues(subject, row.getObjectID());
			} else
				System.out.println("Could not find the domain of attribute " + row.getName() + ". Domain ID = "
						+ row.getObjectID());
		}
	}

	protected void extractAssociations() throws EAProjectExtractorException {
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
						Role rolea = extractProperty( //
								row.getXUID(), //
								source, //
								destin, //
								row.getDestRole(), //
								row.getDestRoleNote(), //
								row.getDestCard(), //
								row.getDestIsAggregate(), //
								true);
						Role roleb = extractProperty( //
								row.getXUID(), //
								destin, //
								source, //
								row.getSourceRole(), //
								row.getSourceRoleNote(), //
								row.getSourceCard(), //
								row.getSourceIsAggregate(), //
								false);
						rolea.mate(roleb);
						roleb.mate(rolea);
					}
				}
			}
		}
	}

	private Table getPackageTable() throws EAProjectExtractorException {
		Table table = null;
		try {
			table = db.getTable("t_package");
		} catch (IOException ioe) {
			throw new EAProjectExtractorException(ioe);
		}
		return table;
	}

	private Table getObjectTable() throws EAProjectExtractorException {
		Table table = null;
		try {
			table = db.getTable("t_object");
		} catch (IOException ioe) {
			throw new EAProjectExtractorException(ioe);
		}
		return table;
	}

	private Table getObjectPropertiesTable() throws EAProjectExtractorException {
		Table table = null;
		try {
			table = db.getTable("t_objectproperties");
		} catch (IOException ioe) {
			throw new EAProjectExtractorException(ioe);
		}
		return table;
	}

	private Table getConnectorTable() throws EAProjectExtractorException {
		Table table = null;
		try {
			table = db.getTable("t_connector");
		} catch (IOException ioe) {
			throw new EAProjectExtractorException(ioe);
		}
		return table;
	}

	private Table getXRefTable() throws EAProjectExtractorException {
		Table table = null;
		try {
			table = db.getTable("t_xref");
		} catch (IOException ioe) {
			throw new EAProjectExtractorException(ioe);
		}
		return table;
	}

	private Table getAttributeTable() throws EAProjectExtractorException {
		Table table = null;
		try {
			table = db.getTable("t_attribute");
		} catch (IOException ioe) {
			throw new EAProjectExtractorException(ioe);
		}
		return table;
	}

	private class Row {
		private Map fields;

		Row(Object raw) {
			fields = (Map) raw;
		}

		public String getDescription() {
			return getString("Description");
		}

		int getObjectID() {
			return getInt("Object_ID");
		}

		int getPackageID() {
			return getInt("Package_ID");
		}

		int getParentID() {
			return getInt("Parent_ID");
		}

		int getStartObjectID() {
			return getInt("Start_Object_ID");
		}

		int getEndObjectID() {
			return getInt("End_Object_ID");
		}

		int getClassifier() {
			Object raw = fields.get("Classifier");
			return raw != null ? Integer.parseInt(raw.toString()) : 0;
		}

		boolean hasClassifier() {
			return getClassifier() != 0;
		}

		String getEAGUID() {
			String eaGUID = fields.get("ea_guid").toString();
			return eaGUID;
		}

		String getXUID() {
			String xuid = fields.get("ea_guid").toString();
			return "_" + xuid.substring(1, xuid.length() - 1);
		}

		public String getClient() {
			return getString("Client");
		}

		String getName() {
			return getString("Name");
		}

		String getObjectType() {
			return getString("Object_Type");
		}

		String getConnectorType() {
			return getString("Connector_Type");
		}

		String getNote() {
			return getString("Note");
		}

		String getNotes() {
			return getString("Notes");
		}

		boolean hasDefault() {
			return fields.get("Default") != null;
		}

		String getDefault() {
			return fields.get("Default").toString();
		}

		String getDestRole() {
			return getString("DestRole");
		}

		String getDestRoleNote() {
			return getString("DestRoleNote");
		}

		String getDestCard() {
			return getString("DestCard");
		}

		int getDestIsAggregate() {
			return getInt("DestIsAggregate");
		}

		String getSourceRole() {
			return getString("SourceRole");
		}

		String getSourceRoleNote() {
			return getString("SourceRoleNote");
		}

		String getSourceCard() {
			return getString("SourceCard");
		}

		int getSourceIsAggregate() {
			return getInt("SourceIsAggregate");
		}
		
		int getInt( String name ) {
			Object raw = fields.get(name);
			return (raw instanceof Integer)? ((Integer)raw).intValue(): 0;
		}

		public String getProperty() {
			return getString("Property");
		}

		public String getValue() {
			return getString("Value");
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
