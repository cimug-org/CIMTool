/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;

public interface EAProjectParser {
	
	public void parse() throws EAProjectParserException;

	public OntModel getModel();
	
}
