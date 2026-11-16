package au.com.langdale.kena.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Tests for the Kena RDF/OWL model API: statements, reading and writing
 * RDF/XML, named classes, and the transitive inference model.
 */
public class KenaTests {

	public static final String NS = "http://example.com/example#";

	private static Resource eg(String name) {
		return ResourceFactory.createProperty(NS + name);
	}

	private static OntModel readSampleProfile() throws IOException {
		OntModel model = ModelFactory.createMem();
		try (InputStream in = TestResources.open(TestResources.SAMPLE_PROFILE)) {
			IO.read(model, in, NS, Format.RDF_XML.toFormat());
		}
		return model;
	}

	@Test
	public void testCreateStatement() {
		OntModel model = ModelFactory.createMem();

		model.add(eg("s"), eg("p"), "v");
		assertTrue(model.contains(eg("s"), eg("p"), "v"), "statement remembered");
	}

	@Test
	public void testReadRDFXML() throws IOException {
		OntModel model = readSampleProfile();
		assertTrue(model.size() > 0, "read some statements");
	}

	@Test
	public void testWriteReadXML(@TempDir Path folder) throws IOException {
		File file = folder.resolve("written.owl").toFile();
		OntModel model = ModelFactory.createMem();

		model.add(eg("s"), eg("p"), "v");
		try (OutputStream contents = new FileOutputStream(file)) {
			IO.write(model, contents, NS, Format.RDF_XML.toFormat(), Collections.EMPTY_MAP);
		}
		assertTrue(file.exists(), "file was written");

		model = ModelFactory.createMem();
		try (InputStream in = new FileInputStream(file)) {
			IO.read(model, in, NS, Format.RDF_XML.toFormat());
		}
		assertTrue(model.size() > 0, "read some statements");
		assertTrue(model.contains(eg("s"), eg("p"), "v"), "statement remembered");
	}

	@Test
	public void testOntResource() throws IOException {
		OntModel model = readSampleProfile();
		assertTrue(model.size() > 0, "read some statements");

		int count = 0;
		ResIterator all = model.listNamedClasses();
		while (all.hasNext()) {
			OntResource clss = all.nextResource();
			assertNotNull(clss, "found a class");
			assertTrue(clss.isURIResource(), "class claims to have URI");
			assertTrue(clss.hasRDFType(OWL.Class), "class has type");
			count++;
		}
		assertTrue(count >= 12, "found a dozen classes");
	}

	/** A model where c is a subclass of b1 and b2, b1 of b2, and b2 of b3. */
	private static OntModel inheritance() {
		OntModel model = ModelFactory.createTransInf();
		model.add(eg("c"), RDFS.subClassOf, eg("b1"));
		model.add(eg("c"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b1"), RDFS.subClassOf, eg("b2"));
		model.add(eg("b2"), RDFS.subClassOf, eg("b3"));
		return model;
	}

	@Test
	public void testInference1() {
		OntModel model = inheritance();

		OntResource c = model.createResource(eg("c").asNode());
		assertTrue(c.hasSuperClass(eg("b3")), "closure of inheritance");
		assertFalse(c.hasSubClass(eg("b3")), "spurious inference");
	}

	@Test
	public void testInference2() {
		OntModel model = inheritance();

		OntResource c = model.createResource(eg("c").asNode());
		assertFalse(c.hasSuperClass(eg("b3"), true), "closed inheritance");
		assertFalse(c.hasSuperClass(eg("b2"), true), "indirect but asserted inheritance");
		assertTrue(c.hasSuperClass(eg("b1"), true), "direct inheritance");
		assertFalse(c.hasSuperClass(c, true), "filter self from direct inheritance");
		assertTrue(c.hasSuperClass(c), "include self in closed inheritance");
	}

	@Test
	public void testInference3() {
		OntModel model = inheritance();

		boolean b1detect = false;

		OntResource c = model.createResource(eg("c").asNode());
		int count = 0;
		ResIterator it = c.listSuperClasses(true);
		while (it.hasNext()) {
			count++;
			OntResource base = it.nextResource();
			if (base.equals(eg("b1")))
				b1detect = true;
		}
		assertTrue(b1detect, "direct inheritance");
		assertEquals(1, count, "unique result");
	}
}
