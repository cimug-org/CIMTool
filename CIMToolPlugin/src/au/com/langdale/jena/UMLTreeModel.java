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

import au.com.langdale.validation.LOG;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
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
		else if( child.canAs(OntClass.class)) {
			OntClass cl = (OntClass) child.as(OntClass.class);
			if( cl.hasProperty(UML.hasStereotype, UML.enumeration))
				return new EnumClassNode(cl);
			else
				return new ClassNode( cl );
		}
		else if( child.canAs(DatatypeProperty.class)) {
			return new DatatypeNode(child.asProperty()); // used for the SubClassModel
		}
		return null;
	}
	
	/**
	 * Construct a list of resources representing a path starting 
	 * from the root resource of this tree to the given target.
	 */
	@Override
	protected List findResourcePathTo(Resource symbol) {
		OntResource target = asOntResource(symbol);
		if( getRoot() == null || target == null) 
			return null;
		OntResource start = getRoot().getSubject();
		if( start == null)
			return null;
		
		ArrayList path = new ArrayList(6);
		path.add(target);

		while( ! target.equals(start)) {
			if( target.canAs(OntProperty.class)) {
				OntProperty prop = (OntProperty) target.as(OntProperty.class);
				target = prop.getDomain();
				if( target == null )
					return null;
			}
			else {
				ExtendedIterator it = target.listRDFTypes(false);
				OntResource enumerated = null;
				while( it.hasNext()) {
					OntResource cls = (OntResource) ((Resource)it.next()).as(OntResource.class);
					if( cls.hasProperty(UML.hasStereotype, UML.enumeration)) {
						enumerated = cls;
						break;
					}
				}
				
				if( enumerated != null ) {
					target = enumerated;
				}
				else {
					Resource pack = target.getIsDefinedBy();
					if( pack == null)
						target = getOntModel().getOntResource(UML.global_package);
					else
						target = (OntResource) pack.as(OntResource.class);
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
			return getOntModel().createOntResource(UML.global_package.getURI());
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
				OntClass clss = (OntClass)it.next();
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
				OntResource dt = (OntResource) it.nextResource().as(OntResource.class);
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
		
		public PackageNode(RDFNode pack) {
			subject = (OntResource) pack.as(OntResource.class);
		}
		
		public PackageNode(OntResource pack) {
			subject = pack;
		}
		
		@Override
		protected void populate() {
			Model m = subject.getModel();
			ResIterator it = m.listSubjectsWithProperty(RDFS.isDefinedBy, subject) ;
			while( it.hasNext()) {
				Resource child = it.nextResource();
				if( child.hasProperty(RDF.type, UML.Package))
					add(new PackageNode(child));
				else if( child.hasProperty(RDF.type, RDFS.Datatype))
					add(new DatatypeClassNode((OntResource)child.as(OntResource.class)));
				else if( child.canAs(OntClass.class)) {
					OntClass cl = (OntClass) child.as(OntClass.class);
					if( cl.hasProperty(UML.hasStereotype, UML.enumeration))
						add(new EnumClassNode(cl));
					else
						add(new ClassNode( cl ));
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
		OntClass subject;
		
		public ClassBaseNode(OntClass clss) {
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
					OntProperty property = exclude.asProperty();
					exclude = property.getInverse();
					if( exclude == null )
						exclude = property.getInverseOf();
				}
			}
			
			if( supers) {
				ExtendedIterator it = new OntSubject(subject).listSuperClasses(true);
				while( it.hasNext()) {
					OntResource clss = (OntResource) it.next();
					if( ( exclude == null || ! clss.equals(exclude)) && clss.isURIResource() && clss.canAs(OntClass.class)) {
						add( new SuperClassNode((OntClass) clss.as(OntClass.class)));
					}
				}
			}
			
			if(subs) {
				ExtendedIterator it = new OntSubject(subject).listSubClasses(true);
				while( it.hasNext()) {
					OntResource clss = (OntResource) it.next();
					if( ! clss.hasProperty(UML.hasStereotype, UML.enumeration) &&
							( exclude == null || ! clss.equals(exclude)) &&
							clss.canAs(OntClass.class))
						add( new SubClassNode((OntClass) clss.as(OntClass.class)));
				}
			}
			
			{
				Iterator it = listProperties(inherited);
				while( it.hasNext()) {
					OntProperty pt = (OntProperty) it.next();
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
				ExtendedIterator it = subject.listInstances();
				while( it.hasNext())
					add( new IndividualNode((OntResource)it.next()));
			}
		}
		
		protected Iterator listProperties(boolean inherited) {
			Set results = new HashSet();
			
			addDirectProperties(subject, results);
			
			if( inherited ) {
				ExtendedIterator it = new OntSubject(subject).listSuperClasses(false);
				while( it.hasNext()) {
					addDirectProperties((Resource)it.next(), results);
				}
			}
			return results.iterator();
		}
		
		private void addDirectProperties(Resource subject, Set results) {
			ResIterator it = getOntModel().listSubjectsWithProperty(RDFS.domain, subject);
			while( it.hasNext()) {
				Resource pr = it.nextResource();
				if( pr.canAs(OntProperty.class)) 
					results.add(pr.as(OntProperty.class));
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

		public ClassNode(OntClass clss) {
			super(clss);
		}
		
	}
	
	/**
	 * A class to be shown as a sub-class of the parent node.
	 * 
	 */
	public class SubClassNode extends ClassBaseNode {
		public SubClassNode(OntClass clss) {
			super(clss);
		}
		
		@Override
		protected void populate() {
			populate(false, true, true);
		}
		
		@Override
		public String toString() {
			return "SubClass: " + label(subject);
		}

		@Override
		protected String collation() {
			return "6" + toString();
		}
	}
	/**
	 * A class to be shown as a super-class of the parent node.
	 * 
	 */
	public class SuperClassNode extends ClassBaseNode {
		public SuperClassNode(OntClass clss) {
			super(clss);
		}
		
		@Override
		protected void populate() {
			populate(false, true, false);
		}
		
		@Override
		public String toString() {
			return "SuperClass: " + label(subject);
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
		public EnumClassNode(OntClass clss) {
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
			OntResource base = subject.getSameAs();
			RDFNode units = subject.getPropertyValue(UML.hasUnits);
			
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
		OntProperty subject;
		public DatatypeNode( OntProperty prop) {
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
			RDFNode units = range != null? range.getPropertyValue(UML.hasUnits): null;
			RDFNode value = subject.getPropertyValue(UML.hasInitialValue);
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
		OntProperty subject;
		public PropertyNode( OntProperty prop) {
			subject = prop;
		}
		
		@Override
		protected void populate() {
			OntResource range = subject.getRange(); 
			if( range != null && range.canAs(OntClass.class))
				adopt(new ClassNode(range.asClass()));
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
	}
	
	/**
	 * A aggregate object property.
	 * 
	 */
	public class AggregateNode extends PropertyNode {
		public AggregateNode(OntProperty prop) {
			super(prop);
		}
	}
	
	/**
	 * A composite object property.
	 * 
	 */
	public class CompositeNode extends PropertyNode {
		public CompositeNode(OntProperty prop) {
			super(prop);
		}
	}
	
	/**
	 * A functional object property.
	 * 
	 */
	public class FunctionalNode extends PropertyNode {
		public FunctionalNode(OntProperty prop) {
			super(prop);
		}
	}
	
	/**
	 * An inverse functional object property.
	 * 
	 */
	public class InverseNode extends PropertyNode {
		public InverseNode(OntProperty prop) {
			super(prop);
		}
	}
	
	/**
	 * A functional property whose object is an enumeration.
	 * 
	 *
	 */
	public class EnumPropertyNode extends PropertyNode {
		public EnumPropertyNode(OntProperty prop) {
			super(prop);
		}
		
		@Override
		protected void populate() {
			OntClass range = subject.getRange().asClass();
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
