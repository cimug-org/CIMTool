package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;

/**
 * Tests the {@code unit} and {@code multiplier} elements that
 * {@code ProfileSerializer} writes for a CIMDatatype (issue #387). Their
 * {@code baseClass} is {@code UnitSymbol} or {@code UnitMultiplier}, which the
 * serializer names itself, so {@code unresolved} must say whether the schema
 * actually declares that class.
 */
class CIMDatatypeUnresolvedSerializationTest {

	private static final String PROFILE = "Profiles/CIMDatatypeProfile.owl";
	private static final String SCHEMA = "Schema/CIMDatatypeSchema.owl";
	private static final String CIM = TestProjects.CIM100;

	@TempDir
	Path project;

	@TempDir
	Path output;

	/**
	 * Runs the {@code xml} builder on the CIMDatatype profile, optionally adding
	 * {@code UnitSymbol} and {@code UnitMultiplier} classes to the schema.
	 */
	private Document serialize(boolean declareUnitClasses) throws Exception {
		TestProjects.copyResource("/unresolved-project/" + PROFILE, project.resolve(PROFILE));
		TestProjects.copyResource("/unresolved-project/" + SCHEMA, project.resolve(SCHEMA));
		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve(PROFILE).toFile());
		try (InputStream in = Files.newInputStream(project.resolve(SCHEMA))) {
			OntModel model = ModelFactory.createMem();
			IO.read(model, in, CIM, Format.RDF_XML.toFormat());
			if (declareUnitClasses) {
				model.createClass(CIM + "UnitSymbol");
				model.createClass(CIM + "UnitMultiplier");
			}
			transformer.addSchema(model);
		}
		transformer.setBuilder("xml");
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();
		File result = transformer.getOutputFile();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(result);
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

	private static void assertUnresolvedAccompaniesEveryBaseClass(Document xml) {
		NodeList all = xml.getElementsByTagNameNS("*", "*");
		for (int i = 0; i < all.getLength(); i++) {
			Element element = (Element) all.item(i);
			if (element.hasAttribute("baseClass"))
				assertTrue(element.hasAttribute("unresolved"),
						element.getLocalName() + " " + element.getAttribute("name") + " has baseClass without unresolved");
		}
	}

	@Test
	void unitClassesTheSchemaDoesNotDeclareAreUnresolved() throws Exception {
		Document xml = serialize(false);
		Element unit = lengthChild(xml, "unit");
		Element multiplier = lengthChild(xml, "multiplier");
		assertNotNull(unit, "the profile XML has a unit element for Length");
		assertNotNull(multiplier, "the profile XML has a multiplier element for Length");
		assertEquals(CIM + "UnitSymbol", unit.getAttribute("baseClass"));
		assertEquals("true", unit.getAttribute("unresolved"));
		assertEquals(CIM + "UnitMultiplier", multiplier.getAttribute("baseClass"));
		assertEquals("true", multiplier.getAttribute("unresolved"));
	}

	@Test
	void unitClassesTheSchemaDeclaresAreResolved() throws Exception {
		Document xml = serialize(true);
		Element unit = lengthChild(xml, "unit");
		Element multiplier = lengthChild(xml, "multiplier");
		assertNotNull(unit, "the profile XML has a unit element for Length");
		assertNotNull(multiplier, "the profile XML has a multiplier element for Length");
		assertEquals("false", unit.getAttribute("unresolved"));
		assertEquals("false", multiplier.getAttribute("unresolved"));
	}

	@Test
	void unresolvedAccompaniesEveryBaseClassWithoutUnitClasses() throws Exception {
		assertUnresolvedAccompaniesEveryBaseClass(serialize(false));
	}

	@Test
	void unresolvedAccompaniesEveryBaseClassWithUnitClasses() throws Exception {
		assertUnresolvedAccompaniesEveryBaseClass(serialize(true));
	}
}
