package au.com.langdale.xmi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import au.com.langdale.kena.OntResource;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;


public class EAPExtractor extends XMIModel {

	private Database db;
	private IDList packageIDs = new IDList(100);
	private IDList objectIDs = new IDList(2000);
	

	public EAPExtractor(File file) throws IOException {
		try {
			db = Database.open(file, true);
		}
		catch( IOException e) {
			if( e.getMessage().startsWith("Unsupported version"))
				;
		}
	}
	
	public void run() throws IOException {
		if( db == null )
			return;
		
		gatherPackageIDs();
		extractPackages();
		extractClasses();
		extractAssociations();
		extractAttributes();
	}

	private void gatherPackageIDs() throws IOException {
		OntResource top = createGlobalPackage();
		
		Iterator it = getPackageTable().iterator();
		while( it.hasNext()) {
			Row row = new Row(it.next());
			OntResource subject;
			if( row.getName().equals("Model")) 
				subject = top;
			else
				subject = createIndividual(row.getXUID(), row.getName(), UML.Package);
			packageIDs.putID(row.getPackageID(), subject);
		}
	}

	private void extractPackages() throws IOException {
		Iterator it = getPackageTable().iterator();
		while( it.hasNext()) {
			Row row = new Row(it.next());
			OntResource subject = packageIDs.getID(row.getPackageID());
			if(! subject.equals(UML.global_package)) {
				OntResource parent = packageIDs.getID( row.getParentID());
				subject.addIsDefinedBy(parent == null? UML.global_package: parent);
				annotate(subject, row.getNotes());
			}
		}
	}

	private void annotate(OntResource subject, String note) {
		if( note == null)
			return;
		
		note = note.trim();
		if( note.length() == 0)
			return;
		
		subject.addComment(note, LANG);
	}

	private void extractClasses() throws IOException {
		Iterator it = getObjectTable().iterator();
		while( it.hasNext()) {
			Row row = new Row(it.next());
			if( row.getObjectType().equals("Class")) {
				OntResource subject = createClass(row.getXUID(), row.getName());
				objectIDs.putID(row.getObjectID(), subject);
				annotate( subject, row.getNote());
				OntResource parent = packageIDs.getID( row.getPackageID());
				subject.addIsDefinedBy(parent == null? UML.global_package: parent);
				if( row.hasStereotype())
					subject.addProperty(UML.hasStereotype, createStereotypeByName(row.getStereotype()));
			}
//			else
//				System.out.println("Ignoring object type " + row.getObjectType() + " id " + row.getObjectID());
		}
	}

	private void extractAttributes() throws IOException {
		Iterator it = getAttributeTable().iterator();
		while( it.hasNext()) {
			Row row = new Row(it.next());
			OntResource id = objectIDs.getID(row.getObjectID());
			if( id != null ) {
				OntResource subject = createAttributeProperty(row.getXUID(), row.getName());
				subject.addDomain(id);
				annotate( subject, row.getNotes());
				if( row.hasClassifier()) {
					OntResource range = objectIDs.getID(row.getClassifier());
					if( range != null)
						subject.addRange(range);
					else
						System.out.println("Could not find the range of attribute " + row.getName() + ". Range ID = " + row.getClassifier());
				}
				if( row.hasDefault())
					subject.addProperty(UML.hasInitialValue, row.getDefault());
			}
			else
				System.out.println("Could not find the domain of attribute " + row.getName() + ". Domain ID = " + row.getObjectID());
		}
		
	}

	private void extractAssociations() throws IOException {
		Iterator it = getConnectorTable().iterator();
		while( it.hasNext()) {
			Row row = new Row(it.next());
			String type = row.getConnectorType();
			if( type.equals("Generalization") || type.equals("Association")) {
				OntResource source = objectIDs.getID(row.getStartObjectID());
				OntResource destin = objectIDs.getID(row.getEndObjectID());
				if( source != null && destin != null) {
					if( type.equals("Generalization")) {
						source.addSuperClass(destin);
					}
					else {
						Role rolea = extractProperty(row.getXUID(), destin, row.getDestRole(), row.getDestRoleNote(), row.getDestCard(), row.getDestIsAggregate(), true);
						Role roleb = extractProperty(row.getXUID(), source, row.getSourceRole(), row.getSourceRoleNote(), row.getSourceCard(), row.getSourceIsAggregate(), false);
						rolea.mate(roleb);
						roleb.mate(rolea);
					}
				}
				
			}
		}
	}

	private Role extractProperty(String xuid, OntResource destin, String name, String note, String card, boolean aggregate, boolean sideA) {
		Role role = new Role();
		role.property = createObjectProperty(xuid, sideA, name);
		annotate( role.property, note);
		role.range = destin;
		role.aggregate = aggregate;
		if( card.equals("1") || card.endsWith("..1"))
			role.upper = 1;
		if( card.equals("*") || card.startsWith("0.."))
			role.lower = 0;
		else
			role.lower = 1;
		return role;
	}

	private Table getPackageTable() throws IOException {
		return db.getTable("t_package");
	}

	private Table getObjectTable() throws IOException {
		return db.getTable("t_object");
	}

	private Table getConnectorTable() throws IOException {
		return db.getTable("t_connector");
	}

	private Table getAttributeTable() throws IOException {
		return db.getTable("t_attribute");
	}

	private class Row {
		private Map fields;

		Row(Object raw) {
			fields = (Map) raw;
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
			return raw != null ? Integer.parseInt(raw.toString()): 0;
		}
		
		boolean hasClassifier() {
			return fields.get("Classifier") != null;
		}

		String getXUID() {
			String xuid = fields.get("ea_guid").toString();
			return "_" + xuid.substring(1, xuid.length()-1);
		}
		
		String getName() {
			return fields.get("Name").toString();
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
		
		String getStereotype() {
			return fields.get("Stereotype").toString();
		}
		
		boolean hasStereotype() {
			return fields.get("Stereotype") != null;
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
		
		boolean getDestIsAggregate() {
			return getInt("DestIsAggregate") == 1;
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
		
		boolean getSourceIsAggregate() {
			return getInt("SourceIsAggregate") == 1;
		}
		
		int getInt( String name ) {
			Object raw = fields.get(name);
			return (raw instanceof Integer)? ((Integer)raw).intValue(): 0;
		}

		String getString( String name ) {
			Object raw = fields.get(name);
			return raw != null ? raw.toString(): "";
		}
	}
	
	@SuppressWarnings("serial")
	private class IDList extends ArrayList {
		
		public IDList(int size) {
			super(size);
		}

		public void putID(int index, OntResource id) {
			while( index > size()) {
				add(null);
			}
			add(index, id);
		}
		
		public OntResource getID(int index) {
			if( index >= size() ) 
				return null;
			return (OntResource) get(index);
		}
	}
}
