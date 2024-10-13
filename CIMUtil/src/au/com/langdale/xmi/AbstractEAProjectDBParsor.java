/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.com.langdale.kena.OntResource;

public abstract class AbstractEAProjectDBParsor extends AbstractEAProjectParser {

	public AbstractEAProjectDBParsor(File file) {
		super(file);
	}

	protected abstract void dbInit() throws EAProjectParserException;

	protected abstract Connection getConnection() throws EAProjectParserException;

	protected void loadStereotypesCache() throws EAProjectParserException {
		stereotypesMap = new HashMap<String, List<String>>();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_xref where Name = 'Stereotypes'");
			while (rs.next()) {
				String eaGUID = rs.getString(COL_Client);
				if (stereotypesMap.containsKey(eaGUID)) {
					List<String> stereotypesList = stereotypesMap.get(eaGUID);
					stereotypesList.add(rs.getString(COL_Description));
				} else {
					List<String> stereotypesList = new ArrayList<String>();
					stereotypesList.add(rs.getString(COL_Description));
					stereotypesMap.put(eaGUID, stereotypesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void loadTaggedValuesCaches() throws EAProjectParserException {
		packagesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		classesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();

			/** This query retrieves all tagged values on packages */
			rs = statement.executeQuery(
					"select * from t_object o, t_objectproperties tv where (tv.Object_ID = o.Object_ID) and (o.Object_Type = 'Package' or o.Object_Type = 'Class')");
			while (rs.next()) {
				if ("Package".equals(rs.getString(COL_Object_Type))) {
					int packageId = rs.getInt(COL_PDATA1);
					if (packagesTaggedValuesMap.containsKey(packageId)) {
						List<TaggedValue> taggedValuesList = packagesTaggedValuesMap.get(packageId);
						taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					} else {
						List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
						taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
						packagesTaggedValuesMap.put(packageId, taggedValuesList);
					}
				} else {
					int objectId = rs.getInt(COL_Object_ID);
					if (classesTaggedValuesMap.containsKey(objectId)) {
						List<TaggedValue> taggedValuesList = classesTaggedValuesMap.get(objectId);
						taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					} else {
						List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
						taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
						classesTaggedValuesMap.put(objectId, taggedValuesList);
					}
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		attributesTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		try {
			connection = getConnection();
			statement = connection.createStatement();

			/** This query retrieves all tagged values on attributes */
			rs = statement.executeQuery("select * from t_attribute a, t_attributetag tv where (tv.ElementID = a.ID)");
			while (rs.next()) {
				int elementId = rs.getInt(COL_ElementID);
				if (attributesTaggedValuesMap.containsKey(elementId)) {
					List<TaggedValue> taggedValuesList = attributesTaggedValuesMap.get(elementId);
					taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
				} else {
					List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
					taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					attributesTaggedValuesMap.put(elementId, taggedValuesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		associationsTaggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		try {
			connection = getConnection();
			statement = connection.createStatement();

			/** This query retrieves all tagged values on attributes */
			rs = statement.executeQuery(
					"select * from t_connector c, t_connectortag tv where (tv.ElementID = c.Connector_ID)");
			while (rs.next()) {
				int elementId = rs.getInt(COL_ElementID);
				if (associationsTaggedValuesMap.containsKey(elementId)) {
					List<TaggedValue> taggedValuesList = associationsTaggedValuesMap.get(elementId);
					taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
				} else {
					List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
					taggedValuesList.add(new TaggedValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					associationsTaggedValuesMap.put(elementId, taggedValuesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void gatherPackageIDs() throws EAProjectParserException {
		OntResource top = createGlobalPackage();

		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_package order by Name");
			while (rs.next()) {
				OntResource subject;
				if (rs.getString(COL_Name).equals("Model"))
					subject = top;
				else
					subject = createIndividual(getPackageEAGUID(rs), rs.getString(COL_Name), UML.Package);
				packageIDs.putID(rs.getInt(COL_Package_ID), subject);
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

	}

	protected void parsePackages() throws EAProjectParserException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			// For extracting packages we must perform a join to be able to pull in the
			// Object_ID for the package. The Object_ID is needed to located tagged values.
			rs = statement.executeQuery("select * from t_package");
			while (rs.next()) {
				int packageId = Integer.parseInt(rs.getString(COL_Package_ID));
				OntResource subject = packageIDs.getID(packageId);
				if (!subject.equals(UML.global_package)) {
					int parentPackageId = rs.getInt(COL_Parent_ID);
					OntResource parent = packageIDs.getID(parentPackageId);
					if (!parent.equals(UML.global_package))
						subject.addIsDefinedBy(parent);
					annotate(subject, rs.getString(COL_Notes));
					/**
					 * Should we ever need to add support for Stereotypes on Packages simply uncomment the line below
					 * to add them. Additionally, the XMIParser.PackageMode.visit() method would need to
					 * have the following code uncommented: return new StereotypeMode(element, packResource);
					 */
					// addStereotypes(subject, rs.getString(COL_ea_guid));
					addTaggedValuesToPackage(subject, packageId);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void parseClasses() throws EAProjectParserException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_object where Object_Type = 'Class'");
			while (rs.next()) {
				OntResource subject = createClass(getEAGUID(rs), rs.getString(COL_Name));
				int objectId = rs.getInt(COL_Object_ID);
				objectIDs.putID(objectId, subject);
				annotate(subject, rs.getString(COL_Note));
				OntResource parent = packageIDs.getID(rs.getInt(COL_Package_ID));
				if (!parent.equals(UML.global_package))
					subject.addIsDefinedBy(parent);
				//
				addStereotypes(subject, rs.getString(COL_ea_guid));
				addTaggedValuesToClass(subject, objectId);
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void parseAttributes() throws EAProjectParserException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_attribute");
			while (rs.next()) {
				int objectId = rs.getInt(COL_Object_ID);
				OntResource id = objectIDs.getID(objectId);
				if (id != null) {
					String name = rs.getString(COL_Name);
					OntResource subject = createAttributeProperty(getEAGUID(rs), name);
					subject.addDomain(id);
					annotate(subject, rs.getString(COL_Notes));
					subject.addIsDefinedBy(id.getIsDefinedBy());
					if (hasClassifier(rs)) {
						int classifier = getClassifier(rs);
						OntResource range = objectIDs.getID(classifier);
						if (range != null)
							subject.addRange(range);
						else {
							System.out.println(
									"Could not find the range of attribute " + name + ". Range ID = " + classifier);
						}
					}
					String defaultValue = rs.getString(COL_Default);
					if (defaultValue != null && "".equals(defaultValue)) {
						subject.addProperty(UML.hasInitialValue, defaultValue);
					}
					//
					addStereotypes(subject, rs.getString(COL_ea_guid));
					addTaggedValuesToAttribute(subject, rs.getInt(COL_ID));
				} else {
					System.out.println("Could not find the domain of attribute " + rs.getString(COL_Name)
							+ ". Domain ID = " + rs.getInt(COL_Object_ID));
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void parseAssociations() throws EAProjectParserException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_connector");
			while (rs.next()) {
				String type = rs.getString(COL_Connector_Type);
				if (type.equals("Generalization") || type.equals("Association") || type.equals("Aggregation")) {
					OntResource source = objectIDs.getID(rs.getInt(COL_Start_Object_ID));
					OntResource destin = objectIDs.getID(rs.getInt(COL_End_Object_ID));
					if (source != null && destin != null) {
						if (type.equals("Generalization")) {
							source.addSuperClass(destin);
						} else {
							Role roleA = extractProperty( //
									getEAGUID(rs), //
									source, //
									destin, //
									rs.getString(COL_DestRole), //
									rs.getString(COL_DestRoleNote), //
									rs.getString(COL_DestCard), //
									rs.getInt(COL_DestIsAggregate), //
									true, //
									rs.getInt(COL_Connector_ID));
							Role roleB = extractProperty( //
									getEAGUID(rs), //
									destin, //
									source, //
									rs.getString(COL_SourceRole), //
									rs.getString(COL_SourceRoleNote), //
									rs.getString(COL_SourceCard), //
									rs.getInt(COL_SourceIsAggregate), //
									false, //
									rs.getInt(COL_Connector_ID));
							roleA.mate(roleB);
							roleB.mate(roleA);
						}
					}
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int getClassifier(ResultSet rs) throws SQLException {
		String classifier = rs.getString("Classifier");
		return classifier != null ? Integer.parseInt(classifier) : 0;
	}

	private boolean hasClassifier(ResultSet rs) throws SQLException {
		return getClassifier(rs) != 0;
	}

	private String getEAGUID(ResultSet rs) throws SQLException {
		String xuid = rs.getString(COL_ea_guid);
		return "EAID_" + xuid.substring(1, xuid.length() - 1).replace("-", "_");
	}
	
	private String getPackageEAGUID(ResultSet rs) throws SQLException {
		String xuid = rs.getString(COL_ea_guid);
		return "EAPK_" + xuid.substring(1, xuid.length() - 1).replace("-", "_");
	}

}
