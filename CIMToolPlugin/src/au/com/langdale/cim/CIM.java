/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cim;

import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;


/**
 * Namespace for the RDF representation of the CIM. 
 * 
 */

public class CIM {

	public final static String NS = "http://iec.ch/TC57/CIM-generic#";
	public final static Resource Domain = ResourceFactory.createResource(NS + "Domain");

}
