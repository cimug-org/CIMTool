package au.com.langdale.cimtoole.project;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.cim.CIMS;
import au.com.langdale.xmi.UML;

public class PrettyTypes {

	static final Object PRETTY_TYPES = new Resource[] {
		OWL.Class,
		OWL.FunctionalProperty,
		OWL.ObjectProperty,
		OWL.DatatypeProperty,
		RDF.Property,
		RDFS.Class,
		RDFS.Datatype,
		ResourceFactory.createProperty(UML.Package.getURI()),
		ResourceFactory.createProperty(CIMS.ClassCategory.getURI())
	};

}
