package au.com.langdale.logging;

import au.com.langdale.kena.OntResource;

/**
 * A specialized implementation of a SchemaImportLogger that supports generation
 * of a schema import reports in AsciiDoc fromat. The intention is that such
 * output could then be naturally viewed within CIMTool's Asciidoc plugin and a
 * PDF or HTML5 browser-based report generated from that.
 */
public class SchemaImportAsciiDocLoggerImpl implements SchemaImportLogger {

	enum Admonition {
		NOTE,
		TIP,
		IMPORTANT,
		CAUTION, // Use CAUTION to advise the reader to act carefully (i.e., exercise care). "Attention"
		WARNING // Use WARNING to inform the reader of danger, harm, or consequences that exist.
	}
	
	enum LogLevelAdmonition {
		
	}

	private StringBuffer schemaImportReport;

	public SchemaImportAsciiDocLoggerImpl() {
		this.schemaImportReport = new StringBuffer();
		this.schemaImportReport.append("//==================================================================").append(System.lineSeparator());
		this.schemaImportReport.append("// The following are 'header' attributes defined for this report").append(System.lineSeparator());
		this.schemaImportReport.append("//==================================================================").append(System.lineSeparator());
		this.schemaImportReport.append(":stem: latexmath").append(System.lineSeparator());
		this.schemaImportReport.append("//").append(System.lineSeparator());
		this.schemaImportReport.append("//============ General document settings ============").append(System.lineSeparator());
		this.schemaImportReport.append("//").append(System.lineSeparator());
		this.schemaImportReport.append(":doctype: article").append(System.lineSeparator());
		this.schemaImportReport.append(":reproducible:").append(System.lineSeparator());
		this.schemaImportReport.append(":icons: font").append(System.lineSeparator());
		this.schemaImportReport.append(":sectnums:").append(System.lineSeparator());
		this.schemaImportReport.append(":sectnumlevels: 4").append(System.lineSeparator());
		this.schemaImportReport.append(":xrefstyle: short").append(System.lineSeparator());
		this.schemaImportReport.append(":table-stripes: even").append(System.lineSeparator());
		this.schemaImportReport.append("//==================================================================").append(System.lineSeparator());
		this.schemaImportReport.append("//").append(System.lineSeparator());
		this.schemaImportReport.append("").append(System.lineSeparator());
		this.schemaImportReport.append("= Schema Import Report").append(System.lineSeparator());
		this.schemaImportReport.append("").append(System.lineSeparator());
		this.schemaImportReport.append("").append(System.lineSeparator());
	}
	
	public void startShadowExtensionTable() {
		this.schemaImportReport.append("[%header,width=100%,cols=\"5%a,10%a,85%a\"]").append(System.lineSeparator());
		this.schemaImportReport.append("|===").append(System.lineSeparator());
		this.schemaImportReport.append("| Level 2+| Violation Description").append(System.lineSeparator());
	}
	
	public void endShadowExtensionTable() {
		this.schemaImportReport.append("|===").append(System.lineSeparator());
		this.schemaImportReport.append("").append(System.lineSeparator());
	}

	@Override
	public void log(String message) {
		if (message != null)
			this.schemaImportReport.append(message);
	}

	@Override
	public void log(LogLevel level, String message) {
		if (message != null)
			this.schemaImportReport.append("[" + level.name() + "] " + message);
	}

	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName) {
		String fullyQualifiedAttribute = packageHierarchy + "::" + className + "." + attributeName;
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] The Range on attribute " + fullyQualifiedAttribute
				+ " has an orphaned or invalid declared type in the model. From within Sparx EA you must reselect the desired declared type for the attribute.");
	}

	@Override
	public void logAttributeMissingRange(String packageHierarchy, String className, String attributeName,
			String attributeDeclaredType, int rangeObjectId) {
		String fullyQualifiedAttribute = packageHierarchy + "::" + className + "." + attributeName;
		String declaredType = (attributeDeclaredType != null & !"".equals(attributeDeclaredType) ? attributeDeclaredType
				: "<Undeclared Type>");
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] The Range on attribute '" + fullyQualifiedAttribute
				+ "' is declared to be of type '" + declaredType
				+ "' which is missing from the model. Range ID (t_attribute.Classifier) = " + rangeObjectId + ".");
	}

	@Override
	public void logInvalidPackageForClass(OntResource subject, int packageId) {
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] Class "
				+ (subject.getLabel() != null ? subject.getLabel() : subject.getLocalName())
				+ " has invalid package ID: " + packageId);
	}

	@Override
	public void logInvalidParentPackage(OntResource subject, int parentPackageId) {
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] Package "
				+ (subject.getLabel() != null ? subject.getLabel() : subject.getLocalName())
				+ " has invalid parent package ID: " + parentPackageId);
	}

	@Override
	public void logAssociationInvalidCardinality(boolean roleA, String assocType, String card, OntResource source,
			String sourceRole, OntResource destin, String destRole) {
		StringBuffer msg = new StringBuffer();
		//
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
		//
		this.schemaImportReport.append(msg.toString());
	}

	@Override
	public void logAttributeMissingDomain(String attributeName, int domainObjectID) {
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] Could not find the class containing attribute '"
				+ attributeName + "'. Domain ID (t_attribute.Object_ID) = " + domainObjectID);
	}

	@Override
	public void logException(Exception exception) {
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] Unexpected exception:");
		exception.printStackTrace(System.err);
	}

	@Override
	public void logClassUnusedInAttribute(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		this.schemaImportReport.append("[" + LogLevel.INFO + "] Class '" + fullyQualifiedClass
				+ "' is not used as the declared type of any attribute in the model.");
	}

	@Override
	public void logClassUnusedInAttributeOrAssociation(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		this.schemaImportReport.append("[" + LogLevel.INFO + "] Class '" + fullyQualifiedClass
				+ "' is not used as the declared type of any attribute nor as the source or destination for any association.");
	}

	@Override
	public void logOrphanedClass(String packageHierarchy, String className) {
		String fullyQualifiedClass = packageHierarchy + "::" + className;
		this.schemaImportReport.append("[" + LogLevel.INFO + "] Orphan class '" + fullyQualifiedClass
				+ "' found in the Sparx EA model. This class has been ignored during import.");
	}

	@Override
	public void logOrphanEnumeration(String packageHierarchy, String enumName) {
		String fullyQualifiedEnum = packageHierarchy + "::" + enumName;
		this.schemaImportReport.append("[" + LogLevel.INFO + "] Orphan enumeration '" + fullyQualifiedEnum
				+ "' found in the Sparx EA model. This enumeration has been ignored during import.");
	}

	@Override
	public void logInvalidEnumerationDefinition(String packageHierarchy, String enumName) {
		String fullyQualifiedEnum = packageHierarchy + "::" + enumName;
		this.schemaImportReport.append("[" + LogLevel.ERROR + "] The enumeration '" + fullyQualifiedEnum
				+ "' has been incorrectly defined in the model as a strict UML Enumeration. Per Rule084 of the CIM Modeling Guidelines a CIM enumeration must be defined as a UML Class with the <<enumeration>> stereotype. Correct this definition from within Sparx EA and reimport.");
	}

}
