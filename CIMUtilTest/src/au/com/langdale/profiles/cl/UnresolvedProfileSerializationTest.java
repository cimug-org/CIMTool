package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
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
 * Tests the profile XML that {@code ProfileSerializer} writes for profile
 * classes whose schema classes the schema does not declare (issue #387). Such a
 * class's {@code baseClass} is the schema class the profile names, not the
 * {@code owl:Thing} sentinel, and the {@code unresolved} attribute accompanies
 * every {@code baseClass}.
 */
class UnresolvedProfileSerializationTest {

	private static final String PROFILE = "Profiles/UnresolvedProfile.owl";
	private static final String SCHEMA = "Schema/MinimalSchema.owl";
	private static final String CIM = TestProjects.CIM100;

	private Document xml;

	@BeforeEach
	void serialize(@TempDir Path project, @TempDir Path output) throws Exception {
		TestProjects.copyResource("/unresolved-project/" + PROFILE, project.resolve(PROFILE));
		TestProjects.copyResource("/minimal-project/" + SCHEMA, project.resolve(SCHEMA));
		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve(PROFILE).toFile());
		try (InputStream in = Files.newInputStream(project.resolve(SCHEMA))) {
			OntModel model = ModelFactory.createMem();
			IO.read(model, in, CIM, Format.RDF_XML.toFormat());
			transformer.addSchema(model);
		}
		transformer.setBuilder("xml");
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();
		File result = transformer.getOutputFile();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		xml = factory.newDocumentBuilder().parse(result);
	}

	/** The top-level element for the named profile class. */
	private Element type(String name) {
		NodeList children = xml.getDocumentElement().getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) instanceof Element && name.equals(((Element) children.item(i)).getAttribute("name")))
				return (Element) children.item(i);
		return null;
	}

	private static Element child(Element parent, String localName) {
		NodeList children = parent.getElementsByTagNameNS("*", localName);
		return children.getLength() > 0 ? (Element) children.item(0) : null;
	}

	@Test
	void resolvedClassIsMarkedResolved() {
		Element identifiedObject = type("IdentifiedObject");
		assertNotNull(identifiedObject);
		assertEquals(CIM + "IdentifiedObject", identifiedObject.getAttribute("baseClass"));
		assertEquals("false", identifiedObject.getAttribute("unresolved"));
	}

	@Test
	void unresolvedClassKeepsTheClassTheProfileNames() {
		Element segment = type("ACLineSegment");
		assertNotNull(segment);
		assertEquals(CIM + "ACLineSegment", segment.getAttribute("baseClass"));
		assertEquals("true", segment.getAttribute("unresolved"));
	}

	@Test
	void unresolvedSupertypeIsItsOwnClassNotTheSubclass() {
		Element superType = child(type("ACLineSegment"), "SuperType");
		assertNotNull(superType);
		assertEquals(CIM + "Conductor", superType.getAttribute("baseClass"));
		assertEquals("true", superType.getAttribute("unresolved"));
	}

	@Test
	void resolvedSupertypeOfAnUnresolvedClassIsMarkedResolved() {
		Element superType = child(type("Conductor"), "SuperType");
		assertNotNull(superType);
		assertEquals(CIM + "IdentifiedObject", superType.getAttribute("baseClass"));
		assertEquals("false", superType.getAttribute("unresolved"));
	}

	@Test
	void noElementCarriesTheSentinel() {
		NodeList all = xml.getElementsByTagNameNS("*", "*");
		for (int i = 0; i < all.getLength(); i++) {
			Element element = (Element) all.item(i);
			assertFalse(element.getAttribute("baseClass").endsWith("owl#Thing"),
					element.getLocalName() + " " + element.getAttribute("name") + " carries owl:Thing");
		}
	}

	@Test
	void unresolvedAccompaniesEveryBaseClass() {
		NodeList all = xml.getElementsByTagNameNS("*", "*");
		int checked = 0;
		for (int i = 0; i < all.getLength(); i++) {
			Element element = (Element) all.item(i);
			if (element.hasAttribute("baseClass")) {
				checked++;
				assertTrue(element.hasAttribute("unresolved"),
						element.getLocalName() + " " + element.getAttribute("name") + " has baseClass without unresolved");
			}
		}
		assertTrue(checked >= 5, "the profile XML has class and supertype elements");
	}
}
