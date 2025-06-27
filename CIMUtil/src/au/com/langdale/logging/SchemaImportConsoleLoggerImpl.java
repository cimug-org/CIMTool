package au.com.langdale.logging;

import au.com.langdale.kena.OntResource;

/**
 * A specialized implementation of a SchemaImportLogger that supports schema
 * import reporting to the console i.e. Standard.out / Standard.err
 */
public class SchemaImportConsoleLoggerImpl implements SchemaImportLogger {

	public SchemaImportConsoleLoggerImpl() {
	}
	
	@Override
	public void log(String message) {
		if (message != null)
			System.out.println(message);
	}

	@Override
	public void log(LogLevel level, String message) {
		if (message != null)
			System.out.println("[" + level.name() + "] " + message);
	}

	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName) {
		String fullyQualifiedAttribute = packageHierarchy + "::" + className + "." + attributeName;
		System.err.println("[" + LogLevel.ERROR + "] The Range on attribute " + fullyQualifiedAttribute
				+ " has an orphaned or invalid declared type in the model. From within Sparx EA you must reselect the desired declared type for the attribute.");
	}

	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName,
			String attributeDeclaredType, int rangeObjectId) {
		String fullyQualifiedAttribute = packageHierarchy + "::" + className + "." + attributeName;
		String declaredType = (attributeDeclaredType != null & !"".equals(attributeDeclaredType) ? attributeDeclaredType
				: "<Undeclared Type>");
		System.err.println("[" + LogLevel.ERROR + "] The Range on attribute '" + fullyQualifiedAttribute
				+ "' is declared to be of type '" + declaredType
				+ "' which is missing from the model. Range ID (t_attribute.Classifier) = " + rangeObjectId + ".");
	}

	@Override
	public void logInvalidPackageForClass(OntResource subject, int packageId) {
		System.err.println("[" + LogLevel.ERROR + "] Class "
				+ (subject.getLabel() != null ? subject.getLabel() : subject.getLocalName())
				+ " has invalid package ID: " + packageId);
	}

	@Override
	public void logInvalidParentPackage(OntResource subject, int parentPackageId) {
		System.err.println("[" + LogLevel.ERROR + "] Package "
				+ (subject.getLabel() != null ? subject.getLabel() : subject.getLocalName())
				+ " has invalid parent package ID: " + parentPackageId);
	}

	@Override
	public void logAssociationInvalidCardinality(boolean roleA, String assocType, String card, OntResource source,
			String sourceRole, OntResource destin, String destRole) {
		StringBuffer msg = new StringBuffer();
		msg.append("[" + LogLevel.ERROR + "] ");
		msg.append(roleA ? "Source " : "Destination ");
		msg.append("role end cardinality ['" + card + "'] is invalid for ");
		msg.append(assocType.toLowerCase()).append(": ");
		msg.append((source.getLabel() != null && !"".equals(source.getLabel()) ? source.getLabel()
				: source.getLocalName()));
		msg.append(" (Source Role: " + (!"".equals(sourceRole) ? sourceRole : "<Unspecified>") + ")");
		msg.append(" -> ");
		msg.append((destin.getLabel() != null && !"".equals(destin.getLabel()) ? destin.getLabel()
				: destin.getLocalName()));
		msg.append(" (Dest Role: " + (!"".equals(destRole) ? destRole : "<Unspecified>") + ")");
		msg.append(" defaulting cardinality to 0..1");
		System.err.println(msg.toString());
	}

	@Override
	public void logAttributeMissingDomain(String attributeName, int domainObjectID) {
		System.err.println("[" + LogLevel.ERROR + "] Could not find the class containing attribute '" + attributeName
				+ "'. Domain ID (t_attribute.Object_ID) = " + domainObjectID);
	}

	@Override
	public void logException(Exception exception) {
		System.err.println("[" + LogLevel.ERROR + "] Unexpected exception:");
		exception.printStackTrace(System.err);
	}

	@Override
	public void logClassUnusedInAttribute(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		System.err.println("[" + LogLevel.INFO + "] Class '" + fullyQualifiedClass
				+ "' is not used as the declared type of any attribute in the model.");
	}

	@Override
	public void logClassUnusedInAttributeOrAssociation(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		System.err.println("[" + LogLevel.INFO + "] Class '" + fullyQualifiedClass
				+ "' is not used as the declared type of any attribute nor as the source or destination for any association.");
	}

	@Override
	public void logOrphanedClass(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		System.err.println("[" + LogLevel.INFO + "] Orphan class '" + fullyQualifiedClass
				+ "' found in the Sparx EA model. This class has been ignored during import.");
	}

	@Override
	public void logOrphanEnumeration(String packageHierarchy, String enumName) {
		String fullyQualifiedEnum = packageHierarchy + "::" + enumName;
		System.err.println("[" + LogLevel.INFO + "] Orphan enumeration '" + fullyQualifiedEnum
				+ "' found in the Sparx EA model. This enumeration has been ignored during import.");
	}

	@Override
	public void logInvalidEnumerationDefinition(String packageHierarchy, String enumName) {
		String fullyQualifiedEnum = packageHierarchy + "::" + enumName;
		System.err.println("[" + LogLevel.ERROR + "] The enumeration '" + fullyQualifiedEnum
				+ "' has been incorrectly defined in the model as a strict UML Enumeration. Per Rule084 of the CIM Modeling Guidelines a CIM enumeration must be defined as a UML Class with the <<enumeration>> stereotype. Correct this definition from within Sparx EA and reimport.");
	}

}
