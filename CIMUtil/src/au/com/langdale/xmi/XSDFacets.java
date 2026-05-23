/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResourceFactory;

/**
 * Class to represent XSD facets in OWL ontologies.
 */

public class XSDFacets {

	public final static String NS = "http://www.w3.org/2001/XMLSchema#";

	// Properties associated with supported XSD facets
	public final static Property length = ResourceFactory.createProperty(NS + "length");
	public final static Property minLength = ResourceFactory.createProperty(NS + "minLength");
	public final static Property maxLength = ResourceFactory.createProperty(NS + "maxLength");
	public final static Property minInclusive = ResourceFactory.createProperty(NS + "minInclusive");
	public final static Property maxInclusive = ResourceFactory.createProperty(NS + "maxInclusive");
	public final static Property minExclusive = ResourceFactory.createProperty(NS + "minExclusive");
	public final static Property maxExclusive = ResourceFactory.createProperty(NS + "maxExclusive");
	public final static Property whiteSpace = ResourceFactory.createProperty(NS + "whiteSpace");
	public final static Property pattern = ResourceFactory.createProperty(NS + "pattern");
	public final static Property enumeration = ResourceFactory.createProperty(NS + "enumeration");
	public final static Property totalDigits = ResourceFactory.createProperty(NS + "totalDigits");
	public final static Property fractionDigits = ResourceFactory.createProperty(NS + "fractionDigits");

}
