/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntResource;

public interface NamespaceResolver {

	/**
	 * Discover the base URI, if given, for a model element.
	 * 
	 * @param resource an untranslated resource
	 * @return a URI
	 */
	String findBaseURI(OntResource resource);

}