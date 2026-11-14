package au.com.langdale.xmi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import au.com.langdale.xmi.Translator;

/**
 * Tests for {@link Translator#packageNamespace(String, String)} and
 * {@link Translator#schemaOntologyUri(String)}, which build package namespaces
 * and the schema ontology IRI during schema import (issue #377).
 */
public class PackageNamespaceTest {

	private static final String[] PATHS = { "", "/Wires", "/Global/TC57CIM/IEC61970/Base/Wires" };

	/** The expression every call site used before issue #377. */
	private static String historical(String base, String path) {
		return Translator.stripHash(base) + path + "#";
	}

	// ---------------------------------------------------------------------
	// Everything except a valid http(s) slash base is unchanged
	// ---------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = { "http://iec.ch/TC57/CIM100#", "http://example.org/x/y/#", "https://example.org/x/y#",
			"file:/C:/workspace/project/", "http://example.org/x/y", "urn:example:ns#" })
	public void keepsHistoricalNamespacesForOtherBases(String base) {
		for (String path : PATHS)
			assertEquals(historical(base, path), Translator.packageNamespace(base, path), base + " " + path);
	}

	@Test
	public void hashBaseExamples() {
		assertEquals("http://iec.ch/TC57/CIM100/Global/Base/Wires#",
				Translator.packageNamespace("http://iec.ch/TC57/CIM100#", "/Global/Base/Wires"));
		assertEquals("http://iec.ch/TC57/CIM100#", Translator.packageNamespace("http://iec.ch/TC57/CIM100#", ""));
		assertEquals("http://example.org/x/y/Wires#", Translator.packageNamespace("http://example.org/x/y/#", "/Wires"));
	}

	// ---------------------------------------------------------------------
	// Slash bases follow the slash convention
	// ---------------------------------------------------------------------

	@Test
	public void slashBaseGivesSlashNamespaces() {
		assertEquals("http://example.org/cim/Global/Base/Wires/",
				Translator.packageNamespace("http://example.org/cim/", "/Global/Base/Wires"));
		assertEquals("https://example.org/cim/Wires/", Translator.packageNamespace("https://example.org/cim/", "/Wires"));
		assertEquals("http://example.org/cim/Wires/", Translator.packageNamespace("http://example.org/cim/", "Wires"));
	}

	@Test
	public void slashBaseWithEmptyPathIsTheBase() {
		assertEquals("http://example.org/cim/", Translator.packageNamespace("http://example.org/cim/", ""));
	}

	@Test
	public void memberNamespaceOfASlashPackage() {
		// Members of package "Wires" whose URI is http://example.org/cim/Base/Wires
		// split as namespace http://example.org/cim/Base/ and local name Wires.
		assertEquals("http://example.org/cim/Base/Wires/",
				Translator.packageNamespace("http://example.org/cim/Base/", "/Wires"));
	}

	// ---------------------------------------------------------------------
	// Schema ontology IRI
	// ---------------------------------------------------------------------

	@Test
	public void schemaOntologyUri() {
		assertEquals("http://iec.ch/TC57/CIM100", Translator.schemaOntologyUri("http://iec.ch/TC57/CIM100#"));
		assertEquals("http://example.org/x/y", Translator.schemaOntologyUri("http://example.org/x/y/#"));
		assertEquals("http://example.org/cim/", Translator.schemaOntologyUri("http://example.org/cim/"));
		assertEquals("https://example.org/cim/", Translator.schemaOntologyUri("https://example.org/cim/"));
		assertEquals("file:/C:/workspace/project", Translator.schemaOntologyUri("file:/C:/workspace/project/"));
	}
}
