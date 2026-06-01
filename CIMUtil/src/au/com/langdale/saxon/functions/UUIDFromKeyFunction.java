package au.com.langdale.saxon.functions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

/**
 * Saxon integrated extension function that exposes a deterministic,
 * identity-based UUID generator to XSLT, taking a single string key.
 *
 * <h2>What it does</h2>
 * <p>
 * Each call returns an RFC 4122 version 3 (name-based, MD5) UUID derived from
 * the supplied key string, in canonical lowercase {@code 8-4-4-4-12} hyphenated
 * form. The mapping is deterministic: the same key always produces the same
 * UUID — every call, every run, every build. Different keys produce different
 * UUIDs with negligible collision probability. The implementation delegates to
 * {@link java.util.UUID#nameUUIDFromBytes(byte[])} on the key's UTF-8 byte
 * representation.
 *
 * <h2>Primary use case</h2>
 * <p>
 * This function was originally designed to support generating stable UUIDs for
 * the two ends of a UML association as it is represented internally in CIMTool.
 * In a UML modeling tool such as Sparx Enterprise Architect, every element —
 * class, attribute, enumeration literal, association — carries its own unique
 * identifier, and an association is a single element with a single id.
 * CIMTool's internal OWL/RDF representation, however, splits an association
 * into two independent properties (one per role end), so it needs an identifier
 * for each end derived from the single id the association came in with. The
 * convention CIMTool uses is to append {@code -A} for the source role and
 * {@code -B} for the target role, yielding two stable, distinguishable keys per
 * association.
 * <p>
 * Those suffixed strings are not themselves valid UUIDs, so they cannot be used
 * as-is in output that expects RFC 4122 form. Passing them through this
 * function produces real, well-formed UUIDs that are nevertheless
 * deterministically tied to the original identifier: the same association end
 * always resolves to the same UUID across regenerations, and the two ends of
 * one association resolve to two distinct UUIDs. The function is not limited to
 * that use, however — any case requiring a stable UUID derived from a string
 * identity is a valid call site.
 *
 * <h2>Why this is a Java extension function</h2>
 * <p>
 * Deterministic functions can in principle be expressed as XSLT 3.0
 * {@code xsl:function}s, but XPath 3.0 has no built-in cryptographic digest. A
 * stylesheet-only implementation would have to hand-roll a hash function, which
 * is workable but considerably more code, weaker in bit-spread than a real
 * digest, and harder for future maintainers to reason about. Delegating to
 * {@code UUID.nameUUIDFromBytes(byte[])} yields a properly formed RFC 4122 v3
 * UUID with no hand-rolled hashing.
 *
 * <h2>Registration</h2>
 * <p>
 * This class is a function <em>definition</em>, not a function on its own.
 * Saxon only recognises it after it is registered on the
 * {@link net.sf.saxon.Configuration} backing the
 * {@link javax.xml.transform.TransformerFactory} that compiles the stylesheet.
 * Registration <strong>must</strong> happen before {@code newTemplates(...)}
 * compiles any stylesheet that references the function; otherwise compilation
 * fails with "unknown function." Typical pattern:
 * 
 * <pre>{@code
 * Configuration config = ((TransformerFactoryImpl) factory).getConfiguration();
 * config.registerExtensionFunction(new UUIDFromKeyFunction());
 * }</pre>
 * 
 * If the application drives Saxon through s9api
 * ({@code Processor}/{@code XsltExecutable}) instead of JAXP, the equivalent
 * registration is {@code processor.registerExtensionFunction(...)} with an
 * {@code ExtensionFunction} adapter; the underlying mechanism is the same.
 *
 * <h2>Use from XSLT</h2>
 * <p>
 * Declare the namespace on {@code <xsl:stylesheet>}, binding it to any prefix
 * the stylesheet author prefers:
 * 
 * <pre>{@code xmlns:cimtool="http://cimtool.ucaiug.io/functions"}</pre>
 * 
 * and call:
 * 
 * <pre>{@code <xsl:value-of select=
 * "cimtool:uuidFromKey('some-stable-key')"/>}</pre>
 * 
 * What binds the call to this function is the namespace URI (see
 * {@link #NAMESPACE_URI}); the prefix is arbitrary.
 *
 * <h2>Determinism</h2>
 * <p>
 * This function is intentionally deterministic — it is the counterpart to the
 * non-deterministic random-UUID function and is intended for identity-style ids
 * (the same logical thing keeps the same UUID across regenerations) rather than
 * event-style ids (each generation is a distinct event). Two consequences worth
 * knowing:
 * <ul>
 * <li>The output is byte-stable across builds and across machines (UTF-8
 * encoding of the key is explicit), so generated artifacts can be diffed and
 * reused without churn.</li>
 * <li>The mapping is one-way: a UUID cannot be reversed back into the key that
 * produced it, because MD5 is a cryptographic hash. If the original key needs
 * to be recoverable, emit it as a sibling field in the output rather than
 * relying on inverting the UUID.</li>
 * </ul>
 *
 * <h2>Input handling</h2>
 * <p>
 * The function hashes the raw UTF-8 bytes of the key. Two consequences follow,
 * and call sites must respect both for the identity guarantee to hold:
 * <ul>
 * <li><strong>Case-sensitive.</strong> The bytes for {@code 'A'} (0x41) and
 * {@code 'a'} (0x61) differ, so the keys {@code "EAID_FFF87758"} and
 * {@code "eaid_fff87758"} hash to entirely different UUIDs. The function does
 * no case-folding, normalisation, trimming, or canonicalisation of the input —
 * what is supplied is what is hashed.</li>
 * <li><strong>Byte-exact.</strong> Whitespace, separators, prefixes (e.g.
 * {@code "EAID_"}), and any other lexical variation are part of the key.
 * {@code "abc-def"} and {@code "ABC-DEF"} are different keys; so are
 * {@code "abc-def"} and {@code "abc_def"}.</li>
 * </ul>
 * <p>
 * The practical implication is that call sites must agree on a single canonical
 * key form. If half a stylesheet passes raw upstream identifiers (e.g.
 * {@code "EAID_FFF87758_..."}) and the other half passes a normalised form
 * (e.g. {@code "fff87758-..."}), then the same logical source identity will
 * resolve to two different UUIDs depending on which call site emits it.
 * Choosing a single normalisation and applying it consistently before calling
 * this function is the call site's responsibility — the function intentionally
 * does not impose one, because the appropriate canonical form is
 * application-specific.
 * <p>
 * Note the asymmetry with the output: the returned UUID is always in Java's
 * canonical lowercase hyphenated form regardless of input case (a contract of
 * {@link UUID#toString()}). Wrapping the result in a further normalisation step
 * is therefore redundant; wrapping the input in one is usually meaningful work.
 *
 * <h2>Empty input</h2>
 * <p>
 * An empty or whitespace-only key is treated as a model-quality error and
 * raises an {@link net.sf.saxon.trans.XPathException}. Hashing an empty string
 * would produce a fixed UUID that is almost certainly meaningless as an
 * identity, so the function refuses rather than silently emit it.
 *
 * <h2>Recommended call pattern</h2>
 * <p>
 * When the source identifier may arrive in varying lexical forms (different
 * cases, optional prefixes, alternative separators), apply the normalisation
 * <em>before</em> calling this function — not after:
 * 
 * <pre>{@code
 * <!-- Correct: normalisation feeds the hash, locking identity stability -->
 * <xsl:value-of select="cimtool:uuidFromKey( normalise( $rawId ) )"/>
 *
 * <!-- Redundant: the result is already canonical; this wraps a no-op -->
 * <xsl:value-of select="normalise( cimtool:uuidFromKey( $rawId ) )"/>
 * }</pre>
 * 
 * The first form ensures every equivalent input shape maps to the same key
 * bytes, and therefore the same UUID. The second form does no useful work,
 * because {@link UUID#toString()} already produces canonical lowercase form
 * regardless of input. The cost is small (one extra function call per id) but
 * the composition obscures the contract: it reads as if the wrapper has work to
 * do when in fact it does not.
 * <p>
 * Where {@code normalise(...)} is any project-specific canonicalisation —
 * typically lowercasing, prefix stripping, or separator normalisation —
 * appropriate to the identifier shape being hashed.
 *
 * <h2>Saxon edition compatibility</h2>
 * <p>
 * The integrated-extension-function API ({@link ExtensionFunctionDefinition},
 * {@link net.sf.saxon.Configuration#registerExtensionFunction}) is supported on
 * Saxon HE 10.x — no PE/EE licence is required.
 *
 * @see net.sf.saxon.lib.ExtensionFunctionDefinition
 * @see net.sf.saxon.Configuration#registerExtensionFunction(ExtensionFunctionDefinition)
 * @see java.util.UUID#nameUUIDFromBytes(byte[])
 */
public class UUIDFromKeyFunction extends ExtensionFunctionDefinition {

	/** Namespace URI under which this function is exposed to XSLT. */
	public static final String NAMESPACE_URI = "http://cimtool.ucaiug.io/functions";

	/**
	 * The fully-qualified XPath function name. The first argument to
	 * {@link StructuredQName} is a <em>suggested</em> prefix only — what actually
	 * binds a stylesheet's call to this function is the namespace URI matching the
	 * stylesheet's {@code xmlns} declaration. The local name and arity together
	 * must be unique within the namespace; a collision with another extension
	 * function or an {@code xsl:function} sharing this namespace, local name, and
	 * arity would be a compile-time error.
	 */
	private static final StructuredQName NAME = new StructuredQName("cimtool", NAMESPACE_URI, "uuidFromKey");

	/**
	 * Returns the fully-qualified XPath name of this function.
	 * <p>
	 * Saxon calls this once during registration to populate its function lookup
	 * table. The triple of (namespace URI, local name, arity) — where arity is
	 * derived from {@link #getArgumentTypes()} — is the unique key under which the
	 * function will be discoverable from XSLT.
	 */
	@Override
	public StructuredQName getFunctionQName() {
		return NAME;
	}

	/**
	 * Returns the function's argument type signature. One required argument,
	 * declared as exactly one {@code xs:string} (not optional, not a sequence): the
	 * key string used to derive the UUID.
	 * <p>
	 * A variant taking different argument types or counts (for example a
	 * two-argument namespaced form combining a namespace string with a key string
	 * per the full RFC 4122 v3/v5 spec) should be defined as a separate
	 * {@link ExtensionFunctionDefinition} subclass. Saxon dispatches by arity, so
	 * different arities of the same local name are distinct functions; giving them
	 * distinct local names is usually clearer for stylesheet authors than relying
	 * on arity overloading.
	 */
	@Override
	public SequenceType[] getArgumentTypes() {
		return new SequenceType[] { SequenceType.SINGLE_STRING };
	}

	/**
	 * Returns the function's result type, declared as exactly one
	 * {@link SequenceType#SINGLE_STRING xs:string}, matching the
	 * {@link StringValue} returned from {@link ExtensionFunctionCall#call
	 * call(...)}.
	 * <p>
	 * Saxon uses this in static analysis of the stylesheet (e.g. confirming the
	 * result is a valid operand wherever the call appears), so the declared type
	 * must accurately describe what {@code call(...)} produces; a mismatch surfaces
	 * as a runtime type error.
	 * <p>
	 * The {@code suppliedArgumentTypes} parameter allows a function to refine its
	 * return type based on the types it was actually called with — useful for
	 * generic functions whose return type depends on input. Not relevant here: the
	 * return type is fixed regardless of what string is passed in.
	 */
	@Override
	public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
		return SequenceType.SINGLE_STRING;
	}

	/**
	 * Produces the {@link ExtensionFunctionCall} that Saxon invokes when the
	 * stylesheet calls this function. A fresh {@code ExtensionFunctionCall}
	 * instance is created per call site at stylesheet-compile time, so any
	 * per-call-site state would live on the returned instance. This function is
	 * stateless, so the inner class simply hashes the supplied key on every
	 * invocation.
	 */
	@Override
	public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {
			/**
			 * Invoked by Saxon for every evaluation of the function.
			 * <p>
			 * The {@code arguments} array has one element, the key string (matches
			 * {@link #getArgumentTypes()}). The {@link XPathContext} is unused here — it
			 * would carry the dynamic context (current node, current date, etc.) if this
			 * function depended on it; see {@link #dependsOnFocus()}.
			 * <p>
			 * The key is hashed via {@link UUID#nameUUIDFromBytes(byte[])} over its UTF-8
			 * byte representation. UTF-8 is specified explicitly rather than relying on the
			 * platform default charset, so the same key produces the same UUID regardless
			 * of where the build runs.
			 *
			 * @return a single {@link StringValue} wrapping the canonical lowercase string
			 *         form of the derived UUID.
			 * @throws XPathException if the key is empty or whitespace-only.
			 */
			@Override
			public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
				String key = arguments[0].head().getStringValue();
				if (key == null || key.trim().isEmpty()) {
					throw new XPathException(
							"cimtool:uuidFromKey($key) requires a non-empty key; got empty or whitespace-only input");
				}
				byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
				return new StringValue(UUID.nameUUIDFromBytes(bytes).toString());
			}
		};
	}

	/**
	 * Declares this function as <strong>pure</strong> (not side-effecting): given
	 * the same key, it always returns the same UUID. Saxon is free to memoise
	 * repeated calls with the same argument, hoist calls out of loops, and
	 * otherwise optimise — which is the correct tradeoff for a deterministic
	 * function. In practice this means calling
	 * {@code cimtool:uuidFromKey('classes')} ten times in one transform hashes
	 * once, not ten times.
	 * <p>
	 * <strong>Guidance for similar functions:</strong> return {@code false} for any
	 * function whose result depends only on its arguments — pure transformations,
	 * deterministic derivations, lookups in read-only structures. Return
	 * {@code true} instead for any function whose result depends on something other
	 * than its arguments (randomness, the wall clock, mutable external state,
	 * network calls); the value of {@link #hasSideEffects()} controls whether Saxon
	 * may optimise the call, and an incorrect declaration in either direction
	 * produces subtle bugs — silent memoisation of a function that should differ
	 * per call, or repeated evaluation of an expensive pure function.
	 */
	@Override
	public boolean hasSideEffects() {
		return false;
	}

	/**
	 * Declares whether this function depends on the XPath "focus" — the current
	 * context node, position, and size of the dynamic evaluation context.
	 * {@code false} here because the function's result depends solely on the
	 * supplied key argument; the node currently being processed has no influence.
	 * <p>
	 * A function that needs the focus (e.g. one that reads the current node's
	 * attributes without them being passed explicitly) must return {@code true},
	 * and the {@link XPathContext} parameter to {@code call(...)} would be used to
	 * retrieve it. Declaring focus dependence accurately matters because Saxon uses
	 * this in optimisation and parallelisation decisions.
	 */
	@Override
	public boolean dependsOnFocus() {
		return false;
	}
}