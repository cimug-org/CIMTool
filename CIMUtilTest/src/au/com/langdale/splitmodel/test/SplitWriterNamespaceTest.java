package au.com.langdale.splitmodel.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import au.com.langdale.splitmodel.SplitWriter;

/**
 * Tests for the namespace handling of {@link SplitWriter}, which writes instance
 * data as a split model, with support for hash and slash namespaces and the
 * {@code https:} scheme (issue #377).
 * <p>
 * The tests live in their own package so that this fragment of CIMUtil does not
 * share the {@code au.com.langdale.splitmodel} package exported by Kena.
 */
public class SplitWriterNamespaceTest {

	private static final String CIM = "http://iec.ch/TC57/CIM100#";
	private static final String DCTERMS = "http://purl.org/dc/terms/";
	private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	@TempDir
	Path dir;

	// ---------------------------------------------------------------------
	// Base namespace
	// ---------------------------------------------------------------------

	@Test
	public void keepsHashBase() {
		assertEquals("http://example.org/data#", writer("hash", "http://example.org/data#").getBase());
	}

	@Test
	public void keepsSlashBase() {
		assertEquals("http://example.org/data/", writer("slash", "http://example.org/data/").getBase());
	}

	@Test
	public void appendsHashToUnterminatedBase() {
		assertEquals("http://example.org/data#", writer("plain", "http://example.org/data").getBase());
	}

	@Test
	public void appendsHashToFileBaseAsBefore() throws IOException {
		// The default base is the destination's file: URI, which ends in '/' for an
		// existing directory. It is not a valid http: or https: namespace, so it
		// keeps its historical '#'.
		File destin = Files.createDirectories(dir.resolve("default")).toFile();
		String fileBase = destin.toURI().toString();
		assertTrue(fileBase.endsWith("/"), fileBase);
		assertEquals(fileBase + "#", new SplitWriter(destin.getPath()).getBase());
	}

	// ---------------------------------------------------------------------
	// Prefixes and abbreviation
	// ---------------------------------------------------------------------

	@Test
	public void abbreviatesHashSlashAndHttpsNamespaces() throws Exception {
		SplitWriter writer = writer("prefixes", "http://example.org/data#");
		writer.setPrefix("cim", CIM);
		writer.setPrefix("dct", DCTERMS);
		writer.setPrefix("ex", "https://example.org/ns#");
		writer.addObjectProperty("http://example.org/data#a", RDF_TYPE, CIM + "ACLineSegment");
		writer.addObjectProperty("http://example.org/data#a", DCTERMS + "conformsTo", "https://example.org/ns#Profile");
		writer.close();

		String output = read("prefixes");
		assertTrue(output.contains("@prefix cim: <" + CIM + "> ."), output);
		assertTrue(output.contains("@prefix dct: <" + DCTERMS + "> ."), output);
		assertTrue(output.contains("@prefix ex: <https://example.org/ns#> ."), output);
		assertTrue(output.contains("cim:ACLineSegment"), output);
		assertTrue(output.contains("dct:conformsTo"), output);
		assertTrue(output.contains("ex:Profile"), output);
		assertFalse(output.contains("<" + DCTERMS + "conformsTo>"), output);
	}

	@Test
	public void ignoresPrefixesForOtherNamespaces() throws Exception {
		SplitWriter writer = writer("ignored", "http://example.org/data#");
		writer.setPrefix("urn", "urn:example:ns#");
		writer.setPrefix("open", "http://example.org/open");
		writer.addObjectProperty("http://example.org/data#a", "urn:example:ns#p", "http://example.org/openValue");
		writer.close();

		String output = read("ignored");
		assertFalse(output.contains("@prefix urn:"), output);
		assertFalse(output.contains("@prefix open:"), output);
		assertTrue(output.contains("<urn:example:ns#p>"), output);
	}

	@Test
	public void abbreviatesSlashBase() throws Exception {
		SplitWriter writer = writer("slashbase", "http://example.org/data/");
		writer.addObjectProperty("http://example.org/data/a", RDF_TYPE, CIM + "ACLineSegment");
		writer.close();

		String output = read("slashbase");
		assertTrue(output.contains("@prefix : <http://example.org/data/> ."), output);
		assertTrue(output.contains(":a "), output);
	}

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	private SplitWriter writer(String name, String base) {
		return new SplitWriter(dir.resolve(name).toString(), base, 2, 2);
	}

	private String read(String name) throws IOException {
		try (Stream<Path> files = Files.walk(dir.resolve(name))) {
			return files.filter(Files::isRegularFile).sorted().map(file -> {
				try {
					return Files.readString(file, StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.joining("\n"));
		}
	}
}
