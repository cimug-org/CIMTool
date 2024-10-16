/**
 * 
 */
package au.com.langdale.xmi;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * 
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
		/**
		 * Below reflects the introduction of the URI primitive domain type in CIM18.
		 */
		else if (l.equalsIgnoreCase("uri"))
			return XSD.anyURI;
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
		 * 
		 * <xs:simpleType name="UUID"> <xs:restriction base="xs:string">
		 * <xs:pattern value=
		 * "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"/>
		 * </xs:restriction> </xs:simpleType>
		 */
		else if (l.equalsIgnoreCase("uuid"))
			return XSD.xstring;
		else
			return null;
	}
	
}
