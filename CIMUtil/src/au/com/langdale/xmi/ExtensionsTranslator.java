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
 * A translator which produces a new OWL model from an existing OWL model that
 * has already had applied the CIM/XML standard naming to an OWL model derived
 * from CIM UML.
 * 
 * This specialized translator is designed to perform URI remappings related to
 * any non-normative extensions to the base CIM that appear in a different
 * namespace.
 */
public class ExtensionsTranslator implements Runnable {

	private OntModel model;
	private OntModel resultModel;
	private String defaultNamespace;

	/**
	 * Construct from input model.
	 * 
	 * @param input   the model for which to translate shadow classes on.
	 * @param namespace the namespace
	 */
	public ExtensionsTranslator(OntModel model, String namespace) {
		this.model = model;
		OntResource ont = model.getValidOntology();
		if (ont != null) {
			defaultNamespace = ont.getURI() + "#";
		} else {
			defaultNamespace = namespace;
		}
	}

	/**
	 * @return Returns the result model.
	 */
	public OntModel getModel() {
		return resultModel;
	}

	/**
	 * Apply translation to input model, populating the result model.
	 * 
	 * Each statement in the input model is transcribed to the result model with
	 * some resources substituted.
	 */
	public void run() {
		pass.run();
	}

	private abstract class Pass implements Runnable {

		protected abstract FrontsNode rename(OntResource r);

		/**
		 * Pass over every statement and apply renameResource() to each resource.
		 */
		public void run() {
			resultModel = ModelFactory.createMem();
			Iterator it = model.getGraph().find(Node.ANY, Node.ANY, Node.ANY);
			
			while (it.hasNext()) {
				Triple s = (Triple) it.next();
				//
				Node subject = s.getSubject();
				Node predicate = s.getPredicate();
				Node object = s.getObject();
				//
				OntResource newSubject = model.createResource(subject);
				OntResource newPredicate = model.createResource(predicate);
				//
				FrontsNode renamedSubject = renameResource(newSubject);
				FrontsNode renamedPredicate = renameResource(newPredicate);
				Node renamedObject = renameObject(object);
				//
				add(renamedSubject, renamedPredicate, renamedObject);
			}

			model = resultModel;
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
		 * Substitute a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result or null if the resource
		 *         should not appear in the result.
		 */
		protected FrontsNode renameResource(OntResource r) {
			if (r.isAnon())
				return r;

			switch (r.getNameSpace()) {
			case XMI.NS:
			case UML.NS:
			case OWL.NS:
				return r;
			default:
				if (r.getNameSpace().equals(RDF.getURI()) || r.getNameSpace().equals(RDFS.getURI()))
					return r;
				break;
			}

			String ls = r.getString(RDFS.label);
			if (ls == null)
				return r;

			String l = escapeNCName(ls);
			if (l.length() == 0)
				return null;

			return rename(r);
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
	 * This translation pass simlpy .
	 */
	Runnable pass = new Pass() {

		/**
		 * For attributes and associations defined on shadow extension classes the URI
		 * of these classes must be renamed after they are "migrated" from the shadow
		 * class into the normative class. The rename simply remaps the name to the
		 * namespace and domain class name that the property gets migrated into.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result or null if the resource
		 *         should not appear in the result.
		 */
		@Override
		protected FrontsNode rename(OntResource r) {
			
			boolean isEnumeratedAttribute = isEnumeratedAttribute(r);
			
			if ((r.hasProperty(RDF.type, OWL.ObjectProperty) || //
					r.hasProperty(RDF.type, OWL.DatatypeProperty) || //
					r.hasProperty(RDF.type, RDF.Property)) || //
					isEnumeratedAttribute) {
				
				OntResource domain = r.getResource(RDFS.domain);
				
				if (domain != null) {
					String domainClass = domain.getString(RDFS.label); //
					if (domainClass != null) {
						String propNamespace = r.getNameSpace();
						String inversePropNamespace = null; // Must default to NULL and only set if there exists an inverse...
						OntResource inverse = r.getInverse();
						if (inverse != null) {
							inversePropNamespace = inverse.getNameSpace();
						}
						//
						String propDomain = r.getURI().substring(r.getURI().lastIndexOf("#") + 1,
								r.getURI().lastIndexOf("."));
						String propName = r.getString(RDFS.label);

						/**
						 * Given that the attributes and associations defined on shadow extension
						 * classes are migrated into the normative class being shadowed, the URI of
						 * these classes must be "remapped" after they are migrated. The URI change that
						 * is performed is using the name of the new domain for the property within the
						 * URI of the property. For example:
						 * 
						 * http://entsoe.eu/CIM/SchemaExtension/3/1#ExtEuIdentifiedObject.shortName ->
						 * http://entsoe.eu/CIM/SchemaExtension/3/1#IdentifiedObject.shortName
						 */
						String newPropNamespace = null;
						if (propNamespace.equals(defaultNamespace) && (inversePropNamespace != null && !inversePropNamespace.equals(defaultNamespace))) {
							newPropNamespace = inversePropNamespace;
						} else if (!propNamespace.equals(defaultNamespace) && (inversePropNamespace != null && inversePropNamespace.equals(defaultNamespace))) {
							newPropNamespace = propNamespace;
						}
						
						String newPropDomain = null;		
						if (!propDomain.equals(domainClass)) {
							newPropDomain = domainClass;
						}

						if (newPropNamespace != null || newPropDomain != null) {
							String newURI = (newPropNamespace != null ? newPropNamespace : propNamespace) + (newPropDomain != null ? newPropDomain : propDomain) + "." + propName;
							OntResource newResource = resultModel.createResource(newURI);
							return newResource;
						}
					}
				} else if (isEnumeratedAttribute) {
					String propNamespace = r.getNameSpace();
					OntResource type = null;
					ResIterator types = r.listRDFTypes(false);
					if (types.hasNext()) {
						type = types.nextResource();
						if (!type.hasProperty(UML.hasStereotype, UML.enumeration))
							type = null;
					}
					//
					if (type != null) {
						String typeDomain = type.getLabel();
						String propName = r.getString(RDFS.label);
						String newURI = propNamespace + typeDomain + "." + propName;
	
						/**
						 * Given that the enumerations defined on shadow class extensions
						 * are migrated into the normative enumeration being shadowed, the URI of
						 * these classes must be "remapped" after they are migrated. The URI change that
						 * is performed is using the name of the new domain for the property within the
						 * URI of the property. For example:
						 * 
						 * http://entsoe.eu/CIM/SchemaExtension/3/1#ExtSinglePhaseKind.kind ->
						 * http://entsoe.eu/CIM/SchemaExtension/3/1#SinglePhaseKind.kind
						 */
						if (!r.getURI().equals(newURI)) {
							return resultModel.createResource(newURI);
						}
					}
				}
			}
			
			return r;
		}
		
		private boolean isEnumeratedAttribute(OntResource r) {
			boolean isEnumeratedAttribute = false;
			if (!r.isProperty() && //
				!r.isDatatype() && //
				!r.isClass() && //
				!r.hasProperty(RDF.type, UML.Package) && //
				!r.hasProperty(UML.hasStereotype, UML.cimdatatype) && //
				!r.hasProperty(UML.hasStereotype, UML.primitive) && //
				!r.hasProperty(UML.hasStereotype, UML.enumeration) ) {
				ResIterator types = r.listRDFTypes(false);
				if (types.hasNext()) {
					OntResource type = types.nextResource();
					if (type.hasProperty(UML.hasStereotype, UML.enumeration))
						isEnumeratedAttribute = true;
				}
			}
			return isEnumeratedAttribute;
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
			resultModel.add(s, p, o);
		}
	}

}
