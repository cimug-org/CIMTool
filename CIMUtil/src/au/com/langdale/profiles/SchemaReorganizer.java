/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import au.com.langdale.kena.OntModel;

/**
 * Transform a profile model to conform with CIM/XML RDFS schema rules as defined
 * in IEC 61970-501 Ed 1.0 with the caveat that the min and max cardinalities of
 * properties will be reorganized to confirm to those as defined in the canonical 
 * schema for the project (i.e. the .XMI, .EAP, .QEAP, etc. file) 
 * 
 * In the resulting profile model, each base class or property is represented
 * by at most one profile class or property.
 */
public class SchemaReorganizer extends Reorganizer {

	public SchemaReorganizer(OntModel profile, OntModel background, boolean useRefs) {
		super(profile, background, useRefs);
	}

	@Override
	protected SelectionOptions getSelectionOptions(boolean required) {
		return new SelectionOptions(SelectionOption.UseSchemaCardinality, (required ? SelectionOption.PropertyRequired : SelectionOption.NoOp));
	}

}
