/**
 * 
 */
package au.com.langdale.xmi;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * <pre>
 * XSD 1.0 and 1.1 Primitive Datatypes:
 * 
 * Category			Primitive Type		In XSD 1.0		In XSD 1.1		Notes
 * --------			--------------		----------		----------		-----
 * String			string				Yes				Yes				Base for many string-like types
 * 					normalizedString	No				No				Derived, not primitive
 * Boolean			boolean				Yes				Yes				Values: true/false or 1/0
 * Decimal			decimal				Yes				Yes				Arbitrary precision
 * 					integer				No				No				Derived from decimal
 * Float			float				Yes				Yes				32-bit IEEE
 * 					double				Yes				Yes				64-bit IEEE
 * Duration			duration			Yes				Yes				ISO 8601 duration
 * Date/Time		dateTime			Yes				Yes				Date and time with time zone
 * 					time				Yes				Yes				Time of day
 * 					date				Yes				Yes				Calendar date
 * 					gYearMonth			Yes				Yes				Year and month
 * 					gYear				Yes				Yes				Year only
 * 					gMonthDay			Yes				Yes				Month and day
 * 					gDay				Yes				Yes				Day only
 * 					gMonth				Yes				Yes				Month only
 * Binary			base64Binary		Yes				Yes				Base64 encoded binary
 * 					hexBinary			Yes				Yes				Hexadecimal binary
 * URI				anyURI				Yes				Yes				RFC 3986 URI
 * QName/Notation	QName				Yes				Yes				XML qualified name
 * 					NOTATION			Yes				Yes				Legacy from DTDs
 * </pre>
 * 

 * 
 * <pre>
 * XSD 1.0 and 1.1 Derived Types:
 * 
 * Category			Primitive Type		In XSD 1.0		In XSD 1.1		Notes
 * --------			--------------		----------		----------		-----
 * String			normalizedString	Yes				Yes				Removes line breaks
 * String			token				Yes				Yes				Normalized string without extra spaces
 * String			language			Yes				Yes				Language identifier per RFC 3066
 * String			Name				Yes				Yes				Valid XML name
 * String			NCName				Yes				Yes				Name without colon
 * String			ID					Yes				Yes				Unique XML identifier
 * String			IDREF				Yes				Yes				Reference to ID
 * String			IDREFS				Yes				Yes				List of IDREF
 * String			ENTITY				Yes				Yes				External entity reference
 * String			ENTITIES			Yes				Yes				List of ENTITY
 * String			NMTOKEN				Yes				Yes				Name token
 * String			NMTOKENS			Yes				Yes				List of NMTOKENs
 * Decimal			integer				Yes				Yes				Whole number
 * Decimal			nonPositiveInteger	Yes				Yes				<= 0
 * Decimal			negativeInteger		Yes				Yes				< 0
 * Decimal			long				Yes				Yes				64-bit signed
 * Decimal			int					Yes				Yes				32-bit signed
 * Decimal			short				Yes				Yes				16-bit signed
 * Decimal			byte				Yes				Yes				8-bit signed
 * Decimal			nonNegativeInteger	Yes				Yes				>= 0
 * Decimal			unsignedLong		Yes				Yes				64-bit unsigned
 * Decimal			unsignedInt			Yes				Yes				32-bit unsigned
 * Decimal			unsignedShort		Yes				Yes				16-bit unsigned
 * Decimal			unsignedByte		Yes				Yes				8-bit unsigned
 * Decimal			positiveInteger		Yes				Yes				> 0
 * Decimal			precisionDecimal	No				Yes				Derived for arbitrary-precision decimals; supersedes decimal.
 * Date/Time		dateTimeStamp		No				Yes				Derived from dateTime with required timezone.
 * </pre>
 */
public final class XSDTypeUtils {

	/**
	 * Select XSD datatypes for UML attributes.
	 * 
	 * @param l A simple name for the datatype received from the UML.
	 * @return A resource representing one of the XSD datatypes recommended for OWL.
	 */
	public static FrontsNode selectXSDType(String l) {
		// TODO: add more XSD datatypes here
		if (l.equalsIgnoreCase("integer"))
			return XSD.integer;
		else if (l.equalsIgnoreCase("int"))
			return XSD.xint;
		else if (l.equalsIgnoreCase("unsigned"))
			return XSD.unsignedInt;
		else if (l.equalsIgnoreCase("ulong") || l.equalsIgnoreCase("ulonglong"))
			return XSD.unsignedLong;
		else if (l.equalsIgnoreCase("short"))
			return XSD.xshort;
		else if (l.equalsIgnoreCase("long") || l.equalsIgnoreCase("longlong"))
			return XSD.xlong;
		else if (l.equalsIgnoreCase("string") || l.equalsIgnoreCase("char"))
			return XSD.xstring;
		else if (l.equalsIgnoreCase("float"))
			return XSD.xfloat;
		else if (l.equalsIgnoreCase("double") || l.equalsIgnoreCase("longdouble"))
			return XSD.xdouble;
		else if (l.equalsIgnoreCase("boolean") || l.equalsIgnoreCase("bool"))
			return XSD.xboolean;
		else if (l.equalsIgnoreCase("decimal"))
			return XSD.decimal;
		else if (l.equalsIgnoreCase("nonNegativeInteger"))
			return XSD.nonNegativeInteger;
		else if (l.equalsIgnoreCase("date"))
			return XSD.date;
		else if (l.equalsIgnoreCase("time"))
			return XSD.time;
		else if (l.equalsIgnoreCase("datetime"))
			return XSD.dateTime;
		else if (l.equalsIgnoreCase("absolutedatetime"))
			return XSD.dateTime;
		else if (l.equalsIgnoreCase("duration"))
			return XSD.duration;
		else if (l.equalsIgnoreCase("monthday"))
			return XSD.gMonthDay;
		else if (l.equalsIgnoreCase("uri"))
			return XSD.anyURI;
		/**
		 * Below reflects the introduction of the IRI primitive domain type in CIM18.
		 * 
		 * 1. IRI in XSD 1.0
		 * 
		 * XSD 1.0 does not define a separate type for IRIs.
		 * 
		 * However, IRIs can be used with anyURI, but they must be converted to a valid
		 * URI using percent-encoding.
		 * 
		 * This is because anyURI follows RFC 2396 (URIs), which only allows ASCII
		 * characters.
		 * 
		 * 2. XSD 1.1 and IRI Support
		 * 
		 * In XSD 1.1, anyURI aligns with IRI (RFC 3987), allowing Unicode characters.
		 * 
		 * This means in XSD 1.1, anyURI can accept both URIs and IRIs, making explicit
		 * IRI support unnecessary.
		 * 
		 * Here is a basic XSD 1.0 example validating IRI values using xs:string with a pattern:
		 * 
		 * <pre>
		 * <xs:element name="resourceIRI" type="iriType"/>
		 * 
		 * <xs:simpleType name="iriType">
		 *    <xs:restriction base="xxs:string">
		 *       <!-- Basic pattern matching IRI-style strings (e.g., http/https) -->
		 *       xs:pattern value="https?://.+"/>
		 *    </xs:restriction>
		 * </xs:simpleType>
		 * </pre>
		 * 
		 * Here is a basic XSD 1.1 example validating IRI values using xs:anyURI with a pattern:
		 * 
		 * <pre>
		 * <xs:element name="resourceIRI" type="iriType"/>
		 * 
		 * <xs:simpleType name="iriType">
		 *    <xs:restriction base="xs:anyURI">
		 *       <!-- Optional: Basic IRI pattern using Unicode ranges -->
		 *       <xs:pattern value="[\i-[:]][\c-[:]]*"/>
		 *    </xs:restriction>
		 * </xs:simpleType>
		 * </pre>
		 */
		else if (l.equalsIgnoreCase("iri"))
			// For XSD 1.0 - for now treated as a string (apply facet
			return XSD.xstring;
			// For future XSD 1.1 where anyURI was loosened to accept Unicode characters
			//return XSD.anyURI; 
		
		/**
		 * Below reflects the introduction of the UUID primitive domain type in CIM18.
		 * 
		 * We are using XSD.xstring and not XSD:ID datatype here. This is due to the
		 * fact that xsd:ID in XML Schema Definition (XSD) is not suitable for
		 * representing a UUID. Here's why:
		 * 
		 * Purpose and Constraints:
		 * 
		 * - xsd:ID is intended to represent a unique identifier within an XML document.
		 * It must be unique within the document and is generally used for linking with
		 * xsd:IDREF.
		 * 
		 * - xsd:ID must follow the rules for XML names, meaning it must start with a
		 * letter or underscore and cannot contain certain characters, such as hyphens
		 * (-) or numbers at the beginning. This makes it incompatible with the typical
		 * structure of UUIDs, which are typically in the form 8-4-4-4-12 hexadecimal
		 * digits separated by hyphens.
		 * 
		 * UUID Format:
		 * 
		 * - A UUID (Universally Unique Identifier) is typically represented as a
		 * 36-character string, including 32 hexadecimal digits and 4 hyphens. This
		 * format does not conform to the XML name rules required by xsd:ID.
		 * 
		 * Alternative:
		 * 
		 * - Instead of using xsd:ID, we need to use xsd:string with a pattern (i.e.
		 * facet) or define a custom type with a pattern that matches the UUID format
		 * such as in this example:
		 * 
		 * <pre>
		 * <xs:simpleType name="UUID">
		 *    <xs:restriction base="xs:string">
		 *       <xs:pattern value="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"/>
		 *    </xs:restriction> 
		 * </xs:simpleType>
		 * </pre>
		 * 
		 * or in OWL or OWL2 (it is compatible with both):
		 * 
		 * <pre>
		 * @prefix : <http://example.org#> .
		 * @prefix owl: <http://www.w3.org/2002/07/owl#> .
		 * @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
		 * @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
		 * 
		 * :UUID a rdfs:Datatype ;
		 *     owl:equivalentClass [
		 *         a rdfs:Datatype ;
		 *         owl:onDatatype xsd:string ;
		 *         owl:withRestrictions ( [ xsd:pattern "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" ] )
		 *     ] .
		 * </pre>
		 */
		//else if (l.equalsIgnoreCase("uuid"))
		//	return XSD.xstring;
		else
			return null;
	}

}
