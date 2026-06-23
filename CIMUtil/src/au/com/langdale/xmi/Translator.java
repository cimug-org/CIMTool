/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.Resource;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.xerces.util.XMLChar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * A translator which produces a OWL model with CIM/XML standard naming from a
 * OWL model derived from CIM UML.
 */
public class Translator implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(Translator.class);

	private OntModel model;
	private OntModel result;
	private String defaultNamespace;
	private boolean extraDecoration;
	private boolean uniqueNamespaces;
	private StereotypedNamespaces stereotypedNamespaces;

	/**
	 * Construct from input model and the namespace for renamed resources.
	 * 
	 * @param stereotypedNamespaces
	 * 
	 * @param input                 the model to be translated.
	 */
	public Translator(OntModel model, StereotypedNamespaces stereotypedNamespaces, String namespace,
			boolean usePackageNames) {
		this.model = model;
		this.stereotypedNamespaces = stereotypedNamespaces;
		OntResource ont = model.getValidOntology();
		if (ont != null) {
			defaultNamespace = ont.getURI() + "#";
			uniqueNamespaces = true;
		} else {
			defaultNamespace = namespace;
			uniqueNamespaces = usePackageNames;
			extraDecoration = true;
		}
	}

	/**
	 * @return Returns the result model.
	 */
	public OntModel getModel() {
		return result;
	}

	/**
	 * Apply translation to input model, populating the result model.
	 * 
	 * Each statement in the input model is transcribed to the result model with
	 * some resources substituted.
	 */
	public void run() {
		pass1.run();

		log.debug("Stage 2 XMI model size: {}", getModel().size());

		propagateAnnotation(UML.baseuri);

		pass2.run();
	}

	private abstract class Pass implements Runnable {

		protected Resource baseuri = UML.baseuri; // Default resource
		protected Resource baseprefix = UML.baseprefix; // Default resource
		protected Resource uriHasPrefix = UML.uriHasPrefix; // Default resource

		protected abstract FrontsNode renameResource(OntResource r, String l);

		protected HashMap<OntResource, String> primitiveToCanonicalCIMTypeMap = new HashMap<OntResource, String>();
		protected HashMap<String, OntResource> canonicalCIMTypeToPrimitiveMap = new HashMap<String, OntResource>();

		/**
		 * Pass over every statement and apply renameResource() to each resource.
		 */
		public void run() {

			result = ModelFactory.createMem();

			preprocess();

			Iterator<Triple> triples = model.getGraph().find(Node.ANY, Node.ANY, Node.ANY);
			while (triples.hasNext()) {
				Triple triple = (Triple) triples.next();
				//
				OntResource subject = model.createResource(triple.getSubject());
				OntResource predicate = model.createResource(triple.getPredicate());
				Node object = triple.getObject();
				//
				FrontsNode renamedSubject = renameResource(subject);
				FrontsNode renamedPredicate = renameResource(predicate);
				Node renamedObject = renameObject(object);
				//
				add(renamedSubject, renamedPredicate, renamedObject);
			}

			model = result;
		}

		/**
		 * Default no-op implementation. Should be overridden if preprocessing is
		 * needed.
		 */
		protected void preprocess() {
		}

		/**
		 * Discover the base URI, if given, for a model element.
		 * 
		 * @param r an untranslated resource
		 * @return a URI
		 */
		protected String findBaseURI(OntResource r) {
			String b = null;
			if (stereotypedNamespaces.hasNamespaces()) {
				b = stereotypedNamespaces.getNamespace(r);
			} else {
				b = r.getString(baseuri);
			}

			if (b != null) {
				return b;
			}

			// If the resource itself does not have a baseuri we then check the domain
			if (r.getDomain() != null) {
				if (stereotypedNamespaces.hasNamespaces()) {
					b = stereotypedNamespaces.getNamespace(r.getDomain());
				} else {
					b = r.getDomain().getString(baseuri);
				}
				if (b != null) {
					return b;
				}
			}

			// This additional code is only relevant for when baseuri/baseprefix
			// tag values are used to define namespaces. Here we check to ensure
			// that no namespaces have been loaded from a *.namespaces mapping file.
			// If not then we need to do the additional check for a baseprefix tag
			// value...
			if (!stereotypedNamespaces.hasNamespaces()) {
				String x = r.getString(baseprefix);
				if (x != null) {
					ResIterator it = model.listSubjectsWithProperty(uriHasPrefix, x);
					if (it.hasNext()) {
						b = it.nextResource().getURI();
						if (!b.contains("#"))
							b += "#";
						return b;
					}
				}
			}

			OntResource p = r.getResource(RDFS.isDefinedBy);
			if (p != null) {
				// Here we navigate up the package hierarchy checking for a namespace (either
				// via a stereotype or baseuri tag value - whichever mode is in play)
				OntResource aPackage = p;
				while (aPackage != null) {
					if (stereotypedNamespaces.hasNamespaces()) {
						b = stereotypedNamespaces.getNamespace(p);
					} else {
						b = aPackage.getString(baseuri);
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

		/**
		 * Substitute a single node.
		 * 
		 * If the node is a resource then it is substituted as required. If the node is
		 * a literal it is preserved.
		 */
		protected Node renameObject(Node n) {
			if (n.isLiteral()) {
				return n;
			} else {
				FrontsNode r = renameResource(model.createResource(n));
				return r == null ? null : r.asNode();
			}
		}

		/**
		 * Substitute a a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result or null if the resource
		 *         should not appear in the result.
		 */
		protected FrontsNode renameResource(OntResource r) {
			if (r.isAnon())
				return r;

			if (!r.getNameSpace().equals(XMI.NS))
				return r;

			String ls = r.getString(RDFS.label);
			if (ls == null)
				return r;

			String l = escapeNCName(ls);
			if (l.length() == 0)
				return null;

			return renameResource(r, l);
		}
	}

	/**
	 * This pass translates annotations and stereotypes to resources in the UML
	 * namespace and packages to resources in the defaultNamespace.
	 * 
	 * These are then available in later passes.
	 */
	Runnable pass1 = new Pass() {

		/**
		 * During pass 1 we must preprocess baseuri, baseprefix, and hasBasePrefix
		 * annotations as they are used in the findBaseURI() method. Without this step
		 * packages are not mapped to their proper namespaces and extension packages
		 * will be incorrectly assigned the default CIM namespaces. This is because at
		 * this stage in translation the baseuri annotation is still only identifiable
		 * by its EAID and not yet by its UML.baseuri property.
		 */
		protected void preprocess() {
			Iterator triples = model.getGraph().find(Node.ANY, RDF.type.asNode(), OWL.AnnotationProperty.asNode());
			while (triples.hasNext()) {
				Triple triple = (Triple) triples.next();
				//
				OntResource subject = model.createResource(triple.getSubject());
				if (subject.getLabel() != null) {
					if (subject.getLabel().equals(UML.baseuri.getLocalName())) {
						baseuri = subject;
					} else if (subject.getLabel().equals(UML.baseprefix.getLocalName())) {
						baseprefix = subject;
					} else if (subject.getLabel().equals(UML.uriHasPrefix.getLocalName())) {
						uriHasPrefix = subject;
					}
				}
			}
		}

		/**
		 * Substitute a a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result or null if the resource
		 *         should not appear in the result.
		 */
		@Override
		protected FrontsNode renameResource(OntResource r, String l) {
			if (r.hasProperty(RDF.type, UML.Stereotype)) {
				OntResource stereotype = result.createResource(UML.NS + l.toLowerCase());
				stereotype.addLabel(l, null);
				return stereotype;
			} else if (r.hasProperty(RDF.type, OWL.AnnotationProperty)) {
				return annotationResource(l);
			} else if (r.hasProperty(RDF.type, UML.Package)) {
				// there are three strategies evolved over time to assign package URI's
				String packageNamespace = findBaseURI(r);
				if (uniqueNamespaces) {
					if (extraDecoration)
						return result.createResource(
								stripHash(packageNamespace) + "/Global" + pathName(r) + "#Package_" + l);
					else
						return result.createResource(stripHash(packageNamespace) + prefixPath(r) + "#" + l);
				} else
					return result.createResource(packageNamespace + "Package_" + l);
			} else
				return r;
		}

	};

	public static String stripHash(String uri) {
		while (uri.endsWith("#") || uri.endsWith("/"))
			uri = uri.substring(0, uri.length() - 1);
		return uri;
	}

	public static boolean powercc = true;;

	/**
	 * Determine whether we are interested in a UML tag of the given name and return
	 * an annotation resource for it if so.
	 */
	public static FrontsNode annotationResource(String tag) {
		if (tag.equals("documentation") || tag.equals("description"))
			return RDFS.comment;
		else if (tag.equals(UML.baseuri.getLocalName()))
			return UML.baseuri;
		else if (tag.equals(UML.baseprefix.getLocalName()))
			return UML.baseprefix;
		else if (tag.equals(UML.baseType.getLocalName()))
			return UML.baseType;
		else if (tag.equals(XSDFacets.length.getLocalName()))
			return XSDFacets.length;
		else if (tag.equals(XSDFacets.minLength.getLocalName()))
			return XSDFacets.minLength;
		else if (tag.equals(XSDFacets.maxLength.getLocalName()))
			return XSDFacets.maxLength;
		else if (tag.equals(XSDFacets.minInclusive.getLocalName()))
			return XSDFacets.minInclusive;
		else if (tag.equals(XSDFacets.maxInclusive.getLocalName()))
			return XSDFacets.maxInclusive;
		else if (tag.equals(XSDFacets.minExclusive.getLocalName()))
			return XSDFacets.minExclusive;
		else if (tag.equals(XSDFacets.maxExclusive.getLocalName()))
			return XSDFacets.maxExclusive;
		else if (tag.equals(XSDFacets.whiteSpace.getLocalName()))
			return XSDFacets.whiteSpace;
		else if (tag.equals(XSDFacets.pattern.getLocalName()))
			return XSDFacets.pattern;
		else if (tag.equals(XSDFacets.enumeration.getLocalName()))
			return XSDFacets.enumeration;
		else if (tag.equals(XSDFacets.totalDigits.getLocalName()))
			return XSDFacets.totalDigits;
		else if (tag.equals(XSDFacets.fractionDigits.getLocalName()))
			return XSDFacets.fractionDigits;
		else if (powercc) {
			if (tag.equals("RationalRose$PowerCC:RdfRoleA"))
				return UML.roleALabel;
			else if (tag.equals("RationalRose$PowerCC:RdfRoleB"))
				return UML.roleBLabel;
			else if (tag.equals("RationalRose$PowerCC:Namespace"))
				return UML.baseprefix;
		}
		return null; // omit other annotations eg 'transient' defined in the UML
	}

	/**
	 * Set's an appropriate tag value.
	 */
	public static void addTagValue(OntResource subject, FrontsNode tag, String value) {
		// XSD facet tag values...
		if (tag.equals(UML.baseType.getLocalName())) {
			OntModel model = subject.getOntModel();
			OntResource uri = model.createResource(XSD.xstring.toString());
			subject.addProperty(tag, uri);
		} else if (tag.equals(XSDFacets.length.getLocalName()) || tag.equals(XSDFacets.minLength.getLocalName())
				|| tag.equals(XSDFacets.maxLength.getLocalName()) || tag.equals(XSDFacets.minInclusive.getLocalName())
				|| tag.equals(XSDFacets.maxInclusive.getLocalName())
				|| tag.equals(XSDFacets.minExclusive.getLocalName())
				|| tag.equals(XSDFacets.maxExclusive.getLocalName()) || tag.equals(XSDFacets.totalDigits.getLocalName())
				|| tag.equals(XSDFacets.fractionDigits.getLocalName())) {
			subject.addProperty(tag, Node.createLiteral(LiteralLabelFactory.create(Integer.valueOf(value))));
		} else {
			subject.addProperty(tag, value);
		}
	}

	private static String pathName(OntResource r) {
		String prefix = prefixPath(r);
		String ls = r.getString(RDFS.label);
		if (ls == null)
			return prefix;

		String l = escapeNCName(ls);
		if (l.length() == 0)
			return prefix;

		return prefix + "/" + l;

	}

	private static String prefixPath(OntResource r) {
		OntResource ps = r.getResource(RDFS.isDefinedBy);
		if (ps != null && !ps.equals(UML.global_package)) {
			return pathName(ps);
		} else {
			return "";
		}
	}

	/**
	 * This pass translates anything found with a label and an XMI namespace, which
	 * would exclude resources translated in earlier passes. Special naming rules
	 * apply to classes, properties and enumeration members.
	 */
	Runnable pass2 = new Pass() {

		@Override
		public void run() {

			super.run();

			/**
			 * Once all renaming has completed for pass 2, the final step is to add the
			 * UML.cimdatatypeMapping to each of the primitives. This is executed after
			 * rename processing
			 */
			ResIterator it = result.listSubjectsBuffered(UML.hasStereotype, UML.primitive);
			while (it.hasNext()) {
				OntResource primitive = it.nextResource();
				String cimdatatypeMapping = primitiveToCanonicalCIMTypeMap.get(primitive);
				if (cimdatatypeMapping != null)
					primitive.addProperty(UML.cimdatatypeMapping, cimdatatypeMapping);
			}

			it = result.listSubjectsBuffered(UML.hasStereotype, UML.constrainedprimitive);
			while (it.hasNext()) {
				OntResource constrainedPrimitive = it.nextResource();
				String cimdatatypeMapping = primitiveToCanonicalCIMTypeMap.get(constrainedPrimitive);
				if (cimdatatypeMapping != null)
					constrainedPrimitive.addProperty(UML.cimdatatypeMapping, cimdatatypeMapping);
			}
		}

		/**
		 * Substitute a a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result or null if the resource
		 *         should not appear in the result.
		 */
		@Override
		protected FrontsNode renameResource(OntResource r, String l) {
			String namespace = findBaseURI(r);

			if (r.hasProperty(RDF.type, OWL.Class)) {
				if ((r.hasProperty(UML.hasStereotype, UML.datatype) || r.hasProperty(UML.hasStereotype, UML.cimdatatype)
						|| r.hasProperty(UML.hasStereotype, UML.primitive)
						|| r.hasProperty(UML.hasStereotype, UML.constrainedprimitive))
						&& !r.hasProperty(UML.hasStereotype, UML.enumeration)) {
					FrontsNode x = XSDTypeUtils.selectXSDType(r, l);
					if (x != null) {
						OntResource resource = result.createResource(x.toString());
						if (r.hasProperty(UML.hasStereotype, UML.primitive)
								|| r.hasProperty(UML.hasStereotype, UML.constrainedprimitive)) {
							if (!primitiveToCanonicalCIMTypeMap.containsKey(resource)) {
								primitiveToCanonicalCIMTypeMap.put(resource, namespace + l);
							}
							if (!canonicalCIMTypeToPrimitiveMap.containsKey(namespace + l)) {
								canonicalCIMTypeToPrimitiveMap.put(namespace + l, resource);
							}
						}
						return resource;
					}
				}
				return result.createResource(namespace + l);
			}

			if (r.hasProperty(RDF.type, OWL.ObjectProperty) || r.hasProperty(RDF.type, OWL.DatatypeProperty)
					|| r.hasProperty(RDF.type, RDF.Property)) {
				OntResource ds = r.getResource(RDFS.domain);
				if (ds != null) {
					String ls = ds.getString(RDFS.label);
					if (ls != null) {
						String c = escapeNCName(ls);
						return result.createResource(namespace + c + "." + l);
					}
				}
				return result.createResource(namespace + "_." + l);
			}

			for (ResIterator it = r.listProperties(RDF.type); it.hasNext();) {
				OntResource ts = it.nextResource();
				if (ts.hasProperty(UML.hasStereotype, UML.enumeration)) {
					String ls = ts.getString(RDFS.label);
					if (ls != null) {
						String c = escapeNCName(ls);
						return result.createResource(namespace + c + "." + l);
					}
				}
			}

			FrontsNode x = XSDTypeUtils.selectXSDType(r, l);
			if (x != null) {
				return x;
			}

			return result.createResource(namespace + l);
		}
	};

	/**
	 * Regenerate a statement in the result model with given subject, predicate and
	 * object.
	 * 
	 * s, p, o are the resources from an input model statement with possible
	 * substitutions. If any are null (no substitution) the original statement is
	 * negated.
	 */
	protected void add(FrontsNode s, FrontsNode p, Node o) {
		if (s != null && p != null && o != null) {
			result.add(s, p, o);
		}
	}

	/**
	 * Convert a string to an NCNAME by substituting underscores for invalid
	 * characters.
	 */
	public static String escapeNCName(String label) {
		StringBuffer result = new StringBuffer(label.trim());
		for (int ix = 0; ix < result.length(); ix++) {
			if (!XMLChar.isNCName(result.charAt(ix)))
				result.setCharAt(ix, '_');
		}
		if (result.length() > 0 && !XMLChar.isNCNameStart(result.charAt(0)))
			result.insert(0, '_');
		return result.toString();
	}
	
	/**
	 * Propagate a given annotation property to descendants.
	 *
	 */
	private void propagateAnnotation(FrontsNode a) {
		ResIterator it = model.listSubjectsWithProperty(RDF.type, UML.Package);
		while (it.hasNext())
			propagateAnnotation(it.nextResource(), a);
	}

	/**
	 * Propagate a given annotation property from p's parent package to p.
	 *
	 */
	private String propagateAnnotation(OntResource p, FrontsNode a) {
		String v = null;

		if (stereotypedNamespaces.hasNamespaces()) {
			v = stereotypedNamespaces.getNamespace(p);
		} else {
			v = p.getString(a);
		}

		if (v != null) {
			return v;
		}

		OntResource s = p.getResource(RDFS.isDefinedBy);
		if (s != null) {
			v = propagateAnnotation(s, a);
			if (v != null) {
				if (stereotypedNamespaces.hasNamespaces()) {
					if (stereotypedNamespaces.hasNamespace(v)) {
						p.addProperty(UML.hasStereotype, stereotypedNamespaces.getStereotypeResource(v));
					}
				} else {
					p.addProperty(a, v);
				}
				return v;
			}
		}

		return null;
	}
}