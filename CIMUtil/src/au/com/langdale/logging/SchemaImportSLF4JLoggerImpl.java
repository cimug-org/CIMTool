package au.com.langdale.logging;

import org.slf4j.Logger;

import au.com.langdale.kena.OntResource;

/**
 * An SLF4J-backed implementation of {@link SchemaImportLogger}.
 *
 * <p>
 * Routes all schema import log events through a named SLF4J {@link Logger},
 * making them visible in the unified Logback pipeline and controllable via
 * {@code logback.xml} at the per-class level.
 *
 * <p>
 * Instances are created via {@link SchemaImportLoggerFactory#getLogger(Class)}
 * rather than directly. This is an interim implementation on the path to
 * removing the {@link SchemaImportLogger} facade entirely (Option 2), at which
 * point each calling class will hold its own {@code static final Logger}
 * directly.
 *
 * <p>
 * Level mapping from the original {@link SchemaImportConsoleLoggerImpl}:
 * <ul>
 * <li>Schema integrity errors (missing range, invalid package, missing domain,
 * invalid enumeration definition) → {@code ERROR}</li>
 * <li>Recoverable cardinality issues → {@code WARN} (cardinality defaults to
 * 0..1 and parsing continues)</li>
 * <li>Informational model quality notices (unused classes, orphaned elements) →
 * {@code INFO}</li>
 * <li>Unexpected exceptions → {@code ERROR} with full stack trace</li>
 * </ul>
 */
public class SchemaImportSLF4JLoggerImpl implements SchemaImportLogger {

	private final Logger logger;

	/**
	 * Creates a new instance wrapping the given SLF4J logger. Use
	 * {@link SchemaImportLoggerFactory#getLogger(Class)} rather than calling this
	 * constructor directly.
	 *
	 * @param logger the SLF4J logger to delegate to
	 */
	SchemaImportSLF4JLoggerImpl(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void log(String message) {
		if (message != null)
			logger.info(message);
	}

	@Override
	public void log(LogLevel level, String message) {
		if (message == null)
			return;
		switch (level) {
		case ERROR:
			logger.error(message);
			break;
		case WARN:
			logger.warn(message);
			break;
		default:
			logger.info(message);
			break;
		}
	}

	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName) {
		String fullyQualifiedAttribute = packageHierarchy + "::" + className + "." + attributeName;
		logger.error("The Range on attribute {} has an orphaned or invalid declared type in the model. "
				+ "From within Sparx EA you must reselect the desired declared type for the attribute.",
				fullyQualifiedAttribute);
	}

	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName,
			String attributeDeclaredType, int rangeObjectId) {
		String fullyQualifiedAttribute = packageHierarchy + "::" + className + "." + attributeName;
		String declaredType = (attributeDeclaredType != null && !"".equals(attributeDeclaredType)
				? attributeDeclaredType
				: "<Undeclared Type>");
		logger.error(
				"The Range on attribute '{}' is declared to be of type '{}' which is missing from the model. "
						+ "Range ID (t_attribute.Classifier) = {}.",
				fullyQualifiedAttribute, declaredType, rangeObjectId);
	}

	@Override
	public void logInvalidPackageForClass(OntResource subject, int packageId) {
		logger.error("Class {} has invalid package ID: {}",
				subject.getLabel() != null ? subject.getLabel() : subject.getLocalName(), packageId);
	}

	@Override
	public void logInvalidParentPackage(OntResource subject, int parentPackageId) {
		logger.error("Package {} has invalid parent package ID: {}",
				subject.getLabel() != null ? subject.getLabel() : subject.getLocalName(), parentPackageId);
	}

	@Override
	public void logAssociationInvalidCardinality(boolean roleA, String assocType, String card, OntResource source,
			String sourceRole, OntResource destin, String destRole) {
		logger.warn(
				"{} role end cardinality ['{}'] is invalid for {}: {} (Source Role: {}) -> {} (Dest Role: {}). "
						+ "Defaulting cardinality to 0..1.",
				roleA ? "Source" : "Destination", card, assocType.toLowerCase(),
				source.getLabel() != null && !"".equals(source.getLabel()) ? source.getLabel()
						: source.getLocalName(),
				!"".equals(sourceRole) ? sourceRole : "<Unspecified>",
				destin.getLabel() != null && !"".equals(destin.getLabel()) ? destin.getLabel()
						: destin.getLocalName(),
				!"".equals(destRole) ? destRole : "<Unspecified>");
	}

	@Override
	public void logAttributeMissingDomain(String attributeName, int domainObjectID) {
		logger.error("Could not find the class containing attribute '{}'. Domain ID (t_attribute.Object_ID) = {}.",
				attributeName, domainObjectID);
	}

	@Override
	public void logException(Exception exception) {
		logger.error("Unexpected exception during schema import", exception);
	}

	@Override
	public void logClassUnusedInAttribute(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		logger.info("Class '{}' is not used as the declared type of any attribute in the model.",
				fullyQualifiedClass);
	}

	@Override
	public void logClassUnusedInAttributeOrAssociation(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		logger.info(
				"Class '{}' is not used as the declared type of any attribute nor as the source or destination "
						+ "for any association.",
				fullyQualifiedClass);
	}

	@Override
	public void logOrphanedClass(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		logger.info("Orphan class '{}' found in the Sparx EA model. This class has been ignored during import.",
				fullyQualifiedClass);
	}

	@Override
	public void logOrphanEnumeration(String packageHierarchy, String enumName) {
		String fullyQualifiedEnum = packageHierarchy + "::" + enumName;
		logger.info(
				"Orphan enumeration '{}' found in the Sparx EA model. This enumeration has been ignored during import.",
				fullyQualifiedEnum);
	}

	@Override
	public void logInvalidEnumerationDefinition(String packageHierarchy, String enumName) {
		String fullyQualifiedEnum = packageHierarchy + "::" + enumName;
		logger.error(
				"The enumeration '{}' has been incorrectly defined in the model as a strict UML Enumeration. "
						+ "Per Rule084 of the CIM Modeling Guidelines a CIM enumeration must be defined as a UML Class "
						+ "with the <<enumeration>> stereotype. Correct this definition from within Sparx EA and reimport.",
				fullyQualifiedEnum);
	}

}
