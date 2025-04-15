package au.com.langdale.xmi;

import au.com.langdale.kena.OntResource;

/**
 * A default "no-op" implementation of a SchemaImportLogger.
 */
public class SchemaImportLoggerNoOpImpl implements SchemaImportLogger {

	@Override
	public void log(String message) {
	}

	@Override
	public void log(LogLevel level, String message) {
	}
	
	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName) {
	}
	
	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName, String attributeDeclaredType, int rangeObjectId) {
	}

	@Override
	public void logInvalidPackageForClass(OntResource subject, int packageId) {
	}

	@Override
	public void logInvalidParentPackage(OntResource subject, int packageId) {
	}

	@Override
	public void logAssociationInvalidCardinality(boolean roleA, String assocType, String card, OntResource source,
			String sourceRole, OntResource destin, String destRole) {
	}

	@Override
	public void logAttributeMissingDomain(String attributeName, int objectID) {
	}

	@Override
	public void logException(Exception exception) {
	}

	@Override
	public void logClassUnusedInAttribute(String packageHierarchy, String className) {
	}

	@Override
	public void logClassUnusedInAttributeOrAssociation(String packageHierarchy, String className) {
	}

	@Override
	public void logOrphanedClass(String packageHierarchy, String className) {
	}

	@Override
	public void logOrphanEnumeration(String packageHierarchy, String enumName) {
	}

	@Override
	public void logInvalidEnumerationDefinition(String packageHierarchy, String enumName) {
	}

}
