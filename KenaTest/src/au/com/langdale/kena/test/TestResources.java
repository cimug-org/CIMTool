package au.com.langdale.kena.test;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;

/**
 * Shared support for the KenaTest suite: the sample data in
 * <code>test-resources</code>, which is on the bundle class path, and the
 * triple and pattern helpers the split model and extractor tests use.
 */
public final class TestResources {

	/** A CIM 2007 CPSM profile, read by the RDF/XML tests. */
	public static final String SAMPLE_PROFILE = "cpsm2007.owl";

	/** A CIM/XML network model written in split model form, one part per hash bucket. */
	public static final String SPLIT_MODEL = "ABB40BusIOP10_rev7";

	/** The namespace of the network model in {@link #SPLIT_MODEL}. */
	public static final String NET = "http://www.ucaiug.org/network#";

	/** The CIM namespace of the network model in {@link #SPLIT_MODEL}. */
	public static final String CIM = "http://iec.ch/TC57/2007/CIM-schema-cim12#";

	/** Passed for any position of a pattern that should match anything. */
	public static final String ANY = null;

	private TestResources() {
	}

	/** Opens a file in <code>test-resources</code>. */
	public static InputStream open(String name) {
		InputStream stream = TestResources.class.getClassLoader().getResourceAsStream(name);
		if (stream == null)
			throw new IllegalStateException(name + " is not on the class path");
		return stream;
	}

	/**
	 * Returns the directory of the split model in <code>test-resources</code>.
	 * It is found through the class path, so the tests do not depend on the
	 * working directory they are run from.
	 */
	public static String splitModelLocation() {
		URL part = TestResources.class.getClassLoader().getResource(SPLIT_MODEL + "/Part00.ttl");
		if (part == null)
			throw new IllegalStateException(SPLIT_MODEL + " is not on the class path");
		try {
			return new File(part.toURI()).getParent();
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	public static TriplePattern pattern(String s, String p, String o) {
		return new TriplePattern(uri(s), uri(p), literal(o));
	}

	public static TriplePattern pattern(String s, String p, Node o) {
		return new TriplePattern(uri(s), uri(p), o);
	}

	public static Node uri(String s) {
		return s == null ? Node.ANY : Node.createURI(s);
	}

	public static Node literal(String s) {
		return s == null ? Node.ANY : Node.createLiteral(s);
	}

	public static Triple triple(String s, String p, String o) {
		return new Triple(Node.createURI(s), Node.createURI(p), Node.createLiteral(o));
	}
}
