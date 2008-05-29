/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.HashSet;
import java.util.Iterator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * Utility to map resources in one model to another based on their type and local name.
 */
public class NSMapper {
	private Model model;
	private HashSet spaces;

	/**
	 * 
	 * @param model: the model containing the target resources.
	 */
	public NSMapper(Model model) {
		this.model = model;
		init();
	}
	
	private void init() {
		spaces = new HashSet();
		StmtIterator it = model.listStatements(null, RDF.type, (RDFNode)null);
		while( it.hasNext()) {
			Resource subject = it.nextStatement().getSubject();
			if( subject.isURIResource())
				spaces.add(subject.getNameSpace());
		}
	}
	
	/**
	 * Map an original resource to a target of the given type in the target model
	 * and the same local name but a possibly different namespace.
	 * 
	 * @param original resource to be mapped
	 * @param type: the type of the target resource 
	 * @return the target resource or <code>null</code> if there is none
	 */
	public Resource map(Resource original, Resource type) {
		if( model.contains(original, RDF.type, type))
			return original;
		
		if( original.isURIResource()) 
			return map(original.getLocalName(), type);

		return null;
	}
	/**
	 * Find a target resource with the given local name and type.
	 * @param name: the local name of the target
	 * @param type: the type of the target
	 * @return thetarget resource or <code>null</code> is there is none
	 */
	public Resource map(String name, Resource type) {
		for (Iterator it = spaces.iterator(); it.hasNext();) {
			String ns = (String) it.next();
			Resource cand = ResourceFactory.createResource(ns + name);
			if( model.contains(cand, RDF.type, type))
				return cand;
		}
		return null;
	}
}
