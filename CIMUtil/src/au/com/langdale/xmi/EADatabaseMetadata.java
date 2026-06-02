/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface EADatabaseMetadata {

	// Table name constants corresponding to the EA project file database tables.
	String TABLE_t_package = "t_package";
	String TABLE_t_object = "t_object";
	String TABLE_t_connector = "t_connector";
	String TABLE_t_xref = "t_xref";
	String TABLE_t_objectproperties = "t_objectproperties";
	String TABLE_t_attribute = "t_attribute";
	String TABLE_t_attributetag = "t_attributetag";
	String TABLE_t_connectortag = "t_connectortag";
	//
	Set<String> tables = new HashSet<>(Arrays.asList( //
			TABLE_t_package, //
			TABLE_t_object, //
			TABLE_t_connector, //
			TABLE_t_xref, //
			TABLE_t_objectproperties, //
			TABLE_t_attribute, //
			TABLE_t_attributetag, //
			TABLE_t_connectortag //
	));

	// Column name constants corresponding to the EA project file database columns.
	// These names are case sensitive by design and should not be modified.
	String COL_Client = "Client";
	String COL_Abstract = "Abstract";
	String COL_Description = "Description";
	String COL_Parent_ID = "Parent_ID";
	String COL_Connector_ID = "Connector_ID";
	String COL_Note = "Note";
	String COL_Object_Type = "Object_Type";
	String COL_Package_ID = "Package_ID";
	String COL_PDATA1 = "PDATA1";
	String COL_VALUE = "VALUE";
	String COL_Value = "Value";
	String COL_Property = "Property";
	String COL_SourceRole = "SourceRole";
	String COL_DestRole = "DestRole";
	String COL_SourceCard = "SourceCard";
	String COL_DestCard = "DestCard";
	String COL_SourceRoleNote = "SourceRoleNote";
	String COL_End_Object_ID = "End_Object_ID";
	String COL_Start_Object_ID = "Start_Object_ID";
	String COL_DestRoleNote = "DestRoleNote";
	String COL_DestIsAggregate = "DestIsAggregate";
	String COL_SourceIsAggregate = "SourceIsAggregate";
	String COL_Connector_Type = "Connector_Type";
	String COL_ea_guid = "ea_guid";
	String COL_ID = "ID";
	String COL_Default = "Default";
	String COL_Name = "Name";
	String COL_Notes = "Notes";
	String COL_Direction = "Direction";
	String COL_Object_ID = "Object_ID";
	String COL_Scope = "Scope";
	String COL_ElementID = "ElementID";
	String COL_Classifier = "Classifier";
	String COL_IsStatic = "IsStatic";
	String COL_Const = "Const";
	String COL_Type = "Type";
	String COL_Stereotype = "Stereotype";
	String COL_LowerBound = "LowerBound";
	String COL_UpperBound = "UpperBound";
}