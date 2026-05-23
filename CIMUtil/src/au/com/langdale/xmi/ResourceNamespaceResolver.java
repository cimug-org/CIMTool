/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * The below namespace resolution code/logic is a mirror of the algorithm within
 * the Translator class. This variant however, traverses up the package
 * hierarchy in search of a namespace. This aspect is different from that needed
 * in Translator.
 */
public class ResourceNamespaceResolver implements NamespaceResolver {

	private OntModel model;
	private String defaultNamespace;
	private boolean extraDecoration;
	private boolean uniqueNamespaces;
	private StereotypedNamespaces stereotypedNamespaces;

	public ResourceNamespaceResolver(OntModel model, StereotypedNamespaces stereotypedNamespaces,
			String cimNormativeBaseURI, boolean usePackageNames) {
		this.model = model;
		this.stereotypedNamespaces = stereotypedNamespaces;
		OntResource ont = model.getValidOntology();
		if (ont != null) {
			defaultNamespace = ont.getURI() + "#";
			uniqueNamespaces = true;
		} else {
			defaultNamespace = cimNormativeBaseURI;
			uniqueNamespaces = usePackageNames;
			extraDecoration = true;
		}
	}

	/**
	 * Discover the base URI, if given, for a model element.
	 * 
	 * @param resource an untranslated resource
	 * @return a URI
	 */
	@Override
	public String findBaseURI(OntResource resource) {
		String b = null;
		if (stereotypedNamespaces.hasNamespaces()) {
			b = stereotypedNamespaces.getNamespace(resource);
		} else {
			b = resource.getString(UML.baseuri);
		}

		if (b != null) {
			return b;
		}

		// If the resource itself does not have a baseuri we then check the domain
		if (resource.getDomain() != null) {
			if (stereotypedNamespaces.hasNamespaces()) {
				b = stereotypedNamespaces.getNamespace(resource.getDomain());
			} else {
				b = resource.getDomain().getString(UML.baseuri);
			}
			if (b != null) {
				return b;
			}
		}

		// This additional code is only relevant for when baseuri/baseprefix
		// tagged values are used to define namespaces. Here we check to
		// ensure that no namespaces have been loaded from a *.namespaces
		// mapping file. If not then we need to do the additional check for
		// a baseprefix tagged value...
		if (!stereotypedNamespaces.hasNamespaces()) {
			String x = resource.getString(UML.baseprefix);
			if (x != null) {
				ResIterator it = model.listSubjectsWithProperty(UML.uriHasPrefix, x);
				if (it.hasNext()) {
					b = it.nextResource().getURI();
					if (!b.contains("#"))
						b += "#";
					return b;
				}
			}
		}

		OntResource p = resource.getResource(RDFS.isDefinedBy);
		if (p != null) {
			// Here we navigate up the package hierarchy checking for a namespace (either
			// via a stereotype or baseuri tagged value - whichever mode is in play)
			OntResource aPackage = p;
			while (aPackage != null) {
				if (stereotypedNamespaces.hasNamespaces()) {
					b = stereotypedNamespaces.getNamespace(p);
				} else {
					b = aPackage.getString(UML.baseuri);
				}
				if (b != null) {
					return b;
				} else {
					if (aPackage.hasProperty(RDFS.isDefinedBy)) {
						aPackage = aPackage.getResource(RDFS.isDefinedBy);
						if (aPackage.equals(UML.global_package))
							aPackage = null;
					} else {
						aPackage = null;
					}
				}
			}
			// "Fall through" processing is to utilize the namespace of the immediate
			// package containing the resource...
			if (uniqueNamespaces)
				if (extraDecoration)
					return p.getNameSpace();
				else
					return stripHash(p.getNameSpace()) + "/" + p.getLocalName() + "#";
		}

		return defaultNamespace;
	}

	private String stripHash(String uri) {
		while (uri.endsWith("#") || uri.endsWith("/"))
			uri = uri.substring(0, uri.length() - 1);
		return uri;
	}

}
