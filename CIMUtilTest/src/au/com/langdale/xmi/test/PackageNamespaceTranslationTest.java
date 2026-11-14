package au.com.langdale.xmi.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.xmi.ResourceNamespaceResolver;
import au.com.langdale.xmi.StereotypedNamespaces;
import au.com.langdale.xmi.Translator;
import au.com.langdale.xmi.UML;
import au.com.langdale.xmi.XMI;

/**
 * Tests the namespaces {@link Translator} assigns to packages and classes that
 * have no baseuri or stereotype namespace of their own (issue #385), the
 * ancestor walk for stereotype namespaces (issue #381), and that
 * {@link ResourceNamespaceResolver} agrees with {@link Translator}.
 */
public class PackageNamespaceTranslationTest {

	private static final String CIM = "http://iec.ch/TC57/CIM100#";
	private static final String EXT = "http://example.org/ext#";
	private static final String EUROPEAN = "http://iec.ch/TC57/CIM100-European#";

	@TempDir
	Path dir;

	private OntModel raw;
	private int ids;

	// ---------------------------------------------------------------------
	// A small raw model, shaped like the output of the EA parsers
	// ---------------------------------------------------------------------

	private OntResource element(String label, com.hp.hpl.jena.graph.FrontsNode type, OntResource parent) {
		OntResource r = raw.createIndividual(XMI.NS + "EAID_" + (++ids), type);
		r.addLabel(label, null);
		r.addProperty(RDFS.isDefinedBy, parent == null ? UML.global_package : parent);
		return r;
	}

	private OntResource pkg(String label, OntResource parent) {
		return element(label, UML.Package, parent);
	}

	private OntResource cls(String label, OntResource pkg) {
		return element(label, OWL.Class, pkg);
	}

	/** Builds the CIM hierarchy plus an extension package tagged with baseuri. */
	private OntResource[] baseuriModel() {
		raw = ModelFactory.createMem();
		UML.loadOntology(raw);
		OntResource tc57 = pkg("TC57CIM", null);
		OntResource iec = pkg("IEC61970", tc57);
		OntResource wires = pkg("Wires", iec);
		OntResource line = cls("ACLineSegment", wires);
		OntResource ext = pkg("ExtPkg", null);
		ext.addProperty(UML.baseuri, EXT);
		OntResource extSub = pkg("ExtSub", ext);
		OntResource extClass = cls("ExtClass", extSub);
		return new OntResource[] { tc57, wires, line, extSub, extClass };
	}

	private Set<String> translate(StereotypedNamespaces sn, boolean usePackageNames) {
		Translator translator = new Translator(raw, sn, CIM, usePackageNames);
		translator.run();
		Set<String> uris = new HashSet<>();
		Iterator<Triple> it = translator.getModel().getGraph().find(Node.ANY, Node.ANY, Node.ANY);
		while (it.hasNext()) {
			Node s = it.next().getSubject();
			if (s.isURI())
				uris.add(s.getURI());
		}
		return uris;
	}

	private static void assertHas(Set<String> uris, String uri) {
		assertTrue(uris.contains(uri), uri);
	}

	private static void assertNoInternalNamespaces(Set<String> uris) {
		for (String uri : uris) {
			assertFalse(uri.startsWith("http://langdale.com.au/2005/xuid/"), uri);
			assertFalse(uri.startsWith("http://langdale.com.au/2005/UML/"), uri);
		}
	}

	// ---------------------------------------------------------------------
	// Translator: package names on and off
	// ---------------------------------------------------------------------

	@Test
	public void packageNamesOnUseTheSchemaNamespace() {
		baseuriModel();
		Set<String> uris = translate(new StereotypedNamespaces(), true);
		assertHas(uris, "http://iec.ch/TC57/CIM100/Global/TC57CIM#Package_TC57CIM");
		assertHas(uris, "http://iec.ch/TC57/CIM100/Global/TC57CIM/IEC61970#Package_IEC61970");
		assertHas(uris, "http://iec.ch/TC57/CIM100/Global/TC57CIM/IEC61970/Wires#Package_Wires");
		assertHas(uris, "http://iec.ch/TC57/CIM100/Global/TC57CIM/IEC61970/Wires#ACLineSegment");
		assertNoInternalNamespaces(uris);
	}

	@Test
	public void packageNamesOnKeepBaseuriNamespaces() {
		baseuriModel();
		Set<String> uris = translate(new StereotypedNamespaces(), true);
		assertHas(uris, "http://example.org/ext/Global/ExtPkg#Package_ExtPkg");
		assertHas(uris, "http://example.org/ext/Global/ExtPkg/ExtSub#Package_ExtSub");
		assertHas(uris, EXT + "ExtClass");
	}

	@Test
	public void packageNamesOffAreUnchanged() {
		baseuriModel();
		Set<String> uris = translate(new StereotypedNamespaces(), false);
		assertHas(uris, CIM + "Package_TC57CIM");
		assertHas(uris, CIM + "Package_Wires");
		assertHas(uris, CIM + "ACLineSegment");
		assertHas(uris, EXT + "Package_ExtPkg");
		assertHas(uris, EXT + "Package_ExtSub");
		assertHas(uris, EXT + "ExtClass");
		assertNoInternalNamespaces(uris);
	}

	// ---------------------------------------------------------------------
	// Stereotype namespace on a grandparent package only (issue #381)
	// ---------------------------------------------------------------------

	private StereotypedNamespaces stereotypeModel() throws IOException {
		raw = ModelFactory.createMem();
		UML.loadOntology(raw);
		OntResource european = raw.createIndividual(XMI.NS + "EAID_European", UML.Stereotype);
		european.addLabel("European", null);
		OntResource top = pkg("EuropeanExt", null);
		top.addProperty(UML.hasStereotype, european);
		OntResource mid = pkg("ExtMid", top);
		OntResource leaf = pkg("ExtLeaf", mid);
		cls("ExtClass", leaf);
		Path file = Files.writeString(dir.resolve("test.namespaces"), "European=" + EUROPEAN + "\n");
		Map<String, OntResource> named = new HashMap<>();
		named.put("European", european);
		StereotypedNamespaces sn = new StereotypedNamespaces(file.toFile(), named);
		assertTrue(sn.hasNamespaces());
		return sn;
	}

	@Test
	public void stereotypeOnGrandparentIsFoundForPackages() throws IOException {
		StereotypedNamespaces sn = stereotypeModel();
		Set<String> uris = translate(sn, true);
		assertHas(uris, "http://iec.ch/TC57/CIM100-European/Global/EuropeanExt#Package_EuropeanExt");
		assertHas(uris, "http://iec.ch/TC57/CIM100-European/Global/EuropeanExt/ExtMid#Package_ExtMid");
		assertHas(uris, "http://iec.ch/TC57/CIM100-European/Global/EuropeanExt/ExtMid/ExtLeaf#Package_ExtLeaf");
		assertHas(uris, EUROPEAN + "ExtClass");
		assertNoInternalNamespaces(uris);
	}

	// ---------------------------------------------------------------------
	// ResourceNamespaceResolver agrees with Translator
	// ---------------------------------------------------------------------

	@Test
	public void resolverAgreesWithTranslatorForClasses() {
		OntResource[] r = baseuriModel();
		ResourceNamespaceResolver on = new ResourceNamespaceResolver(raw, new StereotypedNamespaces(), CIM, true);
		assertEquals("http://iec.ch/TC57/CIM100/Global/TC57CIM/IEC61970/Wires#", on.findBaseURI(r[2]));
		assertEquals(EXT, on.findBaseURI(r[4]));
		ResourceNamespaceResolver off = new ResourceNamespaceResolver(raw, new StereotypedNamespaces(), CIM, false);
		assertEquals(CIM, off.findBaseURI(r[2]));
		assertEquals(EXT, off.findBaseURI(r[4]));
	}

	@Test
	public void resolverBasesPackagesOnTheSchemaNamespace() {
		OntResource[] r = baseuriModel();
		ResourceNamespaceResolver on = new ResourceNamespaceResolver(raw, new StereotypedNamespaces(), CIM, true);
		assertEquals(CIM, on.findBaseURI(r[0]));
		assertEquals(CIM, on.findBaseURI(r[1]));
		assertEquals(EXT, on.findBaseURI(r[3]));
	}

	@Test
	public void resolverFindsStereotypeOnGrandparent() throws IOException {
		StereotypedNamespaces sn = stereotypeModel();
		ResourceNamespaceResolver on = new ResourceNamespaceResolver(raw, sn, CIM, true);
		OntResource leafClass = null;
		for (Iterator<Triple> it = raw.getGraph().find(Node.ANY, RDFS.label.asNode(), Node.ANY); it.hasNext();) {
			Triple t = it.next();
			if ("ExtClass".equals(t.getObject().getLiteralLexicalForm()))
				leafClass = raw.createResource(t.getSubject());
		}
		assertEquals(EUROPEAN, on.findBaseURI(leafClass));
	}
}
