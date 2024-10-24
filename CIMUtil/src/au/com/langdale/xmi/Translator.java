/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.util.Iterator;

import org.apache.xerces.util.XMLChar;

import com.hp.hpl.jena.graph.FrontsNode;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.ResIterator;

/**
 * A translator which produces a OWL model with CIM/XML standard naming from a
 * OWL model derived from CIM UML.
 */
public class Translator implements Runnable {

	private OntModel model;
	private OntModel result;
	private String defaultNamespace;
	private boolean extraDecoration;
	private boolean uniqueNamespaces;

	/**
	 * Construct from input model and the namespace for renamed resources.
	 * 
	 * @param input the model to be translated.
	 */
	public Translator(OntModel model, String namespace, boolean usePackageNames) {
		this.model = model;
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

		System.out.println("Stage 2 XMI model size: " + getModel().size());

		propagateAnnotation(UML.baseuri);
		pass2.run();
	}

	private abstract class Pass implements Runnable {
		protected abstract FrontsNode renameResource(OntResource r, String l);

		/**
		 * Pass over every statement and apply renameResource() to each resource.
		 */
		public void run() {
			result = ModelFactory.createMem();
			Iterator it = model.getGraph().find(Node.ANY, Node.ANY, Node.ANY);
			while (it.hasNext()) {
				Triple s = (Triple) it.next();
				add(renameResource(model.createResource(s.getSubject())),
						renameResource(model.createResource(s.getPredicate())), renameObject(s.getObject()));
			}
			model = result;
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
				if (uniqueNamespaces) {
					if (extraDecoration)
						return result.createResource(
								stripHash(defaultNamespace) + "/Global" + pathName(r) + "#Package_" + l);
					else
						return result.createResource(stripHash(defaultNamespace) + prefixPath(r) + "#" + l);
				} else
					return result.createResource(defaultNamespace + "Package_" + l);
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
	 * Determine whether we are interested in a UML tag of given name and return an
	 * annotation resource for it.
	 */
	public static FrontsNode annotationResource(String l) {
		if (l.equals("documentation") || l.equals("description"))
			return RDFS.comment;
		else if (l.equals(UML.baseuri.getLocalName()))
			return UML.baseuri;
		else if (l.equals(UML.baseprefix.getLocalName()))
			return UML.baseprefix;
		else if (powercc) {
			if (l.equals("RationalRose$PowerCC:RdfRoleA"))
				return UML.roleALabel;
			else if (l.equals("RationalRose$PowerCC:RdfRoleB"))
				return UML.roleBLabel;
			else if (l.equals("RationalRose$PowerCC:Namespace"))
				return UML.baseprefix;
		}
		return null; // omit other annotations eg 'transient' defined in the UML
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
						|| r.hasProperty(UML.hasStereotype, UML.primitive))
						&& !r.hasProperty(UML.hasStereotype, UML.enumeration)) {
					FrontsNode x = XSDTypeUtils.selectXSDType(l);
					if (x != null) {
						OntResource resource = result.createResource(x.toString());
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

			// System.out.println("Unrecognised UML element name: " + l + " uri: " +
			// r.getURI());
			// this is almost certainly a top level datatype declaration
			FrontsNode x = XSDTypeUtils.selectXSDType(l);
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
	 * Discover the base URI, if given, for a model element.
	 * 
	 * @param r an untranslated resource
	 * @return a URI
	 */
	protected String findBaseURI(OntResource r) {
		String b = r.getString(UML.baseuri);
		if (b != null)
			return b;

		String x = r.getString(UML.baseprefix);
		if (x != null) {
			ResIterator it = model.listSubjectsWithProperty(UML.uriHasPrefix, x);
			if (it.hasNext()) {
				b = it.nextResource().getURI();
				if (!b.contains("#"))
					b += "#";
				return b;
			}
		}

		OntResource p = r.getResource(RDFS.isDefinedBy);
		if (p != null) {
			b = p.getString(UML.baseuri);
			if (b != null)
				return b;
			if (uniqueNamespaces)
				if (extraDecoration)
					return p.getNameSpace();
				else
					return stripHash(p.getNameSpace()) + "/" + p.getLocalName() + "#";
		}

		return defaultNamespace;
	}

	/**
	 * Propagate a given annotation property to descendents.
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
		String v = p.getString(a);
		if (v != null) {
			return v;
		}

		OntResource s = p.getResource(RDFS.isDefinedBy);
		if (s != null) {
			v = propagateAnnotation(s, a);
			if (v != null) {
				p.addProperty(a, v);
				return v;
			}
		}

		return null;
	}
}
