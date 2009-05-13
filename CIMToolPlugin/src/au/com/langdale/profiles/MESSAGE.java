/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import com.hp.hpl.jena.vocabulary.OWL;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

/**
 * The Abstract Message Definition vocabulary.. 
 * 
 */
public class MESSAGE {

	public final static String NS = "http://langdale.com.au/2005/Message#";
	public final static Resource Message = ResourceFactory.createResource(NS + "Message");
	public final static Resource profile = ResourceFactory.createResource(NS + "profile");
	public final static Resource Flag = ResourceFactory.createResource(NS + "Flag");
	public final static Property about = ResourceFactory.createProperty(NS + "about");
	@Deprecated public final static Resource Reference = ResourceFactory.createResource(NS + "Reference");
	
	public static void loadOntology( OntModel model ) {
		model.createClass(Message.getURI());
		model.createIndividual(profile.getURI(), OWL.Ontology);
		// model.createClass(Reference.getURI()); 
		// model.createClass(Flag.getURI()); // TODO: replace with OntologyProperty?
		model.createObjectProperty(about.getURI());
	}
}
