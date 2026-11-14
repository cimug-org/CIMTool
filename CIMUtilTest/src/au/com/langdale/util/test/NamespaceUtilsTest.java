package au.com.langdale.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import au.com.langdale.util.NamespaceUtils;

import com.hp.hpl.jena.graph.Node;

/**
 * Tests for {@link NamespaceUtils}, which handles both hash and slash namespaces
 * (issue #377).
 * <p>
 * The tests live in their own package so that this fragment of CIMUtil does not
 * share the {@code au.com.langdale.util} package exported by Kena.
 */
public class NamespaceUtilsTest {

	private static final String CIM = "http://iec.ch/TC57/CIM100#";
	private static final String DCTERMS = "http://purl.org/dc/terms/";
	private static final String PROFILE = "http://ucaiug.org/ns/CGMES/CoreEquipment-EU/3.0#";

	// ---------------------------------------------------------------------
	// Validation
	// ---------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = { CIM, DCTERMS, PROFILE, //
			"https://ucaiug.ai/ns/example#", //
			"https://ucaiug.ai/ns/example/", //
			"HTTP://example.org/ns#", //
			"http://www.w3.org/2004/02/skos/core#" })
	public void acceptsHttpAndHttpsHashAndSlashNamespaces(String namespace) {
		assertTrue(NamespaceUtils.isValidNamespace(namespace), namespace);
	}

	@ParameterizedTest
	@ValueSource(strings = { //
			"http://purl.org/dc/terms", // no terminator
			"http://example.org/ns:", // ends in ':'
			"http://example.org/ns_", // ends in '_'
			"urn:example:ns#", // scheme other than http and https
			"ftp://example.org/ns#", //
			"htp://example.org/ns#", // mistyped scheme
			"http:example.org/ns#", // no authority
			"http://#", // nothing before the terminator
			"http://example.org/my ns#", // whitespace
			"" })
	public void rejectsOtherNamespaces(String namespace) {
		assertFalse(NamespaceUtils.isValidNamespace(namespace), namespace);
	}

	@Test
	public void rejectsNullNamespace() {
		assertFalse(NamespaceUtils.isValidNamespace(null));
	}

	@Test
	public void classifiesHashAndSlashNamespaces() {
		assertTrue(NamespaceUtils.isHashNamespace(CIM));
		assertFalse(NamespaceUtils.isSlashNamespace(CIM));
		assertTrue(NamespaceUtils.isSlashNamespace(DCTERMS));
		assertFalse(NamespaceUtils.isHashNamespace(DCTERMS));
		assertFalse(NamespaceUtils.isHashNamespace(null));
		assertFalse(NamespaceUtils.isSlashNamespace(null));
	}

	// ---------------------------------------------------------------------
	// Splitting
	// ---------------------------------------------------------------------

	@Test
	public void splitsHashUris() {
		String uri = CIM + "ACLineSegment";
		assertEquals(CIM, NamespaceUtils.namespaceOf(uri));
		assertEquals("ACLineSegment", NamespaceUtils.localNameOf(uri));
	}

	@Test
	public void splitsHashUrisAtTheHashNotAnEarlierSlash() {
		String uri = CIM + "ACLineSegment.r";
		assertEquals(CIM, NamespaceUtils.namespaceOf(uri));
		assertEquals("ACLineSegment.r", NamespaceUtils.localNameOf(uri));
	}

	@Test
	public void splitsSlashUris() {
		String uri = DCTERMS + "title";
		assertEquals(DCTERMS, NamespaceUtils.namespaceOf(uri));
		assertEquals("title", NamespaceUtils.localNameOf(uri));
	}

	@Test
	public void splitsHashNamespacesEndingInSlashHash() {
		// A hash namespace such as http://example.org/ns/# splits at the '#'.
		String uri = "http://example.org/ns/#Term";
		assertEquals("http://example.org/ns/#", NamespaceUtils.namespaceOf(uri));
		assertEquals("Term", NamespaceUtils.localNameOf(uri));
	}

	@Test
	public void splittingIsTotal() {
		assertEquals("", NamespaceUtils.namespaceOf("noSeparator"));
		assertEquals("noSeparator", NamespaceUtils.localNameOf("noSeparator"));
		assertEquals(CIM, NamespaceUtils.namespaceOf(CIM));
		assertEquals("", NamespaceUtils.localNameOf(CIM));
		assertNull(NamespaceUtils.namespaceOf(null));
		assertNull(NamespaceUtils.localNameOf(null));
	}

	// ---------------------------------------------------------------------
	// Ontology IRI and namespace derivation
	// ---------------------------------------------------------------------

	@Test
	public void ontologyUriForHashNamespaceDropsTheHash() {
		assertEquals("http://iec.ch/TC57/CIM100", NamespaceUtils.ontologyUriFor(CIM));
		assertEquals("http://ucaiug.org/ns/CGMES/CoreEquipment-EU/3.0", NamespaceUtils.ontologyUriFor(PROFILE));
	}

	@Test
	public void ontologyUriForSlashNamespaceIsTheNamespace() {
		assertEquals(DCTERMS, NamespaceUtils.ontologyUriFor(DCTERMS));
	}

	@Test
	public void ontologyUriForRejectsOtherNamespaces() {
		assertThrows(IllegalArgumentException.class, () -> NamespaceUtils.ontologyUriFor("http://purl.org/dc/terms"));
		assertThrows(IllegalArgumentException.class, () -> NamespaceUtils.ontologyUriFor(null));
	}

	@Test
	public void namespaceForDerivesHashOrSlash() {
		assertEquals(CIM, NamespaceUtils.namespaceFor("http://iec.ch/TC57/CIM100"));
		assertEquals(DCTERMS, NamespaceUtils.namespaceFor(DCTERMS));
		assertNull(NamespaceUtils.namespaceFor(null));
	}

	@Test
	public void derivationRoundTrips() {
		for (String namespace : new String[] { CIM, DCTERMS, PROFILE })
			assertEquals(namespace, NamespaceUtils.namespaceFor(NamespaceUtils.ontologyUriFor(namespace)), namespace);
	}

	@Test
	public void slashTerminatedAndUnterminatedIrisRemainDistinct() {
		// http://purl.org/dc/terms and http://purl.org/dc/terms/ are different IRIs.
		assertNotEquals("http://purl.org/dc/terms", NamespaceUtils.ontologyUriFor(DCTERMS));
		assertEquals("http://purl.org/dc/terms#", NamespaceUtils.namespaceFor("http://purl.org/dc/terms"));
		assertFalse(NamespaceUtils.isConsistent("http://purl.org/dc/terms", DCTERMS));
	}

	@Test
	public void checksDeclaredNamespacesAgainstOntologyIris() {
		assertTrue(NamespaceUtils.isConsistent("http://iec.ch/TC57/CIM100", CIM));
		assertTrue(NamespaceUtils.isConsistent(DCTERMS, DCTERMS));
		assertTrue(NamespaceUtils.isConsistent("http://example.org/ns/", "http://example.org/ns/#"));
		assertFalse(NamespaceUtils.isConsistent("http://iec.ch/TC57/CIM100", "http://iec.ch/TC57/CIM17#"));
		assertFalse(NamespaceUtils.isConsistent("http://iec.ch/TC57/CIM100", "http://iec.ch/TC57/CIM100"));
		assertFalse(NamespaceUtils.isConsistent(null, CIM));
		assertFalse(NamespaceUtils.isConsistent("http://iec.ch/TC57/CIM100", null));
	}

	// ---------------------------------------------------------------------
	// Package namespaces
	// ---------------------------------------------------------------------

	@Test
	public void childNamespaceFollowsTheBaseConvention() {
		assertEquals("http://x/y/Wires#", NamespaceUtils.childNamespace("http://x/y#", "Wires"));
		assertEquals("http://x/y/Wires/", NamespaceUtils.childNamespace("http://x/y/", "Wires"));
		assertEquals("http://x/y/Base/Wires#", NamespaceUtils.childNamespace("http://x/y#", "Base/Wires"));
	}

	@Test
	public void childNamespacePreservesHistoricalOutputForHashBases() {
		// The historical stripHash() helper removed every trailing '#' and '/'.
		assertEquals("http://x/y/Wires#", NamespaceUtils.childNamespace("http://x/y/#", "Wires"));
	}

	@Test
	public void childNamespaceRejectsOtherBases() {
		assertThrows(IllegalArgumentException.class, () -> NamespaceUtils.childNamespace("http://x/y", "Wires"));
	}

	// ---------------------------------------------------------------------
	// Relationship to Jena's namespace splitting
	// ---------------------------------------------------------------------

	@Test
	public void agreesWithJenaForWellFormedNames() {
		for (String uri : new String[] { CIM + "ACLineSegment", CIM + "ACLineSegment.r", DCTERMS + "title" }) {
			Node node = Node.createURI(uri);
			assertEquals(node.getNameSpace(), NamespaceUtils.namespaceOf(uri), uri);
			assertEquals(node.getLocalName(), NamespaceUtils.localNameOf(uri), uri);
		}
	}

	@Test
	public void differsFromJenaForLocalNamesStartingWithADigit() {
		// Jena chooses the longest suffix that is a valid NCName, which cannot begin
		// with a digit, so it moves the split past the digits. NamespaceUtils splits at
		// the '#' (issue #377, decision 3).
		String uri = "http://example.org/ns#123abc";
		assertEquals("123abc", NamespaceUtils.localNameOf(uri));
		assertEquals("http://example.org/ns#", NamespaceUtils.namespaceOf(uri));
		assertNotEquals(NamespaceUtils.localNameOf(uri), Node.createURI(uri).getLocalName());
	}
}
