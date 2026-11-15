package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
 * Tests that {@code ProfileSerializer} keeps a cyclic profile hierarchy out of
 * the profile XML (issue #387). In {@code CyclicProfile.owl}, A and B name each
 * other as superclasses, and C names A. The {@code SuperType} elements that
 * close the cycle are left out, and C keeps its {@code SuperType}.
 */
class CyclicProfileSerializationTest {

	private static final String PROFILE = "Profiles/CyclicProfile.owl";
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

	/** The names of the {@code SuperType} elements of the named profile class. */
	private List<String> superTypes(String name) {
		Element type = type(name);
		assertNotNull(type, "the profile XML has an element for " + name);
		List<String> names = new ArrayList<String>();
		NodeList children = type.getElementsByTagNameNS("*", "SuperType");
		for (int i = 0; i < children.getLength(); i++)
			names.add(((Element) children.item(i)).getAttribute("name"));
		return names;
	}

	@Test
	void superTypeClosingTheCycleIsOmittedForA() {
		assertEquals(List.of(), superTypes("A"));
	}

	@Test
	void superTypeClosingTheCycleIsOmittedForB() {
		assertEquals(List.of(), superTypes("B"));
	}

	/**
	 * C names only A, but through the cycle A and B are each other's
	 * superclasses, so B may also be reported as a direct superclass of C. Neither
	 * leads back to C, so neither is left out.
	 */
	@Test
	void classLeadingIntoTheCycleKeepsItsSuperType() {
		assertTrue(superTypes("C").contains("A"), "C keeps its SuperType A");
	}
}
