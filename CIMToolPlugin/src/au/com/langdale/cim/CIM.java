package au.com.langdale.cim;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


/**
 * Namespace for the RDF representation of the CIM. 
 * 
 */

public class CIM {

	public final static String NS = "http://iec.ch/TC57/CIM-generic#";
	public final static Resource Domain = ResourceFactory.createResource(NS + "Domain");

}
