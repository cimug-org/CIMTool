package au.com.langdale.logging;

import au.com.langdale.kena.OntResource;

public interface SchemaImportLogger {
	
	enum LogLevel {
		INFO,
		WARN,
		ERROR
	}
	
	void log(String message);
	
	void log(LogLevel level, String message);
	
	void logAttributeMissingRange(String packageHierarchy, String className, String attributeName);
	
	void logAttributeMissingRange(String packageHierarchy, String className, String attributeName, String attributeDeclaredType, int rangeObjectId);

	void logInvalidPackageForClass(OntResource subject, int packageId);
	
	void logInvalidParentPackage(OntResource subject, int packageID);

	void logAssociationInvalidCardinality(boolean roleA, String assocType, String card, OntResource source,
			String sourceRole, OntResource destin, String destRole);

	void logAttributeMissingDomain(String attributeName, int objectID);

	void logException(Exception exception);

	void logClassUnusedInAttribute(String packageHierarchy, String className);

	void logClassUnusedInAttributeOrAssociation(String packageHierarchy, String className);

	void logOrphanedClass(String packageHierarchy, String className);

	void logOrphanEnumeration(String packageHierarchy, String enumName);

	void logInvalidEnumerationDefinition(String packageHierarchy, String enumName);
	
}
