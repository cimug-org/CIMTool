package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.vocabulary.OWL2;

import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.SchemaMerger;

/**
 * Serializes a CIMDatatype profile against a schema of two files merged by
 * {@link SchemaMerger} (issue #391). The base CIM schema declares
 * {@code UnitSymbol} and {@code UnitMultiplier}, and an extension schema file
 * imports the base. The profile XML's {@code ontologyURI} is the base CIM
 * ontology, so the {@code unit} and {@code multiplier} elements of the
 * CIMDatatype name the base's {@code UnitSymbol} and {@code UnitMultiplier} and
 * are resolved.
 */
class MergedSchemaCIMDatatypeTest {

	private static final String PROFILE = "Profiles/CIMDatatypeProfile.owl";
	private static final String SCHEMA = "Schema/CIMDatatypeSchema.owl";
	private static final String CIM = TestProjects.CIM100;
	private static final String EXTENSIONS = "http://example.com/extensions";

	@TempDir
	Path project;

	@TempDir
	Path output;

	@Test
	void unitAndMultiplierAreResolvedAgainstTheBaseCIMSchema() throws Exception {
		TestProjects.copyResource("/unresolved-project/" + PROFILE, project.resolve(PROFILE));
		TestProjects.copyResource("/unresolved-project/" + SCHEMA, project.resolve(SCHEMA));

		OntModel base = ModelFactory.createMem();
		try (InputStream in = Files.newInputStream(project.resolve(SCHEMA))) {
			IO.read(base, in, CIM, Format.RDF_XML.toFormat());
		}
		base.createClass(CIM + "UnitSymbol");
		base.createClass(CIM + "UnitMultiplier");

		OntModel extension = ModelFactory.createMem();
		extension.createIndividual(EXTENSIONS, OWL2.Ontology)
				.addProperty(OWL2.imports, extension.createResource(CIM.substring(0, CIM.length() - 1)));
		extension.createClass(EXTENSIONS + "#ExtensionClass");

		SchemaMerger.Result merged = SchemaMerger.merge(Arrays.asList(
				new SchemaMerger.SchemaFile("CIMDatatypeSchema.owl", base, CIM, false),
				new SchemaMerger.SchemaFile("Extensions.owl", extension, EXTENSIONS + "#", false)), null);
		assertTrue(merged.getProblems().isEmpty(), merged.getProblems().toString());

		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve(PROFILE).toFile());
		transformer.addSchema(ModelFactory.createTransInf(merged.getGraph()));
		transformer.setBuilder("xml");
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document xml = factory.newDocumentBuilder().parse(transformer.getOutputFile());

		assertEquals(CIM.substring(0, CIM.length() - 1), xml.getDocumentElement().getAttribute("ontologyURI"));
		for (String[] expected : new String[][] { { "unit", "UnitSymbol" }, { "multiplier", "UnitMultiplier" } }) {
			Element child = lengthChild(xml, expected[0]);
			assertNotNull(child, expected[0] + " element of Length");
			assertEquals(CIM + expected[1], child.getAttribute("baseClass"), expected[0] + " baseClass");
			assertEquals("false", child.getAttribute("unresolved"), expected[0] + " is resolved");
		}
	}

	/** The element with the given name inside the SimpleType for Length. */
	private static Element lengthChild(Document xml, String name) {
		NodeList types = xml.getElementsByTagNameNS("*", "SimpleType");
		for (int i = 0; i < types.getLength(); i++) {
			Element type = (Element) types.item(i);
			if (!"Length".equals(type.getAttribute("name")))
				continue;
			NodeList children = type.getElementsByTagNameNS("*", "*");
			for (int j = 0; j < children.getLength(); j++) {
				Element child = (Element) children.item(j);
				if (name.equals(child.getAttribute("name")))
					return child;
			}
		}
		return null;
	}
}
