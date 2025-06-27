/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.langdale.kena.OntResource;
import au.com.langdale.logging.SchemaImportLogger;

public abstract class AbstractEAProjectDBParsor extends AbstractEAProjectParser {

	public AbstractEAProjectDBParsor(File file, boolean selfHealOnImport, SchemaImportLogger logger) {
		super(file, selfHealOnImport, logger);
	}

	protected abstract void dbInit() throws EAProjectParserException;

	protected abstract Connection getConnection() throws EAProjectParserException;

	protected void loadStereotypesCache() throws EAProjectParserException {
		stereotypesMap = new HashMap<String, List<String>>();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			Map<String, OntResource> stereotypedNamespaces = new HashMap<String, OntResource>();
			connection = getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery("select * from t_xref where Name = 'Stereotypes'");
			while (rs.next()) {
				String eaGUID = rs.getString(COL_Client);
				String stereotypes = rs.getString(COL_Description);
				if (stereotypesMap.containsKey(eaGUID)) {
					List<String> stereotypesList = stereotypesMap.get(eaGUID);
					stereotypesList.add(stereotypes);
				} else {
					List<String> stereotypesList = new ArrayList<String>();
					stereotypesList.add(stereotypes);
					stereotypesMap.put(eaGUID, stereotypesList);
				}
				Map<String,OntResource> stereos = createStereotypedNamespaces(stereotypes);
				stereotypedNamespaces.putAll(stereos);
			}
			StereotypedNamespaces.init(stereotypedNamespaces);
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

			/** 
			 * This query retrieves all tagged values on Packages, Classes and Enumerations. 
			 * Technically there should not be Enumerations in the model as they should all
			 * be defined as Classes with <<enumeration>> stereotypes. They are included here
			 * if "self heal" mode is to work. 
			 */
			rs = statement.executeQuery(
					"select o.Object_ID as Object_ID, Property, Value, Object_Type, PDATA1 from t_object o, t_objectproperties tv where (tv.Object_ID = o.Object_ID) and (o.Object_Type = 'Package' or o.Object_Type = 'Class' or o.Object_Type = 'Enumeration')");
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
			rs = statement.executeQuery("select * from t_package");
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
					if (parent != null) {
						subject.addIsDefinedBy(parent);
					} else {
						System.err.println("Package " + (subject.getLabel() != null ? subject.getLabel() : subject.getLocalName()) + " has invalid parent package ID: " + parentPackageId);
					}
					annotate(subject, rs.getString(COL_Notes));
					/**
					 * Should we ever need to add support for Stereotypes on Packages simply uncomment the line below
					 * to add them. Additionally, the XMIParser.PackageMode.visit() method would need to
					 * have the following code uncommented: return new StereotypeMode(element, packResource);
					 * 
					 * select P.Package_ID, P.Name, P.ea_guid, X.Description  from t_xref X, t_package P where X.Name = 'Stereotypes' and P.ea_guid = X.Client
					 */
					addStereotypes(subject, rs.getString(COL_ea_guid));
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
			/**
			 * We've identified that some models have classes that do not "participate" in any 
			 * associations nor are they the declared type for any attributes with the model. 
			 * Prior to adding the additional where class filters below such "orphan" classes 
			 * which could potentially be the source of issues in the model. 
			 */
			StringBuffer sqlQuery = new StringBuffer();
			sqlQuery.append("SELECT * ");
			sqlQuery.append("FROM t_object ");
			sqlQuery.append("WHERE ");
			sqlQuery.append("(Object_Type = 'Class' OR Object_Type = 'Enumeration') ");
			//
			rs = statement.executeQuery(sqlQuery.toString());
			while (rs.next()) {
				boolean isInvalidEnumeration = false;
				if (rs.getString(COL_Object_Type).equals("Enumeration")) {
					logger.logInvalidEnumerationDefinition(getPackageHierarchy(rs.getInt(COL_Package_ID)), rs.getString(COL_Name));
					isInvalidEnumeration = true; // We ignore and "convert"/"treat" it as a correctly defined enumeration...
				}
				if (!isInvalidEnumeration) {
					String className = rs.getString(COL_Name);
					OntResource subject = createClass(getEAGUID(rs), className);
					int objectId = rs.getInt(COL_Object_ID);
					objectIDs.putID(objectId, subject);
					
					if (classifierMappings.containsKey(className)) {
						// We only allow for a single classifier mapping. If one exists  
						// already we remove it and allow have no classifier entry...
						classifierMappings.remove(className);
					} else  {
						classifierMappings.put(className, objectId);
					}
					
					annotate(subject, rs.getString(COL_Note));
					OntResource parent = packageIDs.getID(rs.getInt(COL_Package_ID));
					if (parent != null) {
						if (!parent.equals(UML.global_package))
							subject.addIsDefinedBy(parent);
					} else {
						System.err.println("Class " + (subject.getLabel() != null ? subject.getLabel() : subject.getLocalName()) + " has invalid package ID: " + rs.getInt(COL_Package_ID));
					}
					//
					addStereotypes(subject, rs.getString(COL_ea_guid));
					addTaggedValuesToClass(subject, objectId);
				} else {
					// If self heal enabled then fix enumeration
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
				String declaredTypeForAttribute = rs.getString(COL_Type);
				OntResource domainOfAttribute = objectIDs.getID(objectId);
				if (domainOfAttribute != null) {
					String attributeName = rs.getString(COL_Name);
					OntResource subject = createAttributeProperty(getEAGUID(rs), attributeName);
					subject.addDomain(domainOfAttribute);
					annotate(subject, rs.getString(COL_Notes));
					subject.addIsDefinedBy(domainOfAttribute.getIsDefinedBy());
					
					if (!"enum".equalsIgnoreCase(rs.getString(COL_Stereotype))) {
						int classifier = 0;
						
						if (hasClassifier(rs)) {
							classifier = getClassifier(rs);
						} else if (selfHealOnImportEnabled()){
							classifier = 0;
							if (classifierMappings.containsKey(declaredTypeForAttribute)) {
								classifier = classifierMappings.get(declaredTypeForAttribute);
							}
						}
						
						if (classifier > 0 && (objectIDs.getID(classifier) != null)) {
							OntResource range = objectIDs.getID(classifier);
							subject.addRange(range);
						} else {
							String packageHierarchy = getPackageHierarchy(domainOfAttribute.getIsDefinedBy());
							String className = domainOfAttribute.getLabel();
							importLogger.logAttributeMissingRange(packageHierarchy, className, attributeName, rs.getString(COL_Type), classifier);
						}
					}
					
					String defaultValue = rs.getString(COL_Default);
					if (defaultValue != null && !"".equals(defaultValue)) {
						subject.addProperty(UML.hasInitialValue, defaultValue);
					}
					//
					int lower = 0; // default lower bound...
					int upper = Integer.MAX_VALUE; // default upper bound...
					if (rs.getString(COL_LowerBound) != null && !"".equals(rs.getString(COL_LowerBound))) {
						try {
							lower = Integer.parseInt(rs.getString(COL_LowerBound));
						} catch (NumberFormatException nfe) {
						}
					}
					if (rs.getString(COL_UpperBound) != null && !"".equals(rs.getString(COL_UpperBound))) {
						try {
							upper = Integer.parseInt(rs.getString(COL_UpperBound));
						} catch (NumberFormatException nfe) {
						}
					}
					subject.addProperty(UML.schemaMin, lower);
					subject.addProperty(UML.schemaMax, upper);
					//
					addStereotypes(subject, rs.getString(COL_ea_guid));
					addTaggedValuesToAttribute(subject, rs.getInt(COL_ID));
				} else {
					/**
					 * Handle missing domain type (i.e. class containing the current attribute)...
					 */
					String attributeName = rs.getString(COL_Name);
					int domainObjectId = rs.getInt(COL_Object_ID);
					importLogger.logAttributeMissingDomain(attributeName, domainObjectId);
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
				if (type.equals(CONN_TYPE_GENERALIZATION) || type.equals(CONN_TYPE_ASSOCIATION) || type.equals(CONN_TYPE_AGGREGATION)) {
					OntResource source = objectIDs.getID(rs.getInt(COL_Start_Object_ID));
					OntResource destin = objectIDs.getID(rs.getInt(COL_End_Object_ID));
					if (source != null && destin != null) {
						if (type.equals(CONN_TYPE_GENERALIZATION)) {
							source.addSuperClass(destin);
						} else {
							/**
							 * Both source and destination cardinalities must be specified on an association 
							 * in the CIM. If not specified we default to "0..1" and log an error message.
							 */
							String sourceCard = validatedCardinality(rs.getString(COL_SourceCard));
							String sourceRole = (rs.getString(COL_SourceRole) == null ? "" : rs.getString(COL_SourceRole));
							String destCard = validatedCardinality(rs.getString(COL_DestCard));
							String destRole = (rs.getString(COL_DestRole) == null ? "" : rs.getString(COL_DestRole));
							/**
							 *  Confirm both source and destination cardinalities are valid otherwise 
							 *  log an error and default the invalid cardinality to "0..1" to allow
							 *  processing to continue...
							 */
							if ("".equals(sourceCard)) {
								importLogger.logAssociationInvalidCardinality(true, type, rs.getString(COL_SourceCard), source, sourceRole, destin, destRole);
								sourceCard = "0..1";
							}
							if ("".equals(destCard)) {
								importLogger.logAssociationInvalidCardinality(false, type, rs.getString(COL_DestCard), source, sourceRole, destin, destRole);
								destCard = "0..1";
							}
							Role roleA = extractProperty( //
									getEAGUID(rs), //
									source, //
									destin, //
									destRole, //
									rs.getString(COL_DestRoleNote), //
									destCard, //
									rs.getInt(COL_DestIsAggregate), //
									true, //
									rs.getInt(COL_Connector_ID));
							Role roleB = extractProperty( //
									getEAGUID(rs), //
									destin, //
									source, //
									sourceRole, //
									rs.getString(COL_SourceRoleNote), //
									sourceCard, //
									rs.getInt(COL_SourceIsAggregate), //
									false, //
									rs.getInt(COL_Connector_ID));
							roleA.mate(roleB);
							roleB.mate(roleA);
							//
							addStereotypes(roleA.property, rs.getString(COL_ea_guid));
							addStereotypes(roleB.property, rs.getString(COL_ea_guid));
						}
					}
				}
			}
		} catch (SQLException sqlException) {
			importLogger.logException(sqlException);
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
		String classifier = rs.getString(COL_Classifier);
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
