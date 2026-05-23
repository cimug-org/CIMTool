/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders;

/**
 * Enumeration identifying the Java-based generator implementation to use for
 * builders of type {@link BuilderType#JAVA}.
 *
 * <ul>
 * <li>{@link #OWL} - Generates a simple OWL representation via
 * {@code OWLGenerator}</li>
 * <li>{@link #RDFS} - Generates a legacy RDFS representation via
 * {@code RDFSGenerator}</li>
 * <li>{@link #COPY} - Copies the profile model directly in the specified RDF
 * serialization format</li>
 * <li>{@link #PROFILE_SERIALIZER} - Serializes the internal CIMTool message
 * model as formatted XML via {@code ProfileSerializer} with the
 * {@code indent-xml.xsl} post-processor</li>
 * </ul>
 */
public enum JavaGeneratorType {

	/**
	 * Generates a simple OWL representation of the profile using
	 * {@code OWLGenerator}. Used by the {@code simple-flat-owl},
	 * {@code simple-owl}, {@code simple-flat-owl-augmented}, and
	 * {@code simple-owl-augmented} builders.
	 */
	OWL,

	/**
	 * Generates a legacy RDFS representation of the profile using
	 * {@code RDFSGenerator}. Used by the {@code legacy-rdfs} and
	 * {@code legacy-rdfs-augmented} builders.
	 */
	RDFS,

	/**
	 * Copies the profile OWL model directly to the output in the specified RDF
	 * serialization format. Used by the {@code ttl} builder.
	 */
	COPY,

	/**
	 * Serializes the CIMTool internal message model as formatted XML using
	 * {@code ProfileSerializer} with the {@code indent-xml.xsl} post-processor.
	 * Used by the {@code xml} builder.
	 */
	PROFILE_SERIALIZER;

	/**
	 * Safely parse a string to a JavaGeneratorType.
	 *
	 * @param value the string value to parse
	 * @return the corresponding JavaGeneratorType, or null if not found
	 */
	public static JavaGeneratorType fromString(String value) {
		if (value == null) {
			return null;
		}
		try {
			return valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
