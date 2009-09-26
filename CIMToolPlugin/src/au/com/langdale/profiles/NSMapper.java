/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * Utility to map resources in one model to another based on their type and local name.
 */
public class NSMapper {
	private OntModel model;
	private Set spaces;

	/**
	 * 
	 * @param model: the model containing the target resources.
	 */
	public NSMapper(OntModel model) {
		this.model = model;
		init();
	}
	
	private void init() { spaces = extractNamespaces(model); }
	
	/**
	 * Map an original resource to a target of the given type in the target model
	 * and the same local name but a possibly different namespace.
	 * 
	 * @param original resource to be mapped
	 * @param type: the type of the target resource 
	 * @return the target resource or <code>null</code> if there is none
	 */
	public Resource map(Resource original, FrontsNode type) {
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
	 * @return the target resource or <code>null</code> is there is none
	 */
	public Resource map(String name, FrontsNode type) { return resourceWithLocalName(name, type, spaces, model); }

	/**
	 * Find all namespaces used for typed, URI resource in a model.
	 */
	public static Set extractNamespaces(OntModel model) {
		Set spaces = new HashSet();
		ResIterator it = model.listSubjectsWithProperty(RDF.type);
		while( it.hasNext()) {
			Resource subject = it.nextResource();
			if( subject.isURIResource())
				spaces.add(subject.getNameSpace());
		}
		return spaces;
	}

	/**
	 * Find a target resource with the given local name and type.
	 * @param name: the local name of the target
	 * @param type: the type of the target
	 * @param spaces: the namespaces to consider
	 * @param model: the model in which to check types
	 * @return the target resource or <code>null</code> is there is none
	 */
	public static Resource resourceWithLocalName(String name, FrontsNode type, Set spaces, OntModel model) {
		for (Iterator it = spaces.iterator(); it.hasNext();) {
			String ns = (String) it.next();
			Resource cand = ResourceFactory.createResource(ns + name);
			if( model.contains(cand, RDF.type, type))
				return cand;
		}
		return null;
	}
}
