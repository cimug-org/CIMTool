/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;
import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * Utility to map resources in one model to another based on their type and local name.
 */
public class NSMapper {

	private static class Index extends MultiMap {
		public void add(String name, Resource resource) {
			putRaw(name, resource);
		}
	}
	
	private OntModel model;
	private Index index;

	/**
	 * 
	 * @param model: the model containing the target resources.
	 */
	public NSMapper(OntModel model) {
		this.model = model;
		buildIndex();
	}
	
	private void buildIndex() {
		index = new Index();
	
		ResIterator it = model.listSubjectsWithProperty(RDF.type);
		while( it.hasNext()) {
			Resource subject = it.nextResource();
			if( subject.isURIResource()) 
				index.add(subject.getLocalName().toLowerCase(), subject);
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
	public Resource map(Resource original, FrontsNode type) {
		if( model.contains(original, RDF.type, type))
			return original;
		
		if( original.isURIResource()) 
			return map(original.getLocalName(), type);

		return null;
	}
	/**
	 * Find a target resource with the given type
	 * whose local name matches the given name ignoring case.
	 * Prefer a local name that matches exactly.
	 * @param name: the local name of the target
	 * @param type: the type of the target
	 * @return the target resource or <code>null</code> is there is none
	 */
	public Resource map(String name, FrontsNode type) {
		Resource result = null;
		for (Iterator it = index.find(name.toLowerCase()).iterator(); it.hasNext();) {
			Resource cand = (Resource) it.next();
			if( model.contains(cand, RDF.type, type)) {
				if( result == null )
					result = cand;
				if( result.getLocalName().equals(name))
					break;
			}
		}
		return result; 
	}

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
}
