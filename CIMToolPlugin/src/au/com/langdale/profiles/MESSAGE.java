package au.com.langdale.profiles;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * The Abstract Message Definition vocabulary.. 
 * 
 */
public class MESSAGE {

	public final static String NS = "http://langdale.com.au/2005/Message#";
	public final static Resource Message = ResourceFactory.createResource(NS + "Message");	
	public final static Resource Flag = ResourceFactory.createResource(NS + "Flag");
	public final static Property about = ResourceFactory.createProperty(NS + "about");
	public final static Resource Reference = ResourceFactory.createResource(NS + "Reference");
	
	public static void loadOntology( OntModel model ) {
		model.createClass(Message.getURI());
		model.createClass(Reference.getURI());
		model.createClass(Flag.getURI());
		model.createObjectProperty(about.getURI());
	}
}
