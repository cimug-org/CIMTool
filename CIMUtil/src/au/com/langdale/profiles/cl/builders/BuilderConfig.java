/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders;

import java.time.Instant;
import java.util.Objects;

/**
 * Configuration data for a builder — either XSLT-based or Java-based.
 * 
 * This is a simple POJO that holds the metadata for a builder without any
 * Eclipse-specific dependencies. It can be used both in the Eclipse plugin
 * context and in standalone command-line applications.
 * 
 * <p>
 * The configuration corresponds to entries in builders.json or
 * java-builders.json:
 * </p>
 * 
 * <pre>
 * {
 *     "xsd": {
 *         "type": "XSD",
 *         "style": "xsd",
 *         "datetime": "2020-05-01T00:00:00Z",
 *         "ext": "xsd"
 *     },
 *     "simple-flat-owl": {
 *         "type": "JAVA",
 *         "javaGenerator": "OWL",
 *         "format": "RDF/XML",
 *         "withInverses": false,
 *         "datetime": "2020-05-01T00:00:00Z",
 *         "ext": "simple-flat-owl"
 *     }
 * }
 * </pre>
 */
public class BuilderConfig {

	private final String style;
	private final BuilderType type;
	private final String extension;
	private final Instant createdDateTime;
	private final String includesFile;

	// Fields used only when type == JAVA
	private final JavaGeneratorType javaGenerator;
	private final String format;
	private final boolean withInverses;

	/**
	 * Create a new BuilderConfig for an XSLT-based builder.
	 * 
	 * @param style           the style name (used as XSL filename prefix, e.g.,
	 *                        "xsd" for xsd.xsl)
	 * @param type            the builder type
	 * @param extension       the output file extension (e.g., "xsd", "json",
	 *                        "draft-07.schema.json")
	 * @param createdDateTime when this builder configuration was created
	 * @param includesFile    optional path to an XSL includes file (may be null)
	 */
	public BuilderConfig(String style, BuilderType type, String extension, Instant createdDateTime,
			String includesFile) {
		this(style, type, extension, createdDateTime, includesFile, null, null, false);
	}

	/**
	 * Create a new BuilderConfig without an includes file.
	 * 
	 * @param style           the style name
	 * @param type            the builder type
	 * @param extension       the output file extension
	 * @param createdDateTime when this builder configuration was created
	 */
	public BuilderConfig(String style, BuilderType type, String extension, Instant createdDateTime) {
		this(style, type, extension, createdDateTime, null, null, null, false);
	}

	/**
	 * Create a new BuilderConfig for a Java-based builder.
	 *
	 * @param style           the builder name (used as the key in
	 *                        java-builders.json)
	 * @param type            must be {@link BuilderType#JAVA}
	 * @param extension       the output file extension
	 * @param createdDateTime when this builder configuration was created
	 * @param javaGenerator   the generator implementation to use
	 * @param format          the RDF serialization format (e.g., "RDF/XML",
	 *                        "TURTLE"); null for PROFILE_SERIALIZER
	 * @param withInverses    whether to include inverse properties in the generated
	 *                        output
	 */
	public BuilderConfig(String style, BuilderType type, String extension, Instant createdDateTime,
			JavaGeneratorType javaGenerator, String format, boolean withInverses) {
		this(style, type, extension, createdDateTime, null, javaGenerator, format, withInverses);
	}

	/**
	 * Full constructor.
	 */
	public BuilderConfig(String style, BuilderType type, String extension, Instant createdDateTime, String includesFile,
			JavaGeneratorType javaGenerator, String format, boolean withInverses) {
		this.style = Objects.requireNonNull(style, "style must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.extension = Objects.requireNonNull(extension, "extension must not be null");
		this.createdDateTime = createdDateTime != null ? createdDateTime : Instant.now();
		this.includesFile = includesFile;
		this.javaGenerator = javaGenerator;
		this.format = format;
		this.withInverses = withInverses;
	}

	/**
	 * @return the style name (used as XSL filename prefix)
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * @return the builder type
	 */
	public BuilderType getType() {
		return type;
	}

	/**
	 * @return the output file extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * @return when this builder configuration was created
	 */
	public Instant getCreatedDateTime() {
		return createdDateTime;
	}

	/**
	 * @return the path to an XSL includes file, or null if none
	 */
	public String getIncludesFile() {
		return includesFile;
	}

	/**
	 * @return true if this builder has an associated includes file
	 */
	public boolean hasIncludesFile() {
		return includesFile != null && !includesFile.isEmpty();
	}

	/**
	 * @return the expected XSL filename (style + ".xsl")
	 */
	public String getXslFileName() {
		return style + ".xsl";
	}

	/**
	 * @return the Java generator type; only non-null when {@link #getType()} is
	 *         {@link BuilderType#JAVA}
	 */
	public JavaGeneratorType getJavaGenerator() {
		return javaGenerator;
	}

	/**
	 * @return the RDF serialization format string (e.g., "RDF/XML", "TURTLE"); null
	 *         for {@link JavaGeneratorType#PROFILE_SERIALIZER} builders
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * @return true if this builder should include inverse properties in its output;
	 *         only meaningful for {@link JavaGeneratorType#OWL} and
	 *         {@link JavaGeneratorType#RDFS} builders
	 */
	public boolean isWithInverses() {
		return withInverses;
	}

	/**
	 * @return true if this is a Java-based builder
	 */
	public boolean isJavaBuilder() {
		return type == BuilderType.JAVA;
	}

	/**
	 * Get a display description suitable for showing in UI lists.
	 * 
	 * @return a description in the format "style (.extension)"
	 */
	public String getDisplayDescription() {
		return style + "  (." + extension + ")";
	}

	@Override
	public int hashCode() {
		return Objects.hash(style);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		BuilderConfig other = (BuilderConfig) obj;
		return Objects.equals(style, other.style);
	}

	@Override
	public String toString() {
		if (type == BuilderType.JAVA) {
			return "BuilderConfig[style=" + style + ", type=JAVA, generator=" + javaGenerator + ", format=" + format
					+ ", withInverses=" + withInverses + ", ext=" + extension + "]";
		}
		return "BuilderConfig[style=" + style + ", type=" + type + ", ext=" + extension + ", includes=" + includesFile
				+ "]";
	}
}