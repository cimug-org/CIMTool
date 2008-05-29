/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
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
				Individual pack = (Individual) nt.nextResource().as(Individual.class);
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
				Individual pack = (Individual) ot.next();
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
		Iterator jt = model.listSubjectsWithProperty(RDF.type);
		Set typed = new HashSet();
		while(jt.hasNext())
			typed.add(jt.next());
		
		// find everything else and call it untyped
		Set untyped = new HashSet();
		Iterator mt = model.listObjects();
		while(mt.hasNext()) {
			RDFNode obj = (RDFNode) mt.next();
			if(obj instanceof Resource) {
				if( !typed.contains(obj))
					untyped.add(obj);
			}
		}
	
		Iterator kt = model.listSubjects();
		while(kt.hasNext()) {
			Resource subj = (Resource) kt.next();
			if( ! typed.contains(subj))
				untyped.add(subj);
				
		}
		
		// remove untyped nodes
		Iterator lt = untyped.iterator();
		while(lt.hasNext()) {
			Resource subj = (Resource) lt.next();
			OntResource res = (OntResource) subj.as(OntResource.class);
			
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
			Resource attrib = it.nextResource();
			Statement s = attrib.getProperty(RDFS.range);
			if( s != null ) {
				Resource type = s.getResource();
				//attrib.removeAll(RDF.type);
				if( (type.hasProperty(RDF.type, RDFS.Datatype) 
						|| type.getNameSpace().equals(XSD.getURI())) 
							&& ! type.hasProperty(UML.hasStereotype, UML.enumeration))
					attrib.addProperty( RDF.type, OWL.DatatypeProperty);
				else
					attrib.addProperty( RDF.type, OWL.ObjectProperty);
				attrib.addProperty( RDF.type, OWL.FunctionalProperty);
			}
		}
	}

}
