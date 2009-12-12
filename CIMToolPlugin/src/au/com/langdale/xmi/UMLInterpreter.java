/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class UMLInterpreter {

	protected OntModel model;
	/**
	 * Set the Jena OWL model to be interpreted.
	 */
	public void setModel(OntModel model) {
		this.model = model;
	}

	/**
	 * Return the underlying Jena OWL model. 
	 */
	public OntModel getModel() {
		return model;
	}

	/**
	 * Adjust incomplete definitions and 
	 * remove all packages that are empty or contain 
	 * only other packages..
	 */
	public void pruneIncomplete() {
		
//		// assume any untyped range is a Class
//		Iterator it = model.listOntProperties();
//		while(it.hasNext()) {
//			OntProperty prop = (OntProperty) it.next();
//			OntResource range = prop.getRange();
//			if( range != null && ! range.hasProperty(RDF.type)) 
//				model.createClass(range.getURI());
//		}
	
		// repeatedly scan packages
		for(;;) {
			
			// build list of empty packages
			List packages = new ArrayList();
			ResIterator nt = model.listSubjectsWithProperty(RDF.type, UML.Package);
			while(nt.hasNext()) {
				OntResource pack = nt.nextResource();
				if( ! model.listSubjectsWithProperty(RDFS.isDefinedBy, pack).hasNext()) {
					packages.add(pack);
				}	
			}
			
			// no more empty packages
			if(packages.isEmpty())
				break;
			
			// remove each package in this batch
			Iterator ot = packages.iterator();
			while( ot.hasNext()) {
				OntResource pack = (OntResource) ot.next();
				pack.remove();
			}
		}
	}

	/**
	 * Utility to remove any untyped nodes from the model. 
	 *
	 */
	public void removeUntyped() {
		// find every typed node in the model
		ResIterator lt = model.listSubjectsWithNoProperty(RDF.type);
		while(lt.hasNext()) {
			OntResource res = lt.nextResource();
			
			// only the resources originating from UML definitions are affected
			if(res.getNameSpace().equals(XMI.NS)) 
				res.remove();
		}
	}
	
	/**
	 * Find all attributes and convert them to OWL DataTypeProperty or FunctionalProperty
	 * depending on their range.  This utilily must be applied after standard naming 
	 * for some XMI derived models.
	 */
	public void classifyAttributes() {
		ResIterator it = model.listSubjectsWithProperty(UML.hasStereotype, UML.attribute);
		while(it.hasNext()) {
			OntResource attrib = it.nextResource();
			OntResource type = attrib.getResource(RDFS.range);
			if( type != null ) {
				//attrib.removeAll(RDF.type);
				if( (type.hasRDFType(RDFS.Datatype) 
						|| type.getNameSpace().equals(XSD.getURI())) 
							&& ! type.hasProperty(UML.hasStereotype, UML.enumeration))
					attrib.addRDFType( OWL.DatatypeProperty);
				else if( ! type.hasRDFType()) {
					type.addRDFType(RDFS.Datatype);
					attrib.addRDFType( OWL.DatatypeProperty);
					System.out.println("Inferring that " + type + " is a Datatype");
				}
				else
					attrib.addRDFType( OWL.ObjectProperty);
				attrib.addRDFType(OWL.FunctionalProperty);
			}
		}
	}

}
