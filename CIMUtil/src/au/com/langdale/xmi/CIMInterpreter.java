/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;

/**
 * Interface for the set of intepreters that extend the generic XMI interpreter
 * by applying IEC CIM modelling conventions.
 *
 */
public interface CIMInterpreter {

	public CIMInterpreterResult interpret( //
			OntModel raw, //
			String baseURI, //
			OntModel annote, //
			boolean usePackageNames, //
			boolean mergeShadowExtensions, //
			boolean validateModel);

}
