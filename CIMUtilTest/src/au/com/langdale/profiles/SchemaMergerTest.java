package au.com.langdale.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.profiles.SchemaMerger.ProblemType;
import au.com.langdale.profiles.SchemaMerger.Result;
import au.com.langdale.profiles.SchemaMerger.SchemaFile;

/**
 * Tests for {@link SchemaMerger}, which chooses the project's base CIM schema
 * and merges the schema files so that the base's ontology is the merged
 * schema's ontology (issue #391).
 */
class SchemaMergerTest {

	private static final String CIM = "http://iec.ch/TC57/CIM100";
	private static final String EUROPEAN = "http://iec.ch/TC57/CIM100-European";
	private static final String EXTENSIONS = "http://example.com/extensions";

	/** A schema file whose model holds one header and one class. */
	private static SchemaFile file(String name, String ontology, boolean flagged) {
		OntModel model = ModelFactory.createMem();
		model.createIndividual(ontology, OWL2.Ontology);
		model.createClass(ontology + "#" + name.replace('.', '_'));
		return new SchemaFile(name, model, ontology + "#", flagged);
	}

	private static OntModel merged(Result result) {
		return ModelFactory.createMem(result.getGraph());
	}

	private static String ontology(Result result) {
		OntResource ontology = merged(result).getValidOntology();
		return ontology == null ? null : ontology.getURI();
	}

	private static ProblemType onlyProblem(Result result) {
		assertEquals(1, result.getProblems().size(), result.getProblems().toString());
		return result.getProblems().get(0).getType();
	}

	@Test
	void singleFileIsTheBase() {
		SchemaFile cim = file("cim.xmi", CIM, false);
		Result result = SchemaMerger.merge(Collections.singletonList(cim), null);
		assertSame(cim, result.getBase());
		assertEquals(CIM, ontology(result));
		assertTrue(result.getProblems().isEmpty());
	}

	@Test
	void flaggedFileIsTheBase() {
		SchemaFile eu = file("eu.xmi", EUROPEAN, false);
		SchemaFile cim = file("cim.xmi", CIM, true);
		Result result = SchemaMerger.merge(Arrays.asList(eu, cim), "http://example.com/other#");
		assertSame(cim, result.getBase(), "the flag outranks the Schema Namespace preference");
		assertEquals(CIM, ontology(result));
	}

	@Test
	void twoFlaggedFilesHaveNoBase() {
		Result result = SchemaMerger.merge(Arrays.asList(file("cim.xmi", CIM, true), file("eu.xmi", EUROPEAN, true)), null);
		assertNull(result.getBase());
		assertEquals(ProblemType.MULTIPLE_BASE_SCHEMAS, onlyProblem(result));
		assertTrue(result.hasErrors());
		assertNull(ontology(result));
	}

	@Test
	void withoutAFlagTheSchemaNamespacePreferenceChoosesTheBase() {
		SchemaFile eu = file("eu.xmi", EUROPEAN, false);
		SchemaFile cim = file("cim.xmi", CIM, false);
		Result result = SchemaMerger.merge(Arrays.asList(eu, cim), null);
		assertSame(cim, result.getBase(), "the preference defaults to " + SchemaMerger.DEFAULT_SCHEMA_NAMESPACE);
		assertEquals(CIM, ontology(result));
	}

	@Test
	void withoutAFlagOrAMatchThereIsNoBase() {
		Result result = SchemaMerger.merge(Arrays.asList(file("eu.xmi", EUROPEAN, false), file("ext.xmi", EXTENSIONS, false)), null);
		assertNull(result.getBase());
		assertEquals(ProblemType.NO_BASE_SCHEMA, onlyProblem(result));
		assertEquals(2, result.getProblems().get(0).getFiles().size());
		assertNull(ontology(result));
	}

	@Test
	void twoFilesWithTheSchemaNamespaceHaveNoBase() {
		SchemaFile first = file("cim-a.xmi", CIM, false);
		SchemaFile second = new SchemaFile("cim-b.owl", file("x", EXTENSIONS, false).getModel(), CIM + "#", false);
		Result result = SchemaMerger.merge(Arrays.asList(first, second, file("eu.xmi", EUROPEAN, false)), null);
		assertNull(result.getBase());
		assertEquals(ProblemType.AMBIGUOUS_BASE_SCHEMA, onlyProblem(result));
		assertEquals(Arrays.asList(first, second), result.getProblems().get(0).getFiles());
	}

	@Test
	void baseWithoutAHeaderGetsOneFromItsNamespace() {
		OntModel model = ModelFactory.createMem();
		model.createClass(CIM + "#ACLineSegment");
		SchemaFile base = new SchemaFile("cim.owl", model, CIM + "#", true);
		Result result = SchemaMerger.merge(Arrays.asList(base, file("eu.xmi", EUROPEAN, false)), null);
		assertSame(base, result.getBase());
		assertEquals(ProblemType.BASE_SCHEMA_NO_HEADER, onlyProblem(result));
		assertFalse(result.hasErrors(), "a header built from the namespace is a warning");
		assertEquals(CIM, ontology(result));
		assertEquals(1, model.size(), "nothing is written to the base file's model");
	}

	@Test
	void baseWithTwoHeadersHasNoBase() {
		SchemaFile base = file("cim.owl", CIM, true);
		base.getModel().createIndividual(EXTENSIONS, OWL2.Ontology);
		Result result = SchemaMerger.merge(Arrays.asList(base, file("eu.xmi", EUROPEAN, false)), null);
		assertNull(result.getBase());
		assertEquals(ProblemType.BASE_SCHEMA_HEADERS, onlyProblem(result));
		assertNull(ontology(result));
	}

	@Test
	void baseHeaderWithHashHasNoBase() {
		SchemaFile base = file("cim.owl", CIM + "#", true);
		Result result = SchemaMerger.merge(Arrays.asList(base, file("eu.xmi", EUROPEAN, false)), null);
		assertNull(result.getBase());
		assertEquals(ProblemType.BASE_SCHEMA_INVALID_URI, onlyProblem(result));
	}

	@Test
	void baseImportsEveryOtherFilesHeader() {
		SchemaFile cim = file("cim.xmi", CIM, true);
		Result result = SchemaMerger.merge(Arrays.asList(cim, file("eu.xmi", EUROPEAN, false), file("ext.owl", EXTENSIONS, false)), null);
		OntModel model = merged(result);
		OntResource base = model.createResource(CIM);
		assertTrue(base.hasProperty(OWL2.imports, model.createResource(EUROPEAN)));
		assertTrue(base.hasProperty(OWL2.imports, model.createResource(EXTENSIONS)));
		assertEquals(CIM, ontology(result));
	}

	@Test
	void anExtensionImportingTheBaseDoesNotChangeTheBase() {
		SchemaFile cim = file("cim.xmi", CIM, true);
		SchemaFile ext = file("ext.owl", EXTENSIONS, false);
		ext.getModel().createResource(EXTENSIONS).addProperty(OWL2.imports, ext.getModel().createResource(CIM));
		Result result = SchemaMerger.merge(Arrays.asList(cim, ext), null);
		assertEquals(CIM, ontology(result), "the extension's own import is hidden from the merged schema");
		assertFalse(merged(result).createResource(EXTENSIONS).hasProperty(OWL2.imports));
		assertTrue(ext.getModel().createResource(EXTENSIONS).hasProperty(OWL2.imports),
				"the file's own model keeps its import");
	}

	@Test
	void mergedSchemaHoldsEveryFilesContent() {
		List<SchemaFile> files = Arrays.asList(file("cim.xmi", CIM, true), file("eu.xmi", EUROPEAN, false));
		OntModel model = merged(SchemaMerger.merge(files, null));
		assertTrue(model.contains(model.createResource(CIM + "#cim_xmi"), com.hp.hpl.jena.vocabulary.RDF.type,
				com.hp.hpl.jena.vocabulary.OWL.Class));
		assertTrue(model.contains(model.createResource(EUROPEAN + "#eu_xmi"), com.hp.hpl.jena.vocabulary.RDF.type,
				com.hp.hpl.jena.vocabulary.OWL.Class));
	}

	@Test
	void nothingIsWrittenToTheFilesModels() {
		SchemaFile cim = file("cim.xmi", CIM, true);
		SchemaFile eu = file("eu.xmi", EUROPEAN, false);
		long cimSize = cim.getModel().size();
		long euSize = eu.getModel().size();
		OntModel model = merged(SchemaMerger.merge(Arrays.asList(cim, eu), null));
		model.add(model.createResource(CIM), RDFS.comment, "written to the merged schema");
		assertEquals(cimSize, cim.getModel().size());
		assertEquals(euSize, eu.getModel().size());
	}

	@Test
	void problemsNameTheFilesInvolved() {
		Result result = SchemaMerger.merge(Arrays.asList(file("cim.xmi", CIM, true), file("eu.xmi", EUROPEAN, true)), null);
		String description = result.getProblems().get(0).getDescription();
		assertNotNull(description);
		assertTrue(description.contains("cim.xmi") && description.contains("eu.xmi"), description);
	}
}
