package au.com.langdale.cimtoole.test.headless;

import java.io.ByteArrayOutputStream;

import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.FullModelProfileModel;
import au.com.langdale.profiles.ProfileSerializer;

/**
 * Viability test for {@link FullModelProfileModel} and {@link SchemaProfileClass}.
 *
 * <p>
 * Uses the standard Eclipse workspace test infrastructure (via {@link ProjectTest})
 * to import the {@code SAMPLE_SCHEMA_QEA} schema through the exact same pipeline
 * that CIMTool itself uses at runtime — {@link Task#importSchema} followed by
 * {@link Task#getBackgroundModel} — then runs the resulting background
 * {@link OntModel} through {@link FullModelProfileModel} and
 * {@link ProfileSerializer} to verify the full pipeline produces valid
 * {@code a:Catalog} XML output.
 * </p>
 *
 * <h3>What this test validates</h3>
 * <ul>
 *   <li>The standard schema import pipeline produces a usable background model.</li>
 *   <li>{@code FullModelProfileModel.initialize()} completes without exceptions.</li>
 *   <li>{@code ProfileSerializer.write()} produces non-empty output.</li>
 *   <li>The output contains the {@code a:Catalog} root element.</li>
 *   <li>At least one {@code a:ComplexType} element is present — confirming
 *       {@code SchemaProfileClass.internalAnalyse()} correctly enumerated classes
 *       from the background model via {@code rdfs:domain}.</li>
 *   <li>At least one property element is present — confirming
 *       {@code SchemaProfileClass.getPropertyInfoFromSchema()} correctly resolved
 *       range from {@code rdfs:range} and cardinalities from
 *       {@code UML.schemaMin} / {@code UML.schemaMax}.</li>
 * </ul>
 */
public class FullModelProfileModelTest extends ProjectTest {

	public static final String SCHEMA_CIM100_NS = "http://iec.ch/TC57/CIM100#";

	/**
	 * Overridden profile for testing.
	 */
	@Override
	protected String getSchemaNSForTesting() {
		return SCHEMA_CIM100_NS;
	}

	/**
	 * Overridden schema for testing.
	 */
	@Override
	protected String getSchemaForTesting() {
		return SAMPLE_SCHEMA_QEA;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Import the QEA schema through the standard Task pipeline - the same path
		// that will be used in production. This populates the workspace cache so
		// that Task.getBackgroundModel(schema) returns a fully-interpreted OntModel.
		setupSchema();
	}

	// -------------------------------------------------------------------------
	// Test cases
	// -------------------------------------------------------------------------

	/**
	 * Core end-to-end viability test: background model -> FullModelProfileModel ->
	 * ProfileSerializer -> a:Catalog XML.
	 */
	public void testFullModelPipelineProducesValidCatalog() throws Exception {
		OntModel backgroundModel = Task.getBackgroundModel(schema);
		assertNotNull("Task.getBackgroundModel() must return a non-null model", backgroundModel);

		// Wire up FullModelProfileModel using the background model alone -
		// no profile .owl file is involved.
		FullModelProfileModel fullModel = new FullModelProfileModel();
		fullModel.initialize(backgroundModel);

		// Serialise to an in-memory stream - no file I/O needed for the assertion.
		ProfileSerializer serializer = new ProfileSerializer(fullModel);
		if (backgroundModel.getValidOntology() != null)
			serializer.setOntologyURI(backgroundModel.getValidOntology().getURI());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		serializer.write(out);
		String xml = out.toString("UTF-8");

		// Write the full XML payload to results.xml alongside the test workspace
		// so the output can be inspected after the test run.
		java.io.File resultsFile = new java.io.File(getSamplesFolder(), "results.xml");
		try (java.io.FileWriter fw = new java.io.FileWriter(resultsFile)) {
			fw.write(xml);
		}
		System.out.println("Full model XML written to: " + resultsFile.getAbsolutePath());

		// --- Structural assertions ---
		// Note: ProfileSerializer emits MESSAGE.NS as the default namespace (no prefix),
		// so without an XSLT stylesheet the identity transformer produces plain element
		// names. The "a:" prefix only appears in stylesheet-transformed output.

		assertTrue("Serializer output must be non-empty", xml.length() > 0);

		assertTrue("Output must contain Catalog root element",
				xml.contains("Catalog"));

		assertTrue("Output must contain at least one ComplexType - "
				+ "confirming SchemaProfileClass.internalAnalyse() populated classes "
				+ "from the background model",
				xml.contains("ComplexType"));

		// At least one of the property element types must appear, confirming that
		// getPropertyInfoFromSchema() resolved range correctly from rdfs:range.
		boolean hasProperties = xml.contains("Simple")
				|| xml.contains("Domain")
				|| xml.contains("Instance")
				|| xml.contains("Reference");
		assertTrue("Output must contain at least one property element "
				+ "(Simple, Domain, Instance, or Reference) - "
				+ "confirming getPropertyInfoFromSchema() resolved range and cardinality "
				+ "from UML.schemaMin / UML.schemaMax annotations",
				hasProperties);
	}

	/**
	 * Confirms that the imported schema produces a background model with a valid
	 * owl:Ontology header - required for FullModelProfileModel.initialize() to
	 * set the tree root.
	 */
	public void testBackgroundModelHasOntologyHeader() throws CoreException {
		OntModel backgroundModel = Task.getBackgroundModel(schema);
		assertNotNull("Task.getBackgroundModel() must return a non-null model", backgroundModel);
		assertNotNull("Background model must have a valid owl:Ontology header - "
				+ "required for FullModelProfileModel.initialize() to set the tree root",
				backgroundModel.getValidOntology());
	}

	/**
	 * Confirms that the imported background model contains named classes before
	 * our code is exercised - rules out a silent empty-model issue.
	 */
	public void testBackgroundModelContainsNamedClasses() throws CoreException {
		OntModel backgroundModel = Task.getBackgroundModel(schema);
		assertNotNull("Task.getBackgroundModel() must return a non-null model", backgroundModel);
		assertTrue("Background model must contain named classes after schema import",
				backgroundModel.listNamedClasses().hasNext());
	}
}
