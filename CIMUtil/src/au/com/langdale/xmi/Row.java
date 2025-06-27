package au.com.langdale.xmi;

import java.util.Map;

class Row implements EADBColumns {
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
