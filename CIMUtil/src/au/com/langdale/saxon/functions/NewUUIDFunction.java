package au.com.langdale.saxon.functions;

import java.util.UUID;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

/**
 * Saxon integrated extension function that exposes a zero-argument UUID
 * generator to XSLT.
 *
 * <h2>What it does</h2>
 * <p>
 * Each call returns a fresh RFC 4122 version 4 UUID in canonical lowercase
 * {@code 8-4-4-4-12} hyphenated form, e.g.
 * {@code "fff87758-6536-44c9-9a1d-805ff10be85d"}. The implementation delegates
 * to {@link java.util.UUID#randomUUID()}, so the entropy source is the JVM's
 * {@code SecureRandom}.
 *
 * <h2>Why this is a Java extension function</h2>
 * <p>
 * XSLT 3.0 {@code xsl:function} declarations are required to be deterministic —
 * given the same arguments, they must return the same value. Genuinely random
 * generation is therefore not expressible as an {@code xsl:function}. A
 * stylesheet-only approximation would have to be deterministic (same UUID on
 * every rebuild for the same seed) or clock-derived (varying only by the
 * current time). Neither produces fresh-per-call values within a single
 * transform, which is the semantic this function provides.
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
 * net.sf.saxon.Configuration config = ((net.sf.saxon.TransformerFactoryImpl) factory).getConfiguration();
 * config.registerExtensionFunction(new NewUUIDFunction());
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
 * <pre>{@code <xsl:value-of select="cimtool:newUUID()"/>}</pre>
 * 
 * What binds the call to this function is the namespace URI (see
 * {@link #NAMESPACE_URI}); the prefix is arbitrary.
 *
 * <h2>Determinism</h2>
 * <p>
 * This function is intentionally non-deterministic. Builds that include its
 * output are not reproducible at the level of the generated UUID values — the
 * same input produces different UUIDs on each run. Tests that compare generated
 * output byte-for-byte cannot anchor on these fields. If reproducibility is
 * required at a particular call site, use a deterministic alternative instead
 * (e.g. a seeded UUID function, or a UUID derived from a stable identifier in
 * the source data).
 *
 * <h2>Saxon edition compatibility</h2>
 * <p>
 * The integrated-extension-function API ({@link ExtensionFunctionDefinition},
 * {@link net.sf.saxon.Configuration#registerExtensionFunction}) is supported on
 * Saxon HE 10.x — no PE/EE licence is required.
 *
 * @see net.sf.saxon.lib.ExtensionFunctionDefinition
 * @see net.sf.saxon.Configuration#registerExtensionFunction(ExtensionFunctionDefinition)
 * @see java.util.UUID#randomUUID()
 */
public class NewUUIDFunction extends ExtensionFunctionDefinition {

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
	private static final StructuredQName NAME = new StructuredQName("cimtool", NAMESPACE_URI, "newUUID");

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
	 * Returns the function's argument type signature. An empty array declares a
	 * zero-argument function.
	 * <p>
	 * A variant that needs arguments should be defined as a separate
	 * {@link ExtensionFunctionDefinition} subclass with the appropriate
	 * {@link SequenceType} entries here. Saxon dispatches by arity, so different
	 * arities of the same local name are distinct functions; giving them distinct
	 * local names is usually clearer for stylesheet authors than relying on arity
	 * overloading.
	 */
	@Override
	public SequenceType[] getArgumentTypes() {
		return new SequenceType[0];
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
	 * generic functions whose return type depends on input. Not relevant here: this
	 * function takes no arguments, so the return type is fixed.
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
	 * stateless, so the inner class simply delegates to {@link UUID#randomUUID()}
	 * on every invocation.
	 * <p>
	 * Each invocation of {@code call(...)} corresponds to one runtime evaluation of
	 * the function in the stylesheet — see {@link #hasSideEffects()} for why that's
	 * "each call" rather than "each call site."
	 */
	@Override
	public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {
			/**
			 * Invoked by Saxon for every evaluation of the function.
			 * <p>
			 * The {@code arguments} array is empty (matches {@link #getArgumentTypes()}).
			 * The {@link XPathContext} is unused here — it would carry the dynamic context
			 * (current node, current date, etc.) if this function depended on it; see
			 * {@link #dependsOnFocus()}.
			 *
			 * @return a single {@link StringValue} wrapping the result of
			 *         {@code UUID.randomUUID().toString()}.
			 */
			@Override
			public Sequence call(XPathContext context, Sequence[] arguments) {
				return new StringValue(UUID.randomUUID().toString());
			}
		};
	}

	/**
	 * Declares this function as <strong>side-effecting</strong>: it may return a
	 * different result on each call, even when called with the same arguments.
	 * <p>
	 * This is the single most important correctness lever in the class. Saxon's
	 * optimiser is permitted to lift "pure" function calls out of loops, memoise
	 * their results, or reorder them, on the assumption that a pure function called
	 * twice with identical arguments returns identical values. For a UUID generator
	 * that optimisation would be a bug:
	 * 
	 * <pre>{@code
	 *     <xsl:for-each select="...">
	 *         <item><xsl:value-of select="cimtool:newUUID()"/></item>
	 *     </xsl:for-each>
	 * }</pre>
	 * 
	 * with the default (purity assumed) Saxon could hoist the call, evaluate it
	 * once, and emit the same UUID for every iteration — silently destroying the
	 * uniqueness the function exists to provide.
	 * <p>
	 * Returning {@code true} disables those optimisations: Saxon evaluates the
	 * function at every call site and at every iteration. The performance cost is
	 * negligible ({@code UUID.randomUUID()} is microsecond-scale) and the
	 * correctness guarantee is absolute.
	 * <p>
	 * <strong>Guidance for similar functions:</strong> return {@code true} for any
	 * function whose result depends on something other than its arguments —
	 * randomness, the wall clock, mutable external state, network calls, anything
	 * that could change between calls. Leave it as the default for genuinely pure
	 * functions, which lets Saxon optimise them — the correct tradeoff when the
	 * function really is pure.
	 */
	@Override
	public boolean hasSideEffects() {
		return true;
	}

	/**
	 * Declares whether this function depends on the XPath "focus" — the current
	 * context node, position, and size of the dynamic evaluation context.
	 * {@code false} here because UUID generation does not depend on the node
	 * currently being processed.
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