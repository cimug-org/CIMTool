/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.jena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.graph.FrontsNode;

import au.com.langdale.inference.LOG;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ResIterator;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 *  Represent an ontology as a tree using a UML inspired layout.
 * 
 */
public class UMLTreeModel extends JenaTreeModelBase {
	/**
	 * The root node and nodes under packages will be 
	 * packages or classes. 
	 */
	@Override
	protected Node classify(OntResource child) {
		if( child.equals(UML.global_package))
			return new GlobalNode();
		if( child.hasProperty(RDF.type, UML.Package))
			return new PackageNode(child);
		else if( child.isClass()) {
			if( child.hasProperty(UML.hasStereotype, UML.enumeration))
				return new EnumClassNode(child);
			else
				return new ClassNode( child );
		}
		else if( child.isDatatypeProperty()) {
			return new DatatypeNode(child); // used for the SubClassModel
		}
		return new Empty("");
	}
	
	/**
	 * Construct a list of resources representing a path starting 
	 * from the root resource of this tree to the given target.
	 */
	@Override
	protected List findResourcePathTo(FrontsNode symbol) {
		OntResource target = asOntResource(symbol);
		if( getRoot() == null || target == null) 
			return null;
		OntResource start = getRoot().getSubject();
		if( start == null)
			return null;
		
		ArrayList path = new ArrayList(6);
		path.add(target);

		while( ! target.equals(start)) {
			if( target.isProperty()) {
				target = target.getDomain();
				if( target == null )
					return null;
			}
			else {
				ResIterator it = target.listRDFTypes(false);
				OntResource enumerated = null;
				while( it.hasNext()) {
					OntResource cls = it.nextResource();
					if( cls.hasProperty(UML.hasStereotype, UML.enumeration)) {
						enumerated = cls;
						break;
					}
				}
				
				if( enumerated != null ) {
					target = enumerated;
				}
				else {
					OntResource pack = target.getIsDefinedBy();
					if( pack == null)
						target = getOntModel().createResource(UML.global_package.asNode());
					else
						target = pack;
				}
			}
			if( path.contains(target))
				return null;
			path.add(target);
		}
		Collections.reverse(path);
		return path;
	}
	
	/**
	 * The global package that includes anything without an explicit
	 * isDefinedBy property.
	 */
	public class GlobalNode extends ModelNode {

		@Override
		public boolean getErrorIndicator() {
			return false;
		}

		@Override
		public OntResource getSubject() {
			return getOntModel().createResource(UML.global_package.asNode());
		}

		@Override
		protected void populate() {
			Set packages = getOntModel().listIndividuals(UML.Package).toSet();
			packages.remove(UML.global_package);
			addTopPackages(packages);
			addTopClasses(packages);
			addTopDatatypes(packages);
		}

		private void addTopPackages(Set packages) {
			Iterator it = packages.iterator();
			while( it.hasNext()) {
				OntResource pack = (OntResource)it.next();
				if( ! isDefinedBy(pack, packages))
					add(new PackageNode(pack));
			}
		}

		private void addTopClasses(Set packages) {
			Iterator it = getOntModel().listNamedClasses();
			while( it.hasNext()) {
				OntResource clss = (OntResource)it.next();
				if( ! isDefinedBy(clss, packages)) {
					if( clss.hasProperty(UML.hasStereotype, UML.enumeration))
						add(new EnumClassNode(clss));
					else
						add(new ClassNode(clss));
				}
			}
		}

		private void addTopDatatypes(Set packages) {
			ResIterator it = getOntModel().listSubjectsWithProperty(RDF.type, RDFS.Datatype);
			while( it.hasNext()) {
				OntResource dt = it.nextResource();
				if( ! isDefinedBy(dt, packages)) {
					add(new DatatypeClassNode(dt));
				}
			}
		}
	}
	
	private boolean isDefinedBy(OntResource subject, Set packages) {
		boolean top = false;
		Iterator jt = subject.listIsDefinedBy();
		while( jt.hasNext()) {
			if( packages.contains(jt.next())) {
				top = true;
				break;
			}
		}
		return top;
	}
	
	/**
	 * A UML package.
	 * 
	 *
	 */
	public class PackageNode extends ModelNode {
		OntResource subject;
		
		public PackageNode(OntResource pack) {
			subject = pack;
		}
		
		@Override
		protected void populate() {
			OntModel m = subject.getOntModel();
			ResIterator it = m.listSubjectsWithProperty(RDFS.isDefinedBy, subject) ;
			while( it.hasNext()) {
				OntResource child = it.nextResource();
				if( child.hasRDFType(UML.Package))
					add(new PackageNode(child));
				else if( child.hasRDFType( RDFS.Datatype))
					add(new DatatypeClassNode(child));
				else if( child.isClass()){
					if( child.hasProperty(UML.hasStereotype, UML.enumeration))
						add(new EnumClassNode(child));
					else if( child.hasProperty(UML.hasStereotype, UML.extension))
						add(new ExtensionClassNode(child));
					else if( child.hasProperty(UML.hasStereotype, UML.compound))
						add(new CompoundClassNode(child));
					else
						add(new ClassNode( child ));
				}
			}
		}
		
		@Override
		public String toString() {
			return "Package: " + label(subject);
		}
		
		@Override
		public OntResource getSubject() {
			return subject;
		}
		
		@Override
		protected String collation() {
			return "1" + toString();
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}
	}
	
	/**
	 * A class in the ontology.
	 * 
	 *
	 */
	public class ClassBaseNode extends ModelNode {
		OntResource subject;
		
		public ClassBaseNode(OntResource clss) {
			subject = clss;
		}
		
		@Override
		protected void populate() {
			// populate(true, false); // the old way
			populate(false, true, true);
		}

		protected void populate(boolean inherited, boolean supers, boolean subs) {
			
			// prevent tedious back links from child to parent
			Node parent = getParent();
			OntResource exclude = null; 
			if( parent != null) {
				exclude = parent.getSubject();
				if( exclude.isObjectProperty()) {
					OntResource property = exclude;
					exclude = property.getInverse();
					if( exclude == null )
						exclude = property.getInverseOf();
				}
			}
			
			if( supers) {
				ResIterator it = subject.listSuperClasses(false);
				while( it.hasNext()) {
					OntResource clss = it.nextResource();
					if( ( exclude == null || ! clss.equals(exclude)) && clss.isURIResource() && clss.isClass()) {
						if( clss.hasProperty(UML.hasStereotype, UML.extension))
							add( new ExtensionNode( clss ));
						else
						    add( new SuperClassNode( clss ));
					}
				}
			}
			
			if(subs) {
				ResIterator it = subject.listSubClasses(false);
				while( it.hasNext()) {
					OntResource clss = it.nextResource();
					if( ! clss.hasProperty(UML.hasStereotype, UML.enumeration) &&
							( exclude == null || ! clss.equals(exclude)) &&
							clss.isClass())
						add( new SubClassNode(clss));
				}
			}
			
			{
				Iterator it = listProperties(inherited);
				while( it.hasNext()) {
					OntResource pt = (OntResource) it.next();
					if( pt.isDatatypeProperty())
						add( new DatatypeNode(pt));
					else if( pt.getRange() != null && pt.getRange().hasProperty(UML.hasStereotype, UML.enumeration))
						add( new EnumPropertyNode(pt));
					else if(pt.hasProperty(UML.hasStereotype, UML.aggregateOf)) 
						add( new AggregateNode(pt));
					else if(pt.hasProperty(UML.hasStereotype, UML.compositeOf)) 
						add( new CompositeNode(pt));
					else if( pt.isFunctionalProperty()) 
						add( new FunctionalNode(pt));
					else if( pt.isInverseFunctionalProperty())
						add( new InverseNode(pt));
					else 
						add( new PropertyNode(pt));
				}
			}
			
			{
				ResIterator it = subject.listInstances();
				while( it.hasNext())
					add( new IndividualNode(it.nextResource()));
			}
		}
		
		protected Iterator listProperties(boolean inherited) {
			Set results = new HashSet();
			
			addDirectProperties(subject, results);
			
			if( inherited ) {
				ResIterator it =subject.listSuperClasses(false);
				while( it.hasNext()) {
					addDirectProperties(it.nextResource(), results);
				}
			}
			return results.iterator();
		}
		
		private void addDirectProperties(Resource subject, Set results) {
			ResIterator it = getOntModel().listSubjectsWithProperty(RDFS.domain, subject);
			while( it.hasNext()) {
				OntResource pr = it.nextResource();
				if( pr.isProperty())
					results.add(pr);
			}
		}

		@Override
		public String toString() {
			return "Class: " + label(subject);
		}
		
		@Override
		protected String collation() {
			return "2" + toString();
		}
		
		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}
	}
	
	public class ClassNode extends ClassBaseNode {

		public ClassNode(OntResource clss) {
			super(clss);
		}
		
	}
	
	public class ExtensionClassNode extends ClassBaseNode {

		public ExtensionClassNode(OntResource clss) {
			super(clss);
		}
		
	}
	
	public class CompoundClassNode extends ClassBaseNode {

		public CompoundClassNode(OntResource clss) {
			super(clss);
		}
		
	}
	
	/**
	 * A class to be shown as a sub-class of the parent node.
	 * 
	 */
	public class SubClassNode extends ClassBaseNode {
		public SubClassNode(OntResource clss) {
			super(clss);
		}
		
		@Override
		protected void populate() {
			populate(false, true, true);
		}
		
		@Override
		public boolean isPruned() {
			return true;
		}
		
		@Override
		public String toString() {
			return "SubClass: " + label(subject);
		}

		@Override
		protected String collation() {
			return "7" + toString();
		}
	}
	/**
	 * A class to be shown as a super-class of the parent node.
	 * 
	 */
	public class SuperClassNode extends ClassBaseNode {
		public SuperClassNode(OntResource clss) {
			super(clss);
		}
		
		@Override
		protected void populate() {
			populate(false, true, false);
		}
		
		@Override
		public boolean isPruned() {
			return true;
		}

		@Override
		public String toString() {
			return "SuperClass: " + label(subject);
		}

		@Override
		protected String collation() {
			return "6" + toString();
		}
	}
	/**
	 * A class to be shown as an extension of the parent node.
	 * 
	 */
	public class ExtensionNode extends ClassBaseNode {
		public ExtensionNode(OntResource clss) {
			super(clss);
		}
		
		@Override
		protected void populate() {
			populate(false, true, false);
		}
		
		@Override
		public boolean isPruned() {
			return true;
		}

		@Override
		public String toString() {
			return "Extension: " + label(subject);
		}

		@Override
		protected String collation() {
			return "5" + toString();
		}
	}
	
	/**
	 * An enumerated class.
	 * 
	 */
	public class EnumClassNode extends ClassBaseNode {
		public EnumClassNode(OntResource clss) {
			super(clss);
		}
		
		@Override
		protected void populate() {
			populate(false, true, true);
		}
	}
	
	/**
	 * A datatype class node (appearing below package)
	 */
	public class DatatypeClassNode extends ModelNode {

		OntResource subject;
		
		public DatatypeClassNode(OntResource subject) {
			this.subject = subject;
		}

		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		protected void populate() {
			// no children
		}
		
		@Override
		public String toString() {
			OntResource base = subject.getEquivalentClass();
			String units = subject.getString(UML.hasUnits);
			
			return "Datatype: " + label(subject) 
				+ (base != null? " = " + label(base): "")
				+ (units != null? " (" + units + ")": "");
		}

		@Override
		public Class getIconClass() {
			return DatatypeNode.class;
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}

		@Override
		public boolean getAllowsChildren() {
			return false;
		}
	}

	/**
	 * A datatype property in the ontology.
	 * 
	 */
	public class DatatypeNode extends ModelNode {
		OntResource subject;
		public DatatypeNode( OntResource prop) {
			subject = prop;
		}

		@Override
		public boolean getAllowsChildren() {
			return false;
		}

		@Override
		protected void populate() {
			// no children
		}
		
		@Override
		public String toString() {
			OntResource range = subject.getRange();
			String units = range != null? range.getString(UML.hasUnits): null;
			String value = subject.getString(UML.hasInitialValue);
			String rname = label(range);
			
			return label(subject) 
				+ (range != null && !rname.equals("<undefined>")? ": " + rname: "")
				+ (value != null? " = " + value: "")
				+ (units != null? " (" + units + ")": "");
		}
		
		@Override
		protected String collation() {
			return "3" + toString();
		}
		
		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}
	}
	
	/**
	 * A general object property.
	 * 
	 */
	public class PropertyNode extends ModelNode {
		OntResource subject;
		public PropertyNode( OntResource prop) {
			subject = prop;
		}
		
		@Override
		protected void populate() {
			OntResource range = subject.getRange(); 
			if( range != null && range.isClass())
				adopt(new ClassNode(range));
		}
		
		@Override
		public String toString() {
			return prop_label(subject);
		}
		
		@Override
		protected String collation() {
			return "4" + toString();
		}
		
		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}
		
		@Override
		public boolean isPruned() {
			return true;
		}
	}
	
	/**
	 * A aggregate object property.
	 * 
	 */
	public class AggregateNode extends PropertyNode {
		public AggregateNode(OntResource prop) {
			super(prop);
		}
	}
	
	/**
	 * A composite object property.
	 * 
	 */
	public class CompositeNode extends PropertyNode {
		public CompositeNode(OntResource prop) {
			super(prop);
		}
	}
	
	/**
	 * A functional object property.
	 * 
	 */
	public class FunctionalNode extends PropertyNode {
		public FunctionalNode(OntResource prop) {
			super(prop);
		}
	}
	
	/**
	 * An inverse functional object property.
	 * 
	 */
	public class InverseNode extends PropertyNode {
		public InverseNode(OntResource prop) {
			super(prop);
		}
	}
	
	/**
	 * A functional property whose object is an enumeration.
	 * 
	 *
	 */
	public class EnumPropertyNode extends PropertyNode {
		public EnumPropertyNode(OntResource prop) {
			super(prop);
		}
		
		@Override
		protected void populate() {
			OntResource range = subject.getRange();
			adopt(new EnumClassNode(range));
		}
		
		@Override
		protected String collation() {
			return "3" + toString();
		}
	}
	
	/**
	 * An instance (individual) of an enumerated class.
	 * 
	 *
	 */
	public class IndividualNode extends ModelNode {
		OntResource subject;
		public IndividualNode( OntResource res) {
			subject = res;
		}

		@Override
		protected void populate() {
			// no children
		}

		@Override
		public boolean getAllowsChildren() {
			return false;
		}
		
		@Override
		public OntResource getSubject() {
			return subject;
		}

		@Override
		public boolean getErrorIndicator() {
			return subject.hasProperty(LOG.hasProblems);
		}
	}
}
