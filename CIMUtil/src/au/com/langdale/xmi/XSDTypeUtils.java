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
		else if (l.equalsIgnoreCase("uri"))
			return XSD.anyURI;
		/**
		 * Below reflects the introduction of the URI primitive domain type in CIM18.
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
		 */
		// else if (l.equalsIgnoreCase("iri"))
		//	return XSD.anyURI;
		else
			return null;
	}

}
