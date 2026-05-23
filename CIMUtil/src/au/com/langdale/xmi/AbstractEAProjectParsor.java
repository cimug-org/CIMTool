/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.easyrules.engine.CIMModellingGuideDBRulesValidator;
import au.com.langdale.easyrules.engine.DBModelRulesValidator;
import au.com.langdale.easyrules.engine.NoOpModelRulesValidator;
import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.easyrules.rules.utils.CIMRuleUtils;
import au.com.langdale.kena.OntResource;
import au.com.langdale.logging.SchemaImportLogger;
import au.com.langdale.logging.SchemaImportLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public abstract class AbstractEAProjectParsor extends XMIModel implements EAProjectParser, EADatabaseMetadata {

	private static final SchemaImportLogger logger = SchemaImportLoggerFactory.getLogger(AbstractEAProjectParsor.class);
	private static final Logger log = LoggerFactory.getLogger(AbstractEAProjectParsor.class);
	//
	private static final String STEREO_ENUM = "enum";
	//
	private static final String OBJ_TYPE_PACKAGE = "Package";
	private static final String OBJ_TYPE_ENUMERATION = "Enumeration";
	//
	private static final String CONN_TYPE_AGGREGATION = "Aggregation";
	private static final String CONN_TYPE_ASSOCIATION = "Association";
	private static final String CONN_TYPE_GENERALIZATION = "Generalization";

	protected File file;

	private IDList packageIDs = new IDList(50000);
	private IDList objectIDs = new IDList(50000);
	private Map<String, Map<String, Integer>> classifierMappings = new HashMap<>();

	private static String stereoPattern = "@STEREO;(.+?)@ENDSTEREO;";
	private static String name = "(.+?)=(.+?);";

	private static Pattern pattern = Pattern.compile(stereoPattern);
	private static Pattern namePattern = Pattern.compile(name);

	private Map<String, List<String>> stereotypesMap = new HashMap<String, List<String>>();
	private Map<String, OntResource> namedStereotypes = new HashMap<String, OntResource>();
	private Map<Integer, List<TagValue>> packagesTagValuesMap = new HashMap<Integer, List<TagValue>>();
	private Map<Integer, List<TagValue>> classesTagValuesMap = new HashMap<Integer, List<TagValue>>();
	private Map<Integer, List<TagValue>> attributesTagValuesMap = new HashMap<Integer, List<TagValue>>();
	private Map<Integer, List<TagValue>> associationsTagValuesMap = new HashMap<Integer, List<TagValue>>();
	private Set<String> stereotypeExtensions = new HashSet<>();
	//
	private String baseURI;
	private File namespacesFile;
	private boolean selfHealOnImport = false;
	private boolean validateModel = false;
	private boolean usePackageNames = false;

	private NamespaceResolver namespaceResolver;
	private StereotypedNamespaces stereotypedNamespaces;

	private DBModelRulesValidator validator;
	private List<RuleViolation> violations = new LinkedList<>();

	public AbstractEAProjectParsor(String baseURI, File file, boolean selfHealOnImport, boolean validateModel,
			boolean usePackageNames, SchemaImportLogger logger, File namespacesFile) {
		super();
		this.baseURI = baseURI;
		this.file = file;
		this.selfHealOnImport = selfHealOnImport;
		this.usePackageNames = usePackageNames;
		if (namespacesFile != null)
			this.namespacesFile = namespacesFile;
		if (logger != null)
			this.importLogger = logger;
		this.validateModel = validateModel;
	}

	public AbstractEAProjectParsor(String baseURI, File file, boolean selfHealOnImport, boolean validateModel,
			boolean usePackageNames, SchemaImportLogger logger, File namespacesFile, Set<String> stereotypeExtensions) {
		this(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger, namespacesFile);
		if (stereotypeExtensions != null && !stereotypeExtensions.isEmpty())
			this.stereotypeExtensions = stereotypeExtensions;
	}

	protected class TagValue {
		@Override
		public String toString() {
			return "TagValue [name=" + name + ", value=" + value + "]";
		}

		public String name;
		public String value;

		public TagValue(String name, String value) {
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

	protected abstract void dbInit() throws EAProjectParserException;

	protected abstract void dbShutdown() throws EAProjectParserException;

	protected abstract Connection getConnection() throws EAProjectParserException;

	public void parse() throws EAProjectParserException {
		try {
			// We call dbInit for those databases that may need to perform
			// JDBC or database related initialization such as registering
			// a JDBC driver...
			dbInit();
			//
			loadStereotypesCache();
			initializeDBModelRulesValidator();
			loadTagValuesCaches();
			gatherPackageIDs();
			parsePackages();
			parseClasses();
			parseAssociations();
			parseAttributes();
		} catch (EAProjectParserException eapException) {
			log.error("EA project parser failed for: {}", file.getAbsolutePath(), eapException);
			throw eapException;
		} catch (Exception exception) {
			log.error("Unexpected error reading EA project file: {}", file.getAbsolutePath(), exception);
			throw new EAProjectParserException("Unable to read the EA project file:  " + file.getAbsolutePath(),
					exception);
		} finally {
			dbShutdown();
		}
	}

	protected void loadStereotypesCache() throws EAProjectParserException {
		stereotypesMap = new HashMap<String, List<String>>();

		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select * from t_xref where Name = 'Stereotypes'")) {

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
				Map<String, OntResource> stereos = createNamedStereotypes(stereotypes);
				namedStereotypes.putAll(stereos);
				if (log.isTraceEnabled())
					log.trace("Loaded stereotype entry: GUID='{}' value='{}'", eaGUID, stereotypes);
			}

			if (log.isDebugEnabled())
				log.debug("Loaded {} stereotype entries from t_xref into cache", stereotypesMap.size());

			for (String stereotypeName : this.stereotypeExtensions) {
				OntResource s = createStereotypeByName(stereotypeName);
				namedStereotypes.put(s.getLabel(), s);
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

	protected void initializeDBModelRulesValidator() {
		if (namespacesFile != null) {
			stereotypedNamespaces = new StereotypedNamespaces(namespacesFile, namedStereotypes);
		} else {
			// A StereotypedNamespaces instance created via the default constructor
			// represents an "empty" namespace to stereotype mapping...
			stereotypedNamespaces = new StereotypedNamespaces();
		}
		//
		this.namespaceResolver = new ResourceNamespaceResolver(getModel(), stereotypedNamespaces, getBaseURI(),
				usePackageNames);
		//
		if (validateModel) {
			this.validator = new CIMModellingGuideDBRulesValidator(baseURI, namespaceResolver, selfHealOnImport);
		} else {
			this.validator = new NoOpModelRulesValidator();
		}
	};

	protected void loadTagValuesCaches() throws EAProjectParserException {
		packagesTagValuesMap = new HashMap<Integer, List<TagValue>>();
		classesTagValuesMap = new HashMap<Integer, List<TagValue>>();

		/**
		 * This first query retrieves all tag values on Packages, Classes and
		 * Enumerations. Technically there should not be Enumerations in the model as
		 * they should all be defined as Classes with <<enumeration>> stereotypes. They
		 * are included here if "self heal" mode is to work.
		 */
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(
						"select o.Object_ID as Object_ID, Property, Value, Object_Type, PDATA1 from t_object o, t_objectproperties tv where (tv.Object_ID = o.Object_ID) and (o.Object_Type = 'Package' or o.Object_Type = 'Class' or o.Object_Type = 'Enumeration')")) {

			while (rs.next()) {
				if (OBJ_TYPE_PACKAGE.equals(rs.getString(COL_Object_Type))) {
					int packageId = rs.getInt(COL_PDATA1);
					if (packagesTagValuesMap.containsKey(packageId)) {
						List<TagValue> tagValuesList = packagesTagValuesMap.get(packageId);
						tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					} else {
						List<TagValue> tagValuesList = new ArrayList<TagValue>();
						tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
						packagesTagValuesMap.put(packageId, tagValuesList);
					}
				} else {
					int objectId = rs.getInt(COL_Object_ID);
					if (classesTagValuesMap.containsKey(objectId)) {
						List<TagValue> tagValuesList = classesTagValuesMap.get(objectId);
						tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					} else {
						List<TagValue> tagValuesList = new ArrayList<TagValue>();
						tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
						classesTagValuesMap.put(objectId, tagValuesList);
					}
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}

		attributesTagValuesMap = new HashMap<Integer, List<TagValue>>();
		/** This query retrieves all tag values on attributes */
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement
						.executeQuery("select * from t_attribute a, t_attributetag tv where (tv.ElementID = a.ID)")) {

			while (rs.next()) {
				int elementId = rs.getInt(COL_ElementID);
				if (attributesTagValuesMap.containsKey(elementId)) {
					List<TagValue> tagValuesList = attributesTagValuesMap.get(elementId);
					tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
				} else {
					List<TagValue> tagValuesList = new ArrayList<TagValue>();
					tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					attributesTagValuesMap.put(elementId, tagValuesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}

		associationsTagValuesMap = new HashMap<Integer, List<TagValue>>();
		/** This query retrieves all tag values on associations */
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(
						"select * from t_connector c, t_connectortag tv where (tv.ElementID = c.Connector_ID)")) {

			while (rs.next()) {
				int elementId = rs.getInt(COL_ElementID);
				if (associationsTagValuesMap.containsKey(elementId)) {
					List<TagValue> tagValuesList = associationsTagValuesMap.get(elementId);
					tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
				} else {
					List<TagValue> tagValuesList = new ArrayList<TagValue>();
					tagValuesList.add(new TagValue(rs.getString(COL_Property), rs.getString(COL_Value)));
					associationsTagValuesMap.put(elementId, tagValuesList);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

	protected void gatherPackageIDs() throws EAProjectParserException {
		OntResource top = createGlobalPackage();

		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select * from t_package")) {

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
		}
	}

	protected void parsePackages() throws EAProjectParserException {

		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select * from t_package")) {

			while (rs.next()) {
				int packageId = Integer.parseInt(rs.getString(COL_Package_ID));
				OntResource aPackage = packageIDs.getID(packageId);
				if (!aPackage.equals(UML.global_package)) {
					int parentPackageId = rs.getInt(COL_Parent_ID);
					OntResource parent = packageIDs.getID(parentPackageId);
					if (parent != null) {
						aPackage.addIsDefinedBy(parent);
					} else {
						logger.logInvalidParentPackage(aPackage, parentPackageId);
					}
					annotate(aPackage, rs.getString(COL_Notes));
					//
					addStereotypes(aPackage, rs.getString(COL_ea_guid));
					addTagValuesToPackage(aPackage, packageId);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
		//
		Map<String, List<String>> packageNamesMap = new HashMap<>();
		packageIDs.forEach(item -> {
			if (item != null) {
				OntResource aPackage = (OntResource) item;
				if (aPackage.getLabel() != null && !aPackage.getLabel().isBlank()
						&& !"DetailedDiagram".equals(aPackage.getLabel())) {
					String packageName = (aPackage.getLabel().startsWith("Package_")
							? aPackage.getLabel().substring(aPackage.getLabel().indexOf("_") + 1)
							: aPackage.getLabel());
					//
					List<String> list = (packageNamesMap.containsKey(packageName) ? packageNamesMap.get(packageName)
							: new ArrayList<>());
					//
					StringBuffer theFullyQualifiedName = new StringBuffer();
					if (aPackage.equals(UML.global_package)) {
						theFullyQualifiedName.append(packageName);
					} else {
						String parentHierarchy = CIMRuleUtils.getPackageHierarchy(aPackage);
						theFullyQualifiedName.append(parentHierarchy).append((!parentHierarchy.isBlank() ? "::" : ""))
								.append(packageName);
					}
					list.add(theFullyQualifiedName.toString());
					packageNamesMap.put(packageName, list);
				}
			}
		});
		//
		// For validating packages we must perform a join to be able to
		// pull in the Scope (from t_object) for the package. Note that
		// t_object.PDATA1 is the column in the t_object table that
		// contains the Package_ID of the row in the t_package table.
		// So we join on that...
		StringBuffer sqlQuery = new StringBuffer();
		sqlQuery.append("SELECT t_package.Package_ID as Package_ID, t_object.Scope as Scope ");
		sqlQuery.append("FROM  t_object, t_package ");
		sqlQuery.append("WHERE ");
		sqlQuery.append("t_object.Object_Type = 'Package' and (t_object.PDATA1 = t_package.Package_ID) ");
		//
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sqlQuery.toString())) {

			while (rs.next()) {
				int packageId = Integer.parseInt(rs.getString(COL_Package_ID));
				OntResource aPackage = packageIDs.getID(packageId);
				if (!aPackage.equals(UML.global_package)) {
					this.validator.validatePackage(rs, aPackage, packageNamesMap, violations);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

	protected void parseClasses() throws EAProjectParserException {
		/**
		 * We've identified that some models have classes that do not "participate" in
		 * any associations nor are they the declared type for any attributes with the
		 * model. Prior to adding the additional where class filters below such "orphan"
		 * classes could potentially be the source of issues in the model.
		 */
		StringBuffer sqlQuery = new StringBuffer();
		sqlQuery.append("SELECT * ");
		sqlQuery.append("FROM t_object ");
		sqlQuery.append("WHERE ");
		sqlQuery.append("(Object_Type = 'Class' OR Object_Type = 'Enumeration') ");
		//
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sqlQuery.toString())) {

			while (rs.next()) {

				boolean isInvalidEnumerationDefinition = false;
				if (rs.getString(COL_Object_Type).equals(OBJ_TYPE_ENUMERATION))
					isInvalidEnumerationDefinition = true;

				String className = rs.getString(COL_Name);
				OntResource aClass = createClass(getEAGUID(rs), className);

				int objectId = rs.getInt(COL_Object_ID);
				objectIDs.putID(objectId, aClass);

				annotate(aClass, rs.getString(COL_Note));
				OntResource parent = packageIDs.getID(rs.getInt(COL_Package_ID));

				if (parent != null) {
					if (!parent.equals(UML.global_package))
						aClass.addIsDefinedBy(parent);
				} else {
					logger.logInvalidPackageForClass(aClass, rs.getInt(COL_Package_ID));
				}

				if (log.isTraceEnabled()) {
					log.trace("Registered class '{}' with Object_ID={}, Package_ID={}", className, objectId,
							rs.getInt(COL_Package_ID));
				}

				addStereotypes(aClass, rs.getString(COL_ea_guid));
				addTagValuesToClass(aClass, objectId);

				if (isInvalidEnumerationDefinition) {
					logger.logInvalidEnumerationDefinition(getPackageHierarchy(rs.getInt(COL_Package_ID)),
							rs.getString(COL_Name));

				}
				//
				if (!classifierMappings.containsKey(className)) {
					Map<String, Integer> classifiers = new HashMap<String, Integer>();
					classifiers.put(getEAGUID(rs), objectId);
					classifierMappings.put(className, classifiers);
				} else {
					Map<String, Integer> classifiers = classifierMappings.get(className);
					classifiers.put(getEAGUID(rs), objectId);
				}
				//
				this.validator.validateClass(rs, aClass, Map.of(), this.violations);
				//
				// Finally, if we are dealing with an invalid enumeration definition an
				// self healing is NOT enabled we remove the enumeration from the model.
				if (isInvalidEnumerationDefinition && !selfHealOnImport) {
					objectIDs.remove(objectId);
					// Removes the resource from the model both as a subject and as an object
					aClass.remove();
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

	protected void parseAttributes() throws EAProjectParserException {

		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select * from t_attribute")) {

			while (rs.next()) {
				int objectId = rs.getInt(COL_Object_ID);

				String declaredTypeForAttribute = rs.getString(COL_Type);
				OntResource attribute = null;
				OntResource domainOfAttribute = objectIDs.getID(objectId);

				if (log.isDebugEnabled())
					log.debug("t_attribute row: name='{}', Object_ID={}, Classifier={}, DeclaredType='{}'",
							rs.getString(COL_Name), objectId, rs.getInt(COL_Classifier),
							declaredTypeForAttribute != null ? declaredTypeForAttribute : "<null>");

				if (domainOfAttribute != null) {
					String attributeName = rs.getString(COL_Name);
					attribute = createAttributeProperty(getEAGUID(rs), attributeName);
					attribute.addDomain(domainOfAttribute);
					annotate(attribute, rs.getString(COL_Notes));
					attribute.addIsDefinedBy(domainOfAttribute.getIsDefinedBy());

					if (!STEREO_ENUM.equalsIgnoreCase(rs.getString(COL_Stereotype))) {

						int classifier = 0;

						if (rs.getInt(COL_Classifier) != 0) {
							classifier = rs.getInt(COL_Classifier);
						} else if (selfHealOnImport) {
							Map<String, Integer> classifiers = null;
							if (classifierMappings.containsKey(declaredTypeForAttribute)) {
								classifiers = classifierMappings.get(declaredTypeForAttribute);
								if (classifiers.size() == 1) {
									// Used the object ID of the one element that exists...
									classifier = classifiers.values().iterator().next();
								}
							} else if (declaredTypeForAttribute != null) {
								// For "self-healing" we infer a mapping for recognized
								// native EA types to CIM primitives...
								switch (declaredTypeForAttribute) {
								case "boolean":
									if (classifierMappings.containsKey("Boolean"))
										classifiers = classifierMappings.get("Boolean");
									break;
								case "short":
								case "int":
								case "integer":
								case "long":
									if (classifierMappings.containsKey("Integer"))
										classifiers = classifierMappings.get("Integer");
									break;
								case "float":
									if (classifierMappings.containsKey("Float"))
										classifiers = classifierMappings.get("Float");
									break;
								case "double":
									if (classifierMappings.containsKey("Decimal"))
										classifiers = classifierMappings.get("Decimal");
									break;
								case "char":
								case "string":
									if (classifierMappings.containsKey("String"))
										classifiers = classifierMappings.get("String");
									break;
								}
								//
								if (classifiers != null && classifiers.size() == 1) {
									// Used the object ID of the one element that exists...
									classifier = classifiers.values().iterator().next();
								}
							}
						}
						//
						if (classifier > 0 && (objectIDs.getID(classifier) != null)) {
							OntResource range = objectIDs.getID(classifier);
							if (log.isDebugEnabled())
								log.debug("  -> range resolved: Classifier={} -> '{}'", classifier,
										range.getLabel() != null ? range.getLabel() : range.getLocalName());
							attribute.addRange(range);
						} else {
							if (log.isDebugEnabled())
								log.debug("  -> range NOT resolved: classifier={}, classifier>0={}, inObjectIDs={}",
										classifier, classifier > 0,
										classifier > 0 ? (objectIDs.getID(classifier) != null ? "yes" : "no") : "N/A");
							String packageHierarchy = getPackageHierarchy(domainOfAttribute.getIsDefinedBy());
							String className = domainOfAttribute.getLabel();
							importLogger.logAttributeMissingRange(packageHierarchy, className, attributeName,
									rs.getString(COL_Type), classifier);
						}
					}

					String defaultValue = rs.getString(COL_Default);
					if (defaultValue != null && !"".equals(defaultValue)) {
						attribute.addProperty(UML.hasInitialValue, defaultValue);
					}
					//
					int lower = 0; // default lower bound in CIM for an attribute...
					int upper = 1; // default upper bound in CIM for an attribute...
					if (rs.getString(COL_LowerBound) != null && !rs.getString(COL_LowerBound).isBlank()) {
						try {
							lower = Integer.parseInt(rs.getString(COL_LowerBound));
						} catch (NumberFormatException nfe) {
						}
					}
					if (rs.getString(COL_UpperBound) != null && !rs.getString(COL_UpperBound).isBlank()) {
						String upperBound = rs.getString(COL_UpperBound).toLowerCase();
						if (upperBound.equals("n") || upperBound.equals("*")) {
							upper = Integer.MAX_VALUE;
						} else {
							try {
								upper = Integer.parseInt(upperBound);
							} catch (NumberFormatException nfe) {
							}
						}
					}
					attribute.addProperty(UML.schemaMin, lower);
					attribute.addProperty(UML.schemaMax, upper);
					//
					addStereotypes(attribute, rs.getString(COL_ea_guid));
					addTagValuesToAttribute(attribute, rs.getInt(COL_ID));
				} else {
					/**
					 * Handle missing domain type (i.e. the class that contains the current
					 * attribute)
					 */
					String attributeName = rs.getString(COL_Name);
					int domainObjectId = rs.getInt(COL_Object_ID);
					importLogger.logAttributeMissingDomain(attributeName, domainObjectId);
				}
				//
				if (attribute != null) {
					this.validator.validateAttribute(rs, attribute, Map.of(), violations);
				}
			}
		} catch (SQLException sqlException) {
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

	protected void parseAssociations() throws EAProjectParserException {

		try (Connection connection = getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select * from t_connector")) {

			while (rs.next()) {
				String type = rs.getString(COL_Connector_Type);
				if (type.equals(CONN_TYPE_GENERALIZATION) || type.equals(CONN_TYPE_ASSOCIATION)
						|| type.equals(CONN_TYPE_AGGREGATION)) {
					OntResource source = objectIDs.getID(rs.getInt(COL_Start_Object_ID));
					OntResource destin = objectIDs.getID(rs.getInt(COL_End_Object_ID));
					if (source != null && destin != null) {
						if (type.equals(CONN_TYPE_GENERALIZATION)) {
							source.addSuperClass(destin);
							addGeneralizationStereotypes(source, destin, rs.getString(COL_ea_guid));
						} else {
							/**
							 * Both source and destination cardinalities must be specified on an association
							 * in the CIM. If not specified we default to "0..1" and log an error message.
							 */
							String sourceCard = validatedCardinality(rs.getString(COL_SourceCard));
							String sourceRole = (rs.getString(COL_SourceRole) == null ? ""
									: rs.getString(COL_SourceRole));
							String destCard = validatedCardinality(rs.getString(COL_DestCard));
							String destRole = (rs.getString(COL_DestRole) == null ? "" : rs.getString(COL_DestRole));
							/**
							 * Confirm both source and destination cardinalities are valid otherwise log an
							 * error and default the invalid cardinality to "0..1" to allow processing to
							 * continue...
							 */
							if ("".equals(sourceCard)) {
								importLogger.logAssociationInvalidCardinality(true, type, rs.getString(COL_SourceCard),
										source, sourceRole, destin, destRole);
								sourceCard = "0..1";
							}
							if ("".equals(destCard)) {
								importLogger.logAssociationInvalidCardinality(false, type, rs.getString(COL_DestCard),
										source, sourceRole, destin, destRole);
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
							//
							this.validator.validateAssociation(rs, roleA.property, Map.of(), violations);
							this.validator.validateAssociation(rs, roleB.property, Map.of(), violations);
						}
					}
				}
			}
		} catch (SQLException sqlException) {
			importLogger.logException(sqlException);
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

	public List<RuleViolation> getRuleViolations() {
		return violations;
	}

	protected String getBaseURI() {
		return baseURI;
	}

	@Override
	public StereotypedNamespaces getStereotypedNamespaces() {
		return this.stereotypedNamespaces;
	}

	protected String getPackageHierarchy(OntResource parent) {
		String packageHierarchy = null;
		while (parent != null && !parent.equals(UML.global_package)) {
			String parentPackageName = parent.getLabel();
			packageHierarchy = (packageHierarchy != null ? parentPackageName + "::" + packageHierarchy
					: parentPackageName);
			parent = parent.getIsDefinedBy();
		}
		return (packageHierarchy == null ? "<Unknown Package>" : packageHierarchy);
	}

	protected String getPackageHierarchy(int packageId) {
		OntResource parent = packageIDs.getID(packageId);
		return getPackageHierarchy(parent);
	}

	protected String validatedCardinality(String cardFromUML) {
		String card = ""; // Defaults to empty string if cardFromUML is invalid...
		if (cardFromUML != null && cardFromUML.length() > 0) {
			if (cardFromUML.contains("..")) {
				if (!cardFromUML.startsWith("..") && !cardFromUML.endsWith("..")) {
					String minCard = cardFromUML.substring(0, cardFromUML.indexOf("."));
					String maxCard = cardFromUML.substring(cardFromUML.lastIndexOf(".") + 1);
					try {
						int parsedMinCard = Integer.parseInt(minCard);
						int parsedMaxCard = Integer.MAX_VALUE; // Default to unbounded
						if (!maxCard.toLowerCase().equals("n") && !maxCard.toLowerCase().equals("*")) {
							parsedMaxCard = Integer.parseInt(maxCard);
						}
						if (parsedMinCard <= parsedMaxCard) {
							card = cardFromUML;
						}
					} catch (Exception e) {
					}
				}
			} else {
				try {
					// If we reach this point we assume that cardinality is declared as
					// a valid number such as "1" or "3" for example in which case we
					// simply parse the value to ensure it's a valid integer.
					Integer.valueOf(cardFromUML);
					card = cardFromUML;
				} catch (Exception e) {
				}
			}
		}
		return card;
	}

	protected void annotate(OntResource subject, String note) {
		if (note == null)
			return;

		note = note.trim();
		if (note.length() == 0)
			return;

		subject.addComment(note, LANG);
	}

	protected Map<String, OntResource> createNamedStereotypes(String stereotypes) {
		Map<String, OntResource> resourcesMap = new HashMap<>();
		Matcher matcher = pattern.matcher(stereotypes);
		while (matcher.find()) {
			String description = matcher.group(1);
			Matcher stereoNameMatcher = namePattern.matcher(description);
			while (stereoNameMatcher.find()) {
				String groupName = stereoNameMatcher.group(1);
				if ("Name".equals(groupName)) {
					String stereoName = stereoNameMatcher.group(2);
					OntResource s = createStereotypeByName(stereoName);
					resourcesMap.put(s.getLabel(), s);
				}
			}
		}
		return resourcesMap;
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
							if (log.isDebugEnabled()) {
								log.debug("Applying stereotype '{}' to '{}'", stereoName,
										subject.getLabel() != null ? subject.getLabel() : subject.getLocalName());
							}
							subject.addProperty(UML.hasStereotype, createStereotypeByName(stereoName));
						}
					}
				}
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No stereotype entry found in cache for '{}' (GUID='{}')",
						subject.getLabel() != null ? subject.getLabel() : subject.getLocalName(), eaGUID);
			}
		}
	}

	protected void addGeneralizationStereotypes(OntResource source, OntResource destination, String connectorEAGUID) {
		if (stereotypesMap.containsKey(connectorEAGUID)) {
			List<String> stereotypesList = stereotypesMap.get(connectorEAGUID);
			for (String stereotypes : stereotypesList) {
				Matcher matcher = pattern.matcher(stereotypes);
				while (matcher.find()) {
					String description = matcher.group(1);
					Matcher stereoNameMatcher = namePattern.matcher(description);
					while (stereoNameMatcher.find()) {
						String groupName = stereoNameMatcher.group(1);
						if ("Name".equals(groupName)) {
							String stereoName = stereoNameMatcher.group(2);
							// Now create the axiom annotation as a blank node
							OntResource axiom = model.createResource();
							axiom.addProperty(RDF.type, OWL2.Axiom);
							axiom.addProperty(OWL2.annotatedSource, source);
							axiom.addProperty(OWL2.annotatedProperty, RDFS.subClassOf);
							axiom.addProperty(OWL2.annotatedTarget, destination);
							axiom.addProperty(UML.hasStereotype, createStereotypeByName(stereoName));
						}
					}
				}
			}
		}
	}

	protected void addTagValuesToPackage(OntResource subject, int packageId) {
		if (packagesTagValuesMap.containsKey(packageId)) {
			List<TagValue> tagValuesList = packagesTagValuesMap.get(packageId);
			for (TagValue tagValue : tagValuesList) {
				FrontsNode property = Translator.annotationResource(tagValue.name);
				if (subject != null && property != null && tagValue.value != null) {
					Translator.addTagValue(subject, property, tagValue.value);
				}
			}
		}
	}

	protected void addTagValuesToClass(OntResource subject, int objectId) {
		if (classesTagValuesMap.containsKey(objectId)) {
			List<TagValue> tagValuesList = classesTagValuesMap.get(objectId);
			for (TagValue tagValue : tagValuesList) {
				FrontsNode property = Translator.annotationResource(tagValue.name);
				if (subject != null && property != null && tagValue.value != null) {
					Translator.addTagValue(subject, property, tagValue.value);
				}
			}
		}
	}

	protected void addTagValuesToAttribute(OntResource subject, int id) {
		if (attributesTagValuesMap.containsKey(id)) {
			List<TagValue> tagValuesList = attributesTagValuesMap.get(id);
			for (TagValue tagValue : tagValuesList) {
				FrontsNode property = Translator.annotationResource(tagValue.name);
				if (subject != null && property != null && tagValue.value != null) {
					Translator.addTagValue(subject, property, tagValue.value);
				}
			}
		}
	}

	protected String getTaggedValueForAssociation(au.com.langdale.kena.Property prop, int connectorId) {
		if (associationsTagValuesMap.containsKey(connectorId)) {
			List<TagValue> tagValuesList = associationsTagValuesMap.get(connectorId);
			for (TagValue tagValue : tagValuesList) {
				if (tagValue.name.equals(prop.getLocalName())) {
					return tagValue.value;
				}
			}
		}
		return null;
	}

	protected Role extractProperty(String xuid, OntResource source, OntResource destin, String name, String note,
			String card, int aggregate, boolean sideA, int connectorId) {
		Role role = new Role();
		role.sideA = sideA;
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

		// Determine and set min cardinality
		if (card.equals("*") || card.equals("n") || card.startsWith("0..")) {
			role.lower = 0;
		} else if (card.equals("1") || card.startsWith("1..")) {
			role.lower = 1;
		} else if (card.contains("..")) {
			// We know that the lower bounds is a numerical value > 1
			try {
				role.lower = Integer.parseInt(card.substring(0, card.indexOf(".")));
			} catch (Exception e1) {
			}
		}

		// Determine and set max cardinality
		if (card.equals("1") || card.endsWith("..1")) {
			role.upper = 1;
		} else if (card.contains("..") && !card.endsWith("..*") && !card.endsWith("..n")) {
			// We know that the upper bounds is a numerical value > 1
			try {
				String maxCard = card.substring(card.lastIndexOf(".") + 1);
				role.upper = Integer.parseInt(maxCard);
			} catch (Exception e1) {
			}
		} else if (!card.contains("..") && !card.equals("*") && !card.equals("n")) {
			try {
				// We know that the upper bounds is a numerical value > 1
				role.upper = Integer.parseInt(card.substring(card.lastIndexOf(".") + 1));
			} catch (Exception e1) {
			}
		}

		role.baseuri = getTaggedValueForAssociation(UML.baseuri, connectorId);
		role.baseprefix = getTaggedValueForAssociation(UML.baseprefix, connectorId);

		return role;
	}

	public OntResource createCardinalityRestriction(OntResource domain, String uri, FrontsNode prop,
			FrontsNode cardinalityType, int card) {
		OntResource result = model.createIndividual(uri, OWL2.Restriction);
		result.addProperty(OWL2.onProperty, prop);
		result.addProperty(cardinalityType, card);
		domain.addSuperClass(result);
		return result;
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
