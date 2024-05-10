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

public abstract class AbstractEAProjectDBExtractor extends AbstractEAProjectExtractor {

	// Column name constants corresponding to the EA project file SQLite DB.
	private static final String COL_CLIENT = "Client";
	private static final String COL_DESCRIPTION = "Description";
	private static final String COL_PARENT_ID = "Parent_ID";
	private static final String COL_NOTE = "Note";
	private static final String COL_OBJECT_TYPE = "Object_Type";
	private static final String COL_PACKAGE_ID = "Package_ID";
	private static final String COL_VALUE = "Value";
	private static final String COL_PROPERTY = "Property";
	private static final String COL_SOURCE_ROLE = "SourceRole";
	private static final String COL_DEST_ROLE = "DestRole";
	private static final String COL_SOURCE_CARD = "SourceCard";
	private static final String COL_DEST_CARD = "DestCard";
	private static final String COL_SOURCE_ROLE_NOTE = "SourceRoleNote";
	private static final String COL_END_OBJECT_ID = "End_Object_ID";
	private static final String COL_START_OBJECT_ID = "Start_Object_ID";
	private static final String COL_DEST_ROLE_NOTE = "DestRoleNote";
	private static final String COL_DEST_IS_AGGREGATE = "DestIsAggregate";
	private static final String COL_SOURCE_IS_AGGREGATE = "SourceIsAggregate";
	private static final String COL_CONNECTOR_TYPE = "Connector_Type";
	private static final String COL_EA_GUID = "ea_guid";
	private static final String COL_DEFAULT = "Default";
	private static final String COL_NAME = "Name";
	private static final String COL_NOTES = "Notes";
	private static final String COL_OBJECT_ID = "Object_ID";

	public AbstractEAProjectDBExtractor(File file) {
		super(file);
	}

	protected abstract void dbInit() throws SQLException;
	
	protected abstract Connection getConnection() throws SQLException;
	
	public void run() throws EAProjectExtractorException {
		// NOTE: Connection and Statement are AutoClosable.
		// Don't forget to close them both in order to avoid leaks.
		try {
			// We call dbInit for those databases that may need to perform 
			// JDBC or database related initialization such as registering
			// a JDBC driver...
			dbInit();
			//
			loadStereotypesCache();
			loadTaggedValuesCache();
			gatherPackageIDs();
			extractPackages();
			extractClasses();
			extractAssociations();
			extractAttributes();
		} catch (EAProjectExtractorException eapException) {
			eapException.printStackTrace(System.err);
			throw eapException;
		} catch (SQLException sqlException) {
			sqlException.printStackTrace(System.err);
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
		} 
	}

	protected void loadStereotypesCache() throws EAProjectExtractorException {
		stereotypesMap = new HashMap<String, List<String>>();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(
					"select * from t_xref where Name = 'Stereotypes'");
			while (rs.next()) {
				String eaGUID = rs.getString(COL_CLIENT);
				if (stereotypesMap.containsKey(eaGUID)) {
					List<String> stereotypesList = stereotypesMap.get(eaGUID);
					stereotypesList.add(rs.getString(COL_DESCRIPTION));
				} else {
					List<String> stereotypesList = new ArrayList<String>();
					stereotypesList.add(rs.getString(COL_DESCRIPTION));
					stereotypesMap.put(eaGUID, stereotypesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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
	
	protected void loadTaggedValuesCache() throws EAProjectExtractorException {
		taggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_objectproperties");
			while (rs.next()) {
				int objectId = rs.getInt(COL_OBJECT_ID);
				if (taggedValuesMap.containsKey(objectId)) {
					List<TaggedValue> taggedValuesList = taggedValuesMap.get(objectId);
					taggedValuesList.add(new TaggedValue(rs.getString(COL_PROPERTY), rs.getString(COL_VALUE)));
				} else {
					List<TaggedValue> taggedValuesList = new ArrayList<TaggedValue>();
					taggedValuesList.add(new TaggedValue(rs.getString(COL_PROPERTY), rs.getString(COL_VALUE)));
					taggedValuesMap.put(objectId, taggedValuesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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
	
	protected void gatherPackageIDs() throws EAProjectExtractorException {
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
				if (rs.getString(COL_NAME).equals("Model"))
					subject = top;
				else
					subject = createIndividual(getXUID(rs), rs.getString(COL_NAME), UML.Package);
				packageIDs.putID(rs.getInt(COL_PACKAGE_ID), subject);
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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

	protected void extractPackages() throws EAProjectExtractorException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			// For extracting packages we must perform a join to be able to pull in the
			// Object_ID for the package. The Object_ID is needed to located tagged values.
			rs = statement.executeQuery(
					"select t_package.Package_ID AS Package_ID, t_package.Parent_ID AS Parent_ID, t_package.Notes AS Notes, t_package.ea_guid AS ea_guid, t_object.Object_ID AS Object_ID from t_package, t_object where t_package.Package_ID = t_object.Package_ID");
			while (rs.next()) {
				int packageId = rs.getInt(COL_PACKAGE_ID);
				String eaGUID = rs.getString(COL_EA_GUID);
				OntResource subject = packageIDs.getID(packageId);
				if (!subject.equals(UML.global_package)) {
					int objectId = rs.getInt(COL_OBJECT_ID);
					OntResource parent = packageIDs.getID(rs.getInt(COL_PARENT_ID));
					subject.addIsDefinedBy(parent == null ? UML.global_package : parent);
					annotate(subject, rs.getString(COL_NOTES));
					//
					addStereotypes(subject, eaGUID);
					addTaggedValues(subject, objectId);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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

	protected void extractClasses() throws EAProjectExtractorException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_object");
			while (rs.next()) {
				if ("Class".equals(rs.getString(COL_OBJECT_TYPE))) {
					OntResource subject = createClass(getXUID(rs), rs.getString(COL_NAME));
					int objectId = rs.getInt(COL_OBJECT_ID);
					objectIDs.putID(objectId, subject);
					annotate(subject, rs.getString(COL_NOTE));
					OntResource parent = packageIDs.getID(rs.getInt(COL_PACKAGE_ID));
					subject.addIsDefinedBy(parent == null ? UML.global_package : parent);
					//
					addStereotypes(subject, rs.getString(COL_EA_GUID));
					addTaggedValues(subject, objectId);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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

	protected void extractAttributes() throws EAProjectExtractorException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_attribute");
			while (rs.next()) {
				int objectId = rs.getInt(COL_OBJECT_ID);
				OntResource id = objectIDs.getID(objectId);
				if (id != null) {
					String name = rs.getString(COL_NAME);
					OntResource subject = createAttributeProperty(getXUID(rs), name);
					subject.addDomain(id);
					annotate(subject, rs.getString(COL_NOTES));
					subject.addIsDefinedBy(id.getIsDefinedBy());
					if (hasClassifier(rs)) {
						int classifier = getClassifier(rs);
						OntResource range = objectIDs.getID(classifier);
						if (range != null)
							subject.addRange(range);
						else
							System.out.println(
									"Could not find the range of attribute " + name + ". Range ID = " + classifier);
					}
					String defaultValue = rs.getString(COL_DEFAULT);
					if (defaultValue != null && "".equals(defaultValue)) {
						subject.addProperty(UML.hasInitialValue, defaultValue);
					}
					//
					addStereotypes(subject, rs.getString(COL_EA_GUID));
					addTaggedValues(subject, objectId);
				} else
					System.out.println("Could not find the domain of attribute " + rs.getString(COL_NAME)
							+ ". Domain ID = " + rs.getInt(COL_OBJECT_ID));
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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

	protected void extractAssociations() throws EAProjectExtractorException {
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_connector");
			while (rs.next()) {
				String type = rs.getString(COL_CONNECTOR_TYPE);
				if (type.equals("Generalization") || type.equals("Association")) {
					OntResource source = objectIDs.getID(rs.getInt(COL_START_OBJECT_ID));
					OntResource destin = objectIDs.getID(rs.getInt(COL_END_OBJECT_ID));
					if (source != null && destin != null) {
						if (type.equals("Generalization")) {
							source.addSuperClass(destin);
						} else {
							boolean sourceIsAggregate = rs.getInt(COL_SOURCE_IS_AGGREGATE) == 1;
							boolean destIsAggregate = rs.getInt(COL_DEST_IS_AGGREGATE) == 1;
							Role rolea = extractProperty(getXUID(rs), source, destin, rs.getString(COL_DEST_ROLE),
									rs.getString(COL_DEST_ROLE_NOTE), rs.getString(COL_DEST_CARD), destIsAggregate,
									true);
							Role roleb = extractProperty(getXUID(rs), destin, source, rs.getString(COL_SOURCE_ROLE),
									rs.getString(COL_SOURCE_ROLE_NOTE), rs.getString(COL_SOURCE_CARD),
									sourceIsAggregate, false);
							rolea.mate(roleb);
							roleb.mate(rolea);
						}

						// NOTE: We can and should support Stereotypes on connectors. We just need
						// to determine what subject that they should be assigned to. Unsure of this.
						// select Description from t_xref where Client = 'ea_guid of t_connector' and
						// Name = 'Stereotypes' and Type = 'connector property'; // Stereotypes on
						// connectors
					}
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectExtractorException("Unable to import the EA project file:  " + file.getAbsolutePath(), sqlException);
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

	private String getXUID(ResultSet rs) throws SQLException {
		String xuid = rs.getString(COL_EA_GUID);
		return "_" + xuid.substring(1, xuid.length() - 1);
	}

}
