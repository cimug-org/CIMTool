/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

import com.hp.hpl.jena.vocabulary.RDF;
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
	private boolean uniqueNamespaces; // carries the "Add package names to default schema namespace" preference
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
					b = stereotypedNamespaces.getNamespace(aPackage);
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
			// "Fall through" processing when no namespace is found on the resource or
			// any of its ancestor packages. The model is untranslated here, so the
			// namespace Translator assigns to the package containing the resource is
			// computed from its package path rather than taken from its internal URI.
			// A package itself, like the global package, is based on the schema
			// namespace (issue #385).
			if (uniqueNamespaces && !resource.hasProperty(RDF.type, UML.Package) && !p.equals(UML.global_package))
				if (extraDecoration)
					return Translator.packageNamespace(defaultNamespace, "/Global" + Translator.pathName(p));
				else
					return Translator.packageNamespace(defaultNamespace, Translator.pathName(p));
		}

		return defaultNamespace;
	}
}
