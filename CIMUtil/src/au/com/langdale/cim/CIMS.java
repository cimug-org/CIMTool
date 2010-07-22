/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cim;

import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

/**
 * This class represents a vocabulary defined by the IEC 61970 standard that extends RDFS.  
 */
public class CIMS {
	public final static String NS = "http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#";
	public final static Property inverseRoleName = ResourceFactory.createProperty(NS + "inverseRoleName");
	public final static Property dataType = ResourceFactory.createProperty(NS + "dataType");
	public final static Property multiplicity = ResourceFactory.createProperty(NS + "multiplicity");
	public final static Property belongsToCategory = ResourceFactory.createProperty(NS + "belongsToCategory");
	public final static Property stereotype = ResourceFactory.createProperty(NS + "stereotype");;
	public final static Resource M0_1 = ResourceFactory.createResource(NS + "M:0..1");
	public final static Resource M1 = ResourceFactory.createResource(NS + "M:1");
	public final static Resource M0_n = ResourceFactory.createResource(NS + "M:0..n");
	public final static Resource M1_n = ResourceFactory.createResource(NS + "M:1..n");
	public final static Resource ClassCategory = ResourceFactory.createResource(NS + "ClassCategory");
}
