/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders;

/**
 * Enumeration defining the types of builders supported by CIMTool.
 * 
 * <ul>
 * <li>{@link #TEXT} - General text output builders (Java, SQL, JSON, etc.)</li>
 * <li>{@link #ASCIIDOC} - AsciiDoc document builders that preserve authored
 * whitespace</li>
 * <li>{@link #XSD} - XML Schema builders with validation</li>
 * <li>{@link #TRANSFORM} - Generic XML transform builders</li>
 * <li>{@link #JAVA} - Java-based builders that use generator classes directly
 * rather than XSLT</li>
 * </ul>
 */
public enum BuilderType {

	/**
	 * Text output builders. These produce text-based artifacts and apply
	 * indentation post-processing.
	 */
	TEXT,

	/**
	 * AsciiDoc document builders. These behave identically to {@link #TEXT} in all
	 * respects except post-processing stylesheet selection: the indent-asciidoc.xsl
	 * stylesheet is applied instead of indent.xsl so that authored whitespace in
	 * AsciiDoc documentation (marked xml:space="preserve" by
	 * ProfileSerializer.emitAsciiDoc()) is preserved verbatim. See defect #288.
	 */
	ASCIIDOC,

	/**
	 * XML Schema builders. These produce XSD schemas and perform XML Schema
	 * validation on the output.
	 */
	XSD,

	/**
	 * Generic transform builders. These produce XML output without additional
	 * post-processing or validation.
	 */
	TRANSFORM,

	/**
	 * Java-based builders. These use CIMTool generator classes directly rather than
	 * XSLT stylesheets. See {@link JavaGeneratorType} for the specific generator
	 * implementations available.
	 */
	JAVA;

	/**
	 * Safely parse a string to a BuilderType.
	 * 
	 * @param value the string value to parse
	 * @return the corresponding BuilderType, or null if not found
	 */
	public static BuilderType fromString(String value) {
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