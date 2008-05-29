/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary for auxiliary terms such as error reports.
 * 
 */

public class LOG {
	public static final String NS = "http://langdale.com.au/2007/log#";
	public static final Resource Problem = ResourceFactory.createResource(NS + "Problem");
	public static final Property hasProblems = ResourceFactory.createProperty(NS + "hasProblem");
	public static final Property problemDetail = ResourceFactory.createProperty(NS + "problemDetail");
	public static final Property problemReference = ResourceFactory.createProperty(NS + "problemReference");
}
