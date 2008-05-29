package au.com.langdale.xmi;

import java.util.Iterator;

import org.apache.xerces.util.XMLChar;

import au.com.langdale.cim.CIM;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 *	A translator which produces a OWL model with CIM/XML standard naming from 
 *  a OWL model derived from CIM UML. 
 */
public class Translator implements Runnable {
	
	private OntModel model;
	private OntModel result;
	private String defaultNamespace;
	
	/**
	 * Construct from input model and the namespace for renamed resources.
	 * 
	 * @param input the model to be translated.
	 */
	public Translator(OntModel model, String namespace ) {
		this.model = model;
		defaultNamespace = namespace;
	}
	
	/**
	 * @return Returns the result model.
	 */
	public OntModel getResult() {
		return result;
	}
	
	/**
	 * Apply translation to input model, populating the result model.
	 * 
	 * Each statement in the input model is transcribed to the 
	 * result model with some resources substituted. 
	 */
	public void run() {

		pass1.run();
		propagateAnnotation(UML.baseuri);
		pass2.run();
	}
	
	private abstract class Pass implements Runnable {
		protected abstract Resource renameResource(Resource r, String l);

		/**
		 * Pass over every statement and apply renameResource() to each resource.
		 */
		public void run() {
			result = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
			StmtIterator it = model.listStatements();
			while( it.hasNext()) {
				Statement s = it.nextStatement();
				add(
					renameResource(s.getSubject()), 
					renameResource(s.getPredicate()),
					renameObject(s.getObject()));
			}
			model = result;
		}

		/**
		 * Substitute a single node.
		 * 
		 * If the node is a resource then it is substituted as
		 * required.  If the node is a literal it is preserved.
		 */
		protected RDFNode renameObject(RDFNode n) {
			if( n instanceof Resource)
				return renameResource((Resource) n);
			else
				return n;
		}
		/**
		 * Substitute a a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result 
		 * or null if the resource should not appear in the result.
		 */
		protected Resource renameResource(Resource r) {
			if (r.isAnon())
				return r;
			
			if (!r.getNameSpace().equals(XMI.NS))
				return r;
			
//			if( ! r.hasProperty(RDF.type))
//				return null;

			Statement ls = r.getProperty(RDFS.label);
			if (ls == null)
				return r;

			String l = escapeNCName(ls.getString());
			if( l.length()==0 )
				return null;
			
			return renameResource(r, l);
		}		
	}
	
	/**
	 * This pass translates annotations and stereotypes 
	 * to resources in the UML namespace.  These are
	 * then available in later passes.
	 */
	Runnable pass1 = new Pass() {
		/**
		 * Substitute a a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result 
		 * or null if the resource should not appear in the result.
		 */
		@Override
		protected Resource renameResource(Resource r, String l) {
			if (r.hasProperty(RDF.type, UML.Stereotype))
				return result.createResource(UML.NS + l.toLowerCase());

			else if (r.hasProperty(RDF.type, OWL.AnnotationProperty)) {
				if (l.equals("documentation"))
					return RDFS.comment;
				else if (l.equals("NERCProfile"))
					// TODO: how should NERC profile be represented?
					return result.createResource(CIM.NS + l);
				else if(l.equals(UML.baseuri.getLocalName()))
					return UML.baseuri;
				else
					return null; // omit other annotations eg 'transient' defined in the UML
			}
			
			else if( r.hasProperty(RDF.type, UML.Package)) 
				// all packages are in the default namespace
				return result.createResource(defaultNamespace + "Package_" + l);  
				
			else
				return r;
		}
	};

	
	/**
	 * This pass translates annotations and stereotypes 
	 * to resources in the UML namespace.  These are
	 * then available in later passes.
	 */
	Runnable pass2 = new Pass() {
		/**
		 * Substitute a a single resource.
		 * 
		 * @param r the resource to be (possibly) replaced.
		 * @return the resource that should appear in result 
		 * or null if the resource should not appear in the result.
		 */
		@Override
		protected Resource renameResource(Resource r, String l) {
			String namespace = findBaseURI(r);

			if (r.hasProperty(RDF.type, OWL.Class)) {
				if ((r.hasProperty(UML.hasStereotype, UML.datatype) ||
						r.hasProperty(UML.hasStereotype, UML.primitive)) &&
						    ! r.hasProperty(UML.hasStereotype, UML.enumeration)) {
					Resource x = selectXSDType(l);
					if (x != null)
						return x;
				}
				return result.createResource(namespace + l);
			}

			if (r.hasProperty(RDF.type, OWL.ObjectProperty)
					|| r.hasProperty(RDF.type, OWL.DatatypeProperty)
					|| r.hasProperty(RDF.type, RDF.Property)) {
				Statement ds = r.getProperty(RDFS.domain);
				if (ds != null) {
					Statement ls = ds.getResource().getProperty(RDFS.label);
					if (ls != null) {
						String c = escapeNCName(ls.getString());
						return result.createResource(namespace + c + "." + l);
					}
				}
				return result.createResource(namespace + "_." + l);
			}
			
			for (Iterator it = r.listProperties(RDF.type); it.hasNext();) {
				Statement ts = (Statement) it.next();
				if( ts.getResource().hasProperty(UML.hasStereotype, UML.enumeration)) {
					Statement ls = ts.getResource().getProperty(RDFS.label);
					if (ls != null) {
						String c = escapeNCName(ls.getString()); 
						return result.createResource(namespace + c + "." + l);
					}
				}
			}

			return result.createResource(namespace + l);
		}
	};
	
	/**
	 * Regenerate a statement in the result model with given
	 * subject, predicate and object. 
	 * 
	 * s, p, o are the resources from an input model statement 
	 * with possible substitutions. If any are null (no substitution) 
	 * the original statement is negated.
	 */
	protected void add(Resource s, Resource p, RDFNode o) {
		if(s != null && p != null && o != null ) {
			result.add(s, (Property)p.as(Property.class), o);
		}
	}
	/**
	 * Select XSD datatypes for UML attributes.
	 * 
	 * @param l A simple name for the datatype received from the UML. 
	 * @return A resource representing one of the XSD datatypes recommended for OWL.
	 */
	protected Resource selectXSDType(String l) {
		// TODO: add more XSD datatypes here
		if( l.equalsIgnoreCase("integer"))
			return XSD.integer;
		else if( l.equalsIgnoreCase("int"))
			return XSD.xint;
		else if( l.equalsIgnoreCase("unsigned"))
			return XSD.unsignedInt;
		else if( l.equalsIgnoreCase("ulong") ||
				l.equalsIgnoreCase("ulonglong"))
			return XSD.unsignedLong;
		else if( l.equalsIgnoreCase("short"))
			return XSD.xshort;
		else if( l.equalsIgnoreCase("long")||
				l.equalsIgnoreCase("longlong"))
			return XSD.xlong;
		else if( l.equalsIgnoreCase("string") ||
				l.equalsIgnoreCase("char"))
			return XSD.xstring;
		else if( l.equalsIgnoreCase("float"))
			return XSD.xfloat;
		else if( l.equalsIgnoreCase("double") ||
				l.equalsIgnoreCase("longdouble"))
			return XSD.xdouble;
		else if( l.equalsIgnoreCase("boolean")  ||
				l.equalsIgnoreCase("bool"))
			return XSD.xboolean;
		else if( l.equalsIgnoreCase("decimal"))
			return XSD.decimal;
		else if( l.equalsIgnoreCase("nonNegativeInteger"))
			return XSD.nonNegativeInteger;
		else if( l.equalsIgnoreCase("date"))
			return XSD.date;
		else if( l.equalsIgnoreCase("time"))
			return XSD.time;
		else if( l.equalsIgnoreCase("datetime"))
			return XSD.dateTime;
		else if( l.equalsIgnoreCase("absolutedatetime"))
			return XSD.dateTime;
		else
			return null;
	}
	
	/**
	 * Convert a string to an NCNAME by substituting underscores for
	 * invalid characters.
	 */
	public static String escapeNCName(String label) {
		StringBuffer result = new StringBuffer(label.trim());
		for(int ix = 0; ix < result.length(); ix++) {
			if( ! XMLChar.isNCName(result.charAt(ix))) 
				result.setCharAt(ix, '_');
		}
		if(result.length() > 0 && ! XMLChar.isNCNameStart(result.charAt(0))) 
			result.insert(0, '_');
		return result.toString();
	}
	
	
	/**
	 * Discover the base URI, if given, for a model element.
	 * @param r an untranslated  resource
	 * @return a URI
	 */
	protected String findBaseURI(Resource r) {
		Statement b = r.getProperty(UML.baseuri);
		if( b != null )
			return b.getString();

		Statement p = r.getProperty(RDFS.isDefinedBy);
		if( p != null ) {
			b = p.getResource().getProperty(UML.baseuri);
			if( b != null )
				return b.getString();
		}

		return defaultNamespace;
	}

	/**
	 * Propagate a given annotation property to descendents.
	 *
	 */
	private void propagateAnnotation(Property a) {
		ResIterator it = model.listSubjectsWithProperty(RDF.type, UML.Package);
		while( it.hasNext()) 
			propagateAnnotation(it.nextResource(), a);
	}

	/**
	 * Propagate a given annotation property from p's parent package to p.
	 *
	 */
	private Statement propagateAnnotation(Resource p, Property a) {
		Statement v = p.getProperty(a);
		if( v != null )
			return v;
	
		Statement s = p.getProperty(RDFS.isDefinedBy);
		if( s != null ) {
			v = propagateAnnotation(s.getResource(), a); 
			if( v != null) {
				p.addProperty(a, v.getObject());
				return v;
			}
		}
		
		return null;
	}
}
