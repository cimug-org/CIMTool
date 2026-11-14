package au.com.langdale.util;

import java.util.regex.Pattern;

/**
 * Namespace utilities that handle both hash and slash namespaces (issue #377).
 *
 * <h2>Conventions</h2>
 * <p>
 * The W3C Working Group Note <i>Best Practice Recipes for Publishing RDF
 * Vocabularies</i> describes two conventions for the URIs of classes and
 * properties:
 * <ul>
 * <li>A <b>hash namespace</b> ends in {@code #}. The ontology IRI is the
 * namespace without its trailing {@code #}, for example ontology
 * {@code http://www.w3.org/2004/02/skos/core} and namespace
 * {@code http://www.w3.org/2004/02/skos/core#}.</li>
 * <li>A <b>slash namespace</b> ends in {@code /}. The ontology IRI and the
 * namespace are the same string, for example {@code http://purl.org/dc/terms/}.</li>
 * </ul>
 * <p>
 * CIMTool accepts a namespace only if it uses the {@code http:} or
 * {@code https:} scheme and ends in {@code #} or {@code /}. IRIs are always
 * compared as exact strings, so {@code http://purl.org/dc/terms} and
 * {@code http://purl.org/dc/terms/} are distinct and are never treated as
 * equivalent.
 *
 * <h2>Splitting rule</h2>
 * <p>
 * A URI is split at its last {@code #} or, if it contains none, at its last
 * {@code /}. Because a local name is an XML name and can contain neither
 * character, this rule is unambiguous for namespaces that end in {@code #} or
 * {@code /}.
 * <p>
 * This rule deliberately differs from Jena's namespace splitting
 * ({@code Node.getNameSpace()} and {@code Node.getLocalName()}), which chooses
 * the longest suffix that is a valid NCName. The two agree for well-formed CIM
 * names but disagree, for example, for a local name that begins with a digit.
 * Existing calls to Jena's methods elsewhere in CIMTool are intentionally left
 * unchanged.
 * <p>
 * The methods are total: they never throw for an unexpected string, which
 * keeps them safe to expose to XSLT builders as extension functions. A
 * {@code null} argument yields {@code null}.
 */
public final class NamespaceUtils {

	/**
	 * A namespace accepted by CIMTool: an {@code http:} or {@code https:} URI
	 * (scheme compared case-insensitively) with an authority, no whitespace,
	 * and a final {@code #} or {@code /}.
	 */
	public static final Pattern NAMESPACE_PATTERN = Pattern.compile("(?i:https?)://\\S+[#/]");

	/** The error message reported for a namespace that is not accepted. */
	public static final String NAMESPACE_ERROR = "The namespace must begin with a scheme such as 'http:' or 'https:' and end with a '#' or '/'";

	private NamespaceUtils() {
	}

	/**
	 * @param namespace a candidate namespace
	 * @return {@code true} if the namespace uses the {@code http:} or
	 *         {@code https:} scheme and ends in {@code #} or {@code /}
	 */
	public static boolean isValidNamespace(String namespace) {
		return namespace != null && NAMESPACE_PATTERN.matcher(namespace).matches();
	}

	/**
	 * @param namespace a namespace
	 * @return {@code true} if the namespace ends in {@code #}
	 */
	public static boolean isHashNamespace(String namespace) {
		return namespace != null && namespace.endsWith("#");
	}

	/**
	 * @param namespace a namespace
	 * @return {@code true} if the namespace ends in {@code /}
	 */
	public static boolean isSlashNamespace(String namespace) {
		return namespace != null && namespace.endsWith("/");
	}

	/**
	 * Returns the namespace part of a URI: everything up to and including its
	 * last {@code #} or, if it contains none, its last {@code /}.
	 *
	 * @param uri a URI such as {@code http://iec.ch/TC57/CIM100#ACLineSegment}
	 * @return the namespace, such as {@code http://iec.ch/TC57/CIM100#}; an
	 *         empty string if the URI contains neither character; or
	 *         {@code null} if the URI is {@code null}
	 */
	public static String namespaceOf(String uri) {
		if (uri == null)
			return null;
		int split = splitIndex(uri);
		return split < 0 ? "" : uri.substring(0, split + 1);
	}

	/**
	 * Returns the local name part of a URI: everything after its last {@code #}
	 * or, if it contains none, its last {@code /}.
	 *
	 * @param uri a URI such as {@code http://purl.org/dc/terms/title}
	 * @return the local name, such as {@code title}; the whole URI if it
	 *         contains neither character; or {@code null} if the URI is
	 *         {@code null}
	 */
	public static String localNameOf(String uri) {
		if (uri == null)
			return null;
		return uri.substring(splitIndex(uri) + 1);
	}

	/**
	 * Returns the ontology IRI for a namespace. For a hash namespace this is the
	 * namespace without its trailing {@code #}. For a slash namespace it is the
	 * namespace itself.
	 *
	 * @param namespace a namespace ending in {@code #} or {@code /}
	 * @return the ontology IRI
	 * @throws IllegalArgumentException if the namespace ends in neither character
	 */
	public static String ontologyUriFor(String namespace) {
		if (isHashNamespace(namespace))
			return namespace.substring(0, namespace.length() - 1);
		if (isSlashNamespace(namespace))
			return namespace;
		throw new IllegalArgumentException("Not a hash or slash namespace: " + namespace);
	}

	/**
	 * Derives the namespace for an ontology IRI when no namespace has been
	 * declared explicitly. An IRI ending in {@code /} is taken to be a slash
	 * namespace; any other IRI is given a trailing {@code #}, which preserves
	 * CIMTool's historical behaviour.
	 * <p>
	 * This derivation cannot recover a hash namespace whose ontology IRI itself
	 * ends in {@code /} (for example namespace {@code http://example.org/ns/#}).
	 * An explicitly declared namespace, such as {@code vann:preferredNamespaceUri}
	 * in a profile header, takes precedence over this method wherever one exists.
	 *
	 * @param ontologyUri an ontology IRI
	 * @return the derived namespace, or {@code null} if the IRI is {@code null}
	 */
	public static String namespaceFor(String ontologyUri) {
		if (ontologyUri == null)
			return null;
		if (ontologyUri.endsWith("/") || ontologyUri.endsWith("#"))
			return ontologyUri;
		return ontologyUri + "#";
	}

	/**
	 * Tests whether a declared namespace agrees with an ontology IRI, that is,
	 * whether the namespace is the IRI followed by {@code #}, or is a slash
	 * namespace equal to the IRI.
	 *
	 * @param ontologyUri an ontology IRI
	 * @param namespace   a declared namespace
	 * @return {@code true} if the two agree
	 */
	public static boolean isConsistent(String ontologyUri, String namespace) {
		if (ontologyUri == null || namespace == null)
			return false;
		if (isHashNamespace(namespace))
			return namespace.equals(ontologyUri + "#");
		if (isSlashNamespace(namespace))
			return namespace.equals(ontologyUri);
		return false;
	}

	/**
	 * Builds the namespace of a package beneath a base namespace, following the
	 * convention of the base: {@code http://x/y#} with {@code Wires} yields
	 * {@code http://x/y/Wires#}, and {@code http://x/y/} with {@code Wires}
	 * yields {@code http://x/y/Wires/}.
	 * <p>
	 * For a hash base, any {@code /} characters before the trailing {@code #}
	 * are also removed, so {@code http://x/y/#} yields {@code http://x/y/Wires#}.
	 * This preserves the output of the historical {@code stripHash()} helper for
	 * hash namespaces exactly.
	 *
	 * @param baseNamespace a namespace ending in {@code #} or {@code /}
	 * @param packagePath   one or more package names, separated by {@code /}
	 * @return the package namespace
	 * @throws IllegalArgumentException if the base namespace ends in neither
	 *                                  character
	 */
	public static String childNamespace(String baseNamespace, String packagePath) {
		if (isHashNamespace(baseNamespace)) {
			String base = ontologyUriFor(baseNamespace);
			while (base.endsWith("/"))
				base = base.substring(0, base.length() - 1);
			return base + "/" + packagePath + "#";
		}
		if (isSlashNamespace(baseNamespace))
			return baseNamespace + packagePath + "/";
		throw new IllegalArgumentException("Not a hash or slash namespace: " + baseNamespace);
	}

	private static int splitIndex(String uri) {
		int hash = uri.lastIndexOf('#');
		return hash >= 0 ? hash : uri.lastIndexOf('/');
	}
}
