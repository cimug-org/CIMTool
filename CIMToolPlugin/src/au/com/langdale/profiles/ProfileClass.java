/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import au.com.langdale.xmi.UML;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResIterator;

import au.com.langdale.kena.Resource;
import com.hp.hpl.jena.util.OneToManyMap;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Represents a class in a profile, encapsulating its base (original) class and restrictions.
 */
public class ProfileClass {
	private final OntResource clss;
	private final String namespace;
	private final OntModel model;

	private OneToManyMap props;
	private OntResource baseClass;
	private Set classes;
	private boolean enumeratedBase;
	private final OntResource defaultBase;
	
	public ProfileClass(OntResource clss, String namespace, OntResource base) {
		this.clss = clss;
		this.namespace = namespace;
		this.model = clss.getOntModel();
		this.defaultBase = base;
		analyse();
	}
	
	public ProfileClass(OntResource clss, String namespace) {
		this(clss, namespace, clss.getOntModel().createClass( OWL.Thing.getURI()));
	}
	
	/**
	 * Construct a map of properties to restrictions 
	 * in the context of a given class.
	 */
	public void analyse() {
		props = new OneToManyMap();
		classes = new HashSet();
		baseClass = defaultBase;

		ResIterator it = clss.listSuperClasses(true);
		while( it.hasNext()) {
			OntResource node = it.nextResource();
			if( ! node.isClass() && ! node.isDatatype() && ! node.equals(MESSAGE.Reference)) {
				System.out.println("Superclass not typed:");
				System.out.println(node.describe());
				
			}
			if( node.isClass() && ! node.equals(MESSAGE.Reference)) {
				if(node.isRestriction()) {
					OntResource prop = node.getOnProperty();
					if( prop != null)
						props.put(prop, node);
				}
				else if( ! node.isAnon()) {
					//  its a named, general base class
					if( node.getNameSpace().equals(namespace)) {
						classes.add(node); // locally defined class
					}
					else {
						baseClass = node; // externally defined class (expect only one)
					}
				}
			}
		}
		analyseBaseClass();
	}

	private void analyseBaseClass() {
		if( OWL.Thing.equals(baseClass) && ! clss.isDatatype()) {
			System.out.println("Profile with no schema class:");
			System.out.println( clss.describe());
		}
		enumeratedBase = baseClass.hasProperty(UML.hasStereotype, UML.enumeration);
	}
	
	/**
	 * remove a SomeValueFrom restriction.
	 */
	public void remove(Property prop, OntResource childClass) {
		Iterator it = props.getAll(prop);
		
		while(it.hasNext()) {
		    OntResource res = (OntResource) it.next();
		    if( res.isSomeValuesFromRestriction()) {
		    	Resource type =  res.getSomeValuesFrom();
		    	if( type != null && type.equals(childClass)) 
		    		res.remove();
		    }
		}
	}
	
	/**
	 * Change the type of the node in the underlying ontology.
	 */
	public void setBaseClass(OntResource type) {
		if( ! OWL.Thing.equals(baseClass)) {
			if( baseClass.equals(type))
				return;
			clss.removeSuperClass(baseClass);
		}
		clss.addSuperClass(type);
		baseClass = type;
		analyseBaseClass();
	}

	/**
	 * Remove all restrictions on the given property.
	 */
	public void remove(OntResource prop) {
		Iterator jt = props.getAll(prop);
		while( jt.hasNext()) {
			OntResource res = (OntResource) jt.next();
			res.remove();
		}
		props.remove(prop);
	}
	
	public void setMaxCardinality(int card) {
		if( card < Integer.MAX_VALUE)
			clss.setProperty(UML.hasMaxCardinality, card);
		else
			clss.removeAll(UML.hasMaxCardinality);
	}

	
	public void setMinCardinality(int card) {
		if( card > 0)
			clss.setProperty(UML.hasMinCardinality, card);
		else
			clss.removeAll(UML.hasMinCardinality);
	}
	
	public int getMaxCardinality() {
		Integer card = clss.getInteger(UML.hasMaxCardinality);
		return card != null? card.intValue(): Integer.MAX_VALUE;
	}
	
	public int getMinCardinality() {
		Integer card = clss.getInteger(UML.hasMinCardinality);
		return card != null? card.intValue(): 0;
	}

	private boolean removeCardinality(OntResource prop) {
		boolean removed = false;
		Iterator it = props.getAll(prop);
		while( it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if( res.isCardinalityRestriction()) {
				res.remove();
				it.remove();
				removed = true;
			}
		}
		return removed;
	}

	private void setMaxCardinality(OntResource prop, int card) {
		removeMaxCardinality(prop);
		if( card < Integer.MAX_VALUE) {
			OntResource res = model.createMaxCardinalityRestriction(null, prop, card);
			clss.addSuperClass(res);
			props.put(prop, res);
		}
	}

	private void removeMaxCardinality(OntResource prop) {
		Iterator it = props.getAll(prop);
		while( it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if( res.isMaxCardinalityRestriction()) {
				res.remove();
				it.remove();
			}
		}
	}

	private void setMinCardinality(OntResource prop, int card) {
		removeMinCardinality(prop);
		if( card > 0 ) {
			OntResource res = model.createMinCardinalityRestriction(null, prop, card);
			clss.addSuperClass(res);
			props.put(prop, res);
		}
	}

	private void removeMinCardinality(OntResource prop) {
		Iterator it = props.getAll(prop);
		while( it.hasNext()) {
			OntResource res = (OntResource) it.next();
			if( res.isMinCardinalityRestriction()) {
				res.remove();
				it.remove();
			}
		}
	}

	public void setReference(boolean state) {
		setStereotype(UML.byreference, state);
		clss.removeSuperClass(MESSAGE.Reference); // deprecated
	}
	
	public void setStereotype(Resource stereo, boolean state) {
		if(state)
			clss.addProperty(UML.hasStereotype, stereo);
		else
			clss.removeProperty(UML.hasStereotype, stereo);
	}
	
	public boolean hasStereotype(Resource stereo) {
		return clss.hasProperty(UML.hasStereotype, stereo) || baseClass.hasProperty(UML.hasStereotype, stereo);
	}

	public OntResource createSomeValuesFrom(OntResource prop, OntResource type) {
		OntResource child = model.createClass();
		child.addSuperClass(type);
		String label = type.getLabel(null);
		if( label == null)
			label = type.getLocalName();
		child.addLabel(label, null);
		OntResource res = model.createSomeValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);
		return child;
	}

	public OntResource createAllValuesFrom(OntResource prop, boolean required) {
		OntResource child; 
		if( prop.isDatatypeProperty())
			child = model.createIndividual(RDFS.Datatype); // its not really an individual
		else
			child = model.createClass();
		String label = prop.getLabel(null);
		if( label == null)
			label = prop.getLocalName();
		child.addLabel(label, null);
		
		OntResource res = model.createAllValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);
		
		if(required && canBeRequired(prop)) {
			OntResource req = model.createMinCardinalityRestriction(null, prop, 1);
			clss.addSuperClass(req);
			props.put(prop, req);
		}
		return child;
	}

	public OntResource createSuperClass(OntResource base) {
		OntResource child = model.createClass(namespace + base.getLocalName());
		child.addSuperClass(base);
		return addSuperClass(child);
	}

	public OntResource addSuperClass(OntResource child) {
		clss.addSuperClass(child);
		classes.add(child);
		return child;
	}
	
	public Iterator getIndividuals() {
		if( clss.isEnumeratedClass()) {
			return clss.getOneOf().listResourceElements();
		}
		if( enumeratedBase && classes.size() == 0) {
			return baseClass.listInstances();
		}
		return Collections.EMPTY_LIST.iterator();	
	}
	
	public void setRestrictedEnum( boolean state) {
		if( state && ! clss.isEnumeratedClass()) {
			clss.addProperty(OWL.oneOf, model.createList(baseClass.listInstances()));
		}
		else if( ! state && clss.isEnumeratedClass()){
			OntResource extent = clss.getOneOf();
			clss.removeAll(OWL.oneOf);
			if( ! extent.equals(RDF.nil))
				extent.removeList();
		}
	}
	
	public boolean isRestrictedEnum() {
		return clss.isEnumeratedClass();
	}
	
	public void addIndividual(OntResource indiv) {
		if( ! indiv.hasRDFType(baseClass))
			return;
		
		if( ! clss.isEnumeratedClass()) {
			clss.addProperty(OWL.oneOf, model.createList().cons(indiv));
		}
		else {
			OntResource extent = clss.getOneOf();
			if(! extent.contains(indiv))
				clss.setOneOf(extent.cons(indiv));
		}
	}
	
	public void removeIndividual(OntResource indiv) {
		setRestrictedEnum( true );
		OntResource extent = clss.getOneOf();
		clss.setOneOf(extent.remove(indiv));
	}
	
	public boolean isUnion() {
		return clss.hasProperty(OWL.unionOf);
	}

	public Iterator getProperties() {
		return props.keySet().iterator();
	}
	
	public boolean hasProperty(Property prop) {
		return props.containsKey(prop);
	}

	public Iterator getRestrictions(Property prop) {
		return props.getAll(prop);
	}

	public Iterator getSuperClasses() {
		if(isPropertyRange())
			return Collections.EMPTY_LIST.iterator();
		else
			return classes.iterator();
	}

	public PropertyInfo getPropertyInfo(OntResource prop) {

		PropertyInfo info = new PropertyInfo(clss, prop);
		Iterator jt = props.getAll(prop);
		while(jt.hasNext()) {
			info.scanRestrict((OntResource)jt.next());
		}
		assert info.range != null;
		return info;
	}
	
	public OntResource getSubject() {
		return clss;
	}

	public String getNamespace() {
		return namespace;
	}

	public OntResource getBaseClass() {
		return baseClass;
	}

	public boolean isEnumerated() {
		return enumeratedBase;
	}
	
	public boolean isReference() {
		return hasStereotype(UML.byreference) || clss.hasSuperClass(MESSAGE.Reference, false);
	}

	public class PropertyInfo {
		private OntResource prop;
		private OntResource range;
		private OntResource domain;
		private int min = 0;
		private int max = Integer.MAX_VALUE;

		private PropertyInfo(OntResource domain, OntResource prop) {
			this.prop = prop;
			this.domain = domain;
			if(prop.isFunctionalProperty() || prop.isDatatypeProperty())
				max = 1;
		}

		public OntResource getDomain() {
			return domain;
		}

		public OntResource getProperty() {
			return prop;
		}

		public OntResource getRange() {
			return range;
		}

		public ProfileClass createProfileClass() {
			if( range == null)
				return null;
			
			OntResource type = prop.getRange();
			if( type != null && type.isClass()) 
				return new ProfileClass(range, namespace, type);
			else
				return new ProfileClass(range, namespace);
		}
		
		public ProfileClass getDomainProfile() {
			return ProfileClass.this;
		}

		public boolean isRequired() {
			return min > 0;
		}
		
		public boolean canBeRequired() {
			return ProfileClass.this.canBeRequired(prop);
		}

		public boolean isFunctional() {
			return max == 1;
		}
		
		public boolean isAlwaysFunctional() {
			// TODO: drop datatype term here and in ctor?
			return prop.isFunctionalProperty() || prop.isDatatypeProperty(); 
		}

		public void setMaxCardinality(int card) {
			if( removeCardinality(prop)) 
				ProfileClass.this.setMinCardinality(prop, min);
			ProfileClass.this.setMaxCardinality(prop, card);
			max = card;
		}
		
		public int getMaxCardinality() {
			return max;
		}
		
		public void setMinCardinality(int card) {
			if( removeCardinality(prop)) 
				ProfileClass.this.setMaxCardinality(prop, max);
			ProfileClass.this.setMinCardinality(prop, card);
			min = card;
		}
		
		public int getMinCardinality() {
			return min;
		}
		
		/**
		 * Scan a restriction computing net cardinality for this property.
		 */
		private void scanRestrict(OntResource res) {
			if(res.isAllValuesFromRestriction())
				scanAllValuesFromRestriction(res);
			else if(res.isCardinalityRestriction())
				scanCardinalityRestriction(res);
			else if(res.isMinCardinalityRestriction())
				scanMinCardinalityRestriction(res);
			else if(res.isMaxCardinalityRestriction())
				scanMaxCardinalityRestriction(res);
		}
		
		private void scanAllValuesFromRestriction( OntResource res ) {
			range = res.getAllValuesFrom();
		}
		
		private void scanCardinalityRestriction(OntResource res) {
			if(res.getCardinality() > min)
				min = res.getCardinality();
			if(res.getCardinality() < max)
				max = res.getCardinality();
		}
		
		private void scanMinCardinalityRestriction(OntResource res) {
			if( res.getMinCardinality() > min)
				min = res.getMinCardinality();
		}
		
		private void scanMaxCardinalityRestriction(OntResource res) {
			if(res.getMaxCardinality() < max)
				max = res.getMaxCardinality();
		}
	}
	
	/**
	 * Returns a ProfileClass for each named class.
	 */
	public static Iterator getProfileClasses(final OntModel profileModel, final OntModel fullModel) {
		return new Iterator() {
			List classes = getNamedProfiles(profileModel, fullModel);
			int ix;
			
			public boolean hasNext() {
				return ix < classes.size();
			}

			public Object next() {
				OntResource clss = (OntResource)classes.get(ix++);
				return new ProfileClass(clss, clss.getNameSpace());
			}

			public void remove() {
			}
		};
	}
	
	/**
	 * Return a list of named classes (excluding some named support classes).
	 */
	public static List getNamedProfiles(OntModel profileModel, OntModel fullModel) {
		List classes = new ArrayList();
		ResIterator jt = profileModel.listNamedClasses();
		while( jt.hasNext()) {
			Resource symbol = jt.nextResource();
			if(! symbol.getNameSpace().equals(MESSAGE.NS) && ! symbol.getNameSpace().equals(OWL.NS)) {
				classes.add(fullModel.createResource(symbol.asNode()));
			}
		}
		return classes;
	}

	public boolean canBeRequired(OntResource prop) {
		OntResource domain = prop.getDomain();
		return domain== null || baseClass.hasSuperClass(domain);
	}
	
	/**
	 * Broaden this class by making it a union of its 
	 * present definition and a new profile of the given
	 * base class.   
	 * 
	 * @return the new profile class
	 */
	public OntResource createUnionMember(OntResource base, boolean named) {
		OntResource member;
		
		if(named) {
			member = model.createResource(namespace + base.getLocalName());
			if( member.isClass()) {
				if( member.hasSuperClass(base)) { 
					addUnionMember(member);
					return member;
				}
			}
		}

		member = model.createClass();
		member.addSuperClass(base);
		member.addLabel(base.getLocalName(), null);
		addUnionMember(member);
		return member;
	}

	/**
	 * Broaden this class by making it a union of its 
	 * present definition and the given profile class.   
	 * 
	 */
	public void addUnionMember(OntResource child) {
		OntResource union = clss.getResource(OWL.unionOf);
		
		if( union != null && union.isList()) 
			{}
		else if( isPropertyRange()) 
			union = buildUnion();
		else
			union = model.createList();
				
		union = union.cons(child);
		clss.setProperty(OWL.unionOf, union);
	}

	/**
	 * 
	 * Create an explicit union definition for this class.
	 * 
	 * If the class has profile superclasses these become the union members.
	 * (This is intended for the case of a single superclass that is to be 
	 * broadened.)
	 * 
	 *  If the class has property restrictions, they are moved to a fresh
	 *  anonymous class and that becomes the single union member. 
	 * 
	 * @return an RDFList representing the members of the union.
	 */
	private OntResource buildUnion() {
		OntResource union = model.createList();
		if(! classes.isEmpty()) {
			for (Iterator it = classes.iterator(); it.hasNext();) {
				OntResource sup = (OntResource) it.next();
				clss.removeSuperClass(sup);
				union = union.cons(sup);
			}
			classes = new HashSet();
		}
		if( ! props.isEmpty()) {
			OntResource member = model.createClass();
			member.addSuperClass(baseClass);
			member.addLabel(baseClass.getLocalName(), null);
			
			for (Iterator it = props.keySet().iterator(); it.hasNext();) {
				OntResource prop = (OntResource) it.next();
				for (Iterator iv = props.getAll(prop); iv.hasNext();) {
					OntResource res = (OntResource) iv.next();
					clss.removeSuperClass(res);
					member.addSuperClass(res);
				}
			}
			union = union.cons(member);
			props = new OneToManyMap();
		}
		return union;
	}
	
	private void removeAllProps() {
		for (Iterator it = props.keySet().iterator(); it.hasNext();) {
			OntResource prop = (OntResource) it.next();
			remove(prop);
		}
	}
	
	/** 
	 * Narrow this class by removing one of the union members
	 * of which it is composed.
	 */
	public void removeUnionMember(OntResource child) {
		
		OntResource union = clss.getResource(OWL.unionOf);
		if( union != null && union.isList()) {
			union = union.remove(child);
			clss.setProperty(OWL.unionOf, union);
		}
		else if(isPropertyRange()) {
			if( child.equals(clss)) 
				removeAllProps();
			else 
				removeSuperClass(child);
		}
	}
	
	/**
	 * 
	 * If this class is a union, return its members as ProfileClasses
	 * otherwise return an empty list.
	 * 
	 * The only classes that are may be unions are the anonymous ranges of
	 * restricted properties and these are always regarded as unions.
	 * 
	 * If the class has an explicit unionOf axiom, its declared members are
	 * returned.
	 * 
	 * If the class if a property range and has property restrictions
	 * in turn, then  it is itself regarded as the single member of the union.
	 * 
	 * Otherwise an empty List is returned.
	 * 
	 * 
	 * @return A List of ProfileClass
	 */
	public List getUnionMembers() {
		List members = new ArrayList();
		
		OntResource union = clss.getResource(OWL.unionOf);
		if( union != null && union.isList()) {
			for (ResIterator it = union.listResourceElements(); it.hasNext();) {
				OntResource item = it.nextResource();
				if( item.isClass()) {
					members.add(new ProfileClass(item, namespace));
				}
			}
		}
		else if(isPropertyRange()) {
			for (Iterator it = classes.iterator(); it.hasNext();) 
				members.add(new ProfileClass((OntResource) it.next(), namespace));
			if( ! props.isEmpty())
				members.add(new ProfileClass(clss, namespace, baseClass));
		}
		return members;
	}

	public boolean isPropertyRange() {
		return ! defaultBase.equals(OWL.Thing);
	}

	public void removeSuperClass(OntResource child) {
		if(classes.remove(child))
			clss.removeSuperClass(child);
	}
}