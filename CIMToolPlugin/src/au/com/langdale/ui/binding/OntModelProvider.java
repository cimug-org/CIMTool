/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
/**
 * A reference to a an ontology model and a resource in that model.
 */
public interface OntModelProvider {
	public OntModel getModel();
	public OntResource getSubject();
}
