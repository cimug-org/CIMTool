package au.com.langdale.kena.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

import com.hp.hpl.jena.vocabulary.OWL2;

/**
 * Tests for {@link OntModel#getValidOntology()}, which returns the ontology
 * header that no other header in the model imports (issue #391). A merged
 * schema has one header per schema file, and the base CIM schema's header
 * imports the others.
 */
public class OntModelValidOntologyTest {

	private static final String BASE = "http://iec.ch/TC57/CIM100";
	private static final String EUROPEAN = "http://iec.ch/TC57/CIM100-European";
	private static final String EXTENSIONS = "http://example.com/extensions";
	private static final String ABSENT = "http://example.com/not-loaded";

	private OntModel model;

	@BeforeEach
	public void setUp() {
		model = ModelFactory.createMem();
	}

	private OntResource header(String uri) {
		return model.createIndividual(uri, OWL2.Ontology);
	}

	private static void imports(OntResource importer, OntResource imported) {
		importer.addProperty(OWL2.imports, imported);
	}

	@Test
	public void noHeaderGivesNull() {
		assertNull(model.getValidOntology());
	}

	@Test
	public void oneHeaderIsReturned() {
		OntResource base = header(BASE);
		assertEquals(base, model.getValidOntology());
	}

	@Test
	public void oneHeaderWithHashInItsUriGivesNull() {
		header(BASE + "#");
		assertNull(model.getValidOntology());
	}

	@Test
	public void oneBlankNodeHeaderGivesNull() {
		model.createIndividual(OWL2.Ontology);
		assertNull(model.getValidOntology());
	}

	@Test
	public void twoHeadersWithoutImportsGiveNull() {
		header(BASE);
		header(EUROPEAN);
		assertNull(model.getValidOntology(), "unconnected headers, as in a merged schema today");
	}

	@Test
	public void baseImportingOneOtherIsReturned() {
		OntResource base = header(BASE);
		imports(base, header(EUROPEAN));
		assertEquals(base, model.getValidOntology());
	}

	@Test
	public void baseImportingTwoOthersIsReturned() {
		OntResource base = header(BASE);
		imports(base, header(EUROPEAN));
		imports(base, header(EXTENSIONS));
		assertEquals(base, model.getValidOntology());
	}

	@Test
	public void headAOfAChainIsReturned() {
		OntResource a = header(BASE);
		OntResource b = header(EUROPEAN);
		imports(a, b);
		imports(b, header(EXTENSIONS));
		assertEquals(a, model.getValidOntology());
	}

	@Test
	public void importedHeaderWithHashDoesNotPreventTheBase() {
		OntResource base = header(BASE);
		imports(base, header(EXTENSIONS + "#"));
		assertEquals(base, model.getValidOntology(), "the '#' rule applies only to the header returned");
	}

	@Test
	public void baseWithHashGivesNullRatherThanAnotherHeader() {
		OntResource base = header(BASE + "#");
		imports(base, header(EUROPEAN));
		assertNull(model.getValidOntology());
	}

	@Test
	public void importOfAnAbsentOntologyDoesNotConnectHeaders() {
		OntResource a = header(BASE);
		imports(a, model.createResource(ABSENT));
		header(EUROPEAN);
		assertNull(model.getValidOntology(), "an import of an ontology not in the model is ignored");
	}

	@Test
	public void importOfAnAbsentOntologyDoesNotAffectTheBase() {
		OntResource base = header(BASE);
		imports(base, header(EUROPEAN));
		imports(base, model.createResource(ABSENT));
		assertEquals(base, model.getValidOntology());
	}

	@Test
	public void headersImportingEachOtherGiveNull() {
		OntResource a = header(BASE);
		OntResource b = header(EUROPEAN);
		imports(a, b);
		imports(b, a);
		assertNull(model.getValidOntology(), "every header is imported, so there is no single answer");
	}

	@Test
	public void cycleAlongsideAnUnimportedHeaderGivesThatHeader() {
		OntResource root = header(BASE);
		OntResource c1 = header(EUROPEAN);
		OntResource c2 = header(EXTENSIONS);
		imports(c1, c2);
		imports(c2, c1);
		assertEquals(root, model.getValidOntology());
	}

	@Test
	public void headerImportingItselfIsReturned() {
		OntResource base = header(BASE);
		imports(base, base);
		assertEquals(base, model.getValidOntology(), "a self-import does not count");
	}
}
