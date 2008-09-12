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

import au.com.langdale.jena.OntSubject;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.CardinalityRestriction;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.EnumeratedClass;
import com.hp.hpl.jena.ontology.MaxCardinalityRestriction;
import com.hp.hpl.jena.ontology.MinCardinalityRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.OneToManyMap;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Represents a class in a profile, encapsulating its base (original) class and restrictions.
 */
public class ProfileClass {
	private OneToManyMap props;
	private OntClass baseClass;
	private Set classes;
	private OntClass clss;
	private String namespace;
	private OntModel model;
	private boolean enumerated;
	private OntClass defaultBase;
	
	public ProfileClass(OntClass clss, String namespace, OntModel model, OntClass base) {
		this.clss = clss;
		this.namespace = namespace;
		this.model = model;
		this.defaultBase = base;
		analyse();
	}
	
	public ProfileClass(OntClass clss, String namespace, OntModel model) {
		this(clss, namespace, model, null);
	}
	
	public ProfileClass(OntClass clss) {
		this(clss, clss.getNameSpace(), clss.getOntModel());
	}
	
	/**
	 * Construct a map of properties to restrictions 
	 * in the context of a given class.
	 */
	public void analyse() {
		props = new OneToManyMap();
		classes = new HashSet();
		baseClass = defaultBase;

		Iterator it = new OntSubject(clss).listSuperClasses(true);
		while( it.hasNext()) {
			OntResource node = (OntResource) it.next();
			if( node.canAs(OntClass.class) && ! node.equals(MESSAGE.Reference)) {
				OntClass parent = (OntClass) node.as(OntClass.class);
				if(parent.isRestriction()) {
					Restriction res = parent.asRestriction();
					try {
						props.put(res.getOnProperty(), res);
					}
					catch (ConversionException e) {
						// ignore an undefined property
					}
				}
				else if( ! parent.isAnon()) {
					//  its a named, general base class
					if( parent.getNameSpace().equals(namespace)) {
						classes.add(parent); // locally defined class
					}
					else {
						baseClass = parent; // externally defined class (expect only one)
					}
				}
			}
		}
		analyseBaseClass();
	}

	private void analyseBaseClass() {
		enumerated = baseClass != null && baseClass.hasProperty(UML.hasStereotype, UML.enumeration);
	}
	
	/**
	 * remove a SomeValueFrom restriction.
	 */
	public void remove(Property prop, OntClass childClass) {
		Iterator it = props.getAll(prop);
		
		while(it.hasNext()) {
		    Restriction res = (Restriction) it.next();
		    if( res.isSomeValuesFromRestriction()) {
		    	SomeValuesFromRestriction some = res.asSomeValuesFromRestriction();
		    	Resource type =  some.getSomeValuesFrom();
		    	if( type != null && type.equals(childClass)) 
		    		some.remove();
		    }
		}
	}
	
	/**
	 * Change the type of the node in the underlying ontology.
	 */
	public void setBaseClass(OntResource type) {
		if( baseClass != null) {
			if( baseClass.equals(type))
				return;
			clss.removeSuperClass(baseClass);
		}
		clss.addSuperClass(type);
		baseClass = type.asClass();
		analyseBaseClass();
	}

	/**
	 * Remove all restrictions on the given property.
	 */
	public void remove(Property prop) {
		Iterator jt = props.getAll(prop);
		while( jt.hasNext()) {
			Restriction res = (Restriction) jt.next();
			res.remove();
		}
		props.remove(prop);
	}

	private boolean removeCardinality(OntProperty prop) {
		boolean removed = false;
		Iterator it = props.getAll(prop);
		while( it.hasNext()) {
			Restriction res = (Restriction) it.next();
			if( res.isCardinalityRestriction()) {
				res.remove();
				it.remove();
				removed = true;
			}
		}
		return removed;
	}

	private void setMaxCardinality(OntProperty prop, int card) {
		removeMaxCardinality(prop);
		if( card < Integer.MAX_VALUE) {
			Restriction res = model.createMaxCardinalityRestriction(null, prop, card);
			clss.addSuperClass(res);
			props.put(prop, res);
		}
	}

	private void removeMaxCardinality(OntProperty prop) {
		Iterator it = props.getAll(prop);
		while( it.hasNext()) {
			Restriction res = (Restriction) it.next();
			if( res.isMaxCardinalityRestriction()) {
				res.remove();
				it.remove();
			}
		}
	}

	private void setMinCardinality(OntProperty prop, int card) {
		removeMinCardinality(prop);
		if( card > 0 ) {
			Restriction res = model.createMinCardinalityRestriction(null, prop, card);
			clss.addSuperClass(res);
			props.put(prop, res);
		}
	}

	private void removeMinCardinality(OntProperty prop) {
		Iterator it = props.getAll(prop);
		while( it.hasNext()) {
			Restriction res = (Restriction) it.next();
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
		return clss.hasProperty(UML.hasStereotype, stereo);
	}

	public OntClass createSomeValuesFrom(Property prop, OntResource type) {
		OntClass child = model.createClass();
		child.addSuperClass(type);
		String label = type.getLabel(null);
		if( label == null)
			label = type.getLocalName();
		child.setLabel(label, null);
		SomeValuesFromRestriction res = model.createSomeValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);
		return child;
	}

	public OntResource createAllValuesFrom(OntProperty prop, boolean required) {
		OntResource child; 
		if( prop.isDatatypeProperty())
			child = model.createIndividual(RDFS.Datatype); // its not really an individual
		else
			child = model.createClass();
		String label = prop.getLabel(null);
		if( label == null)
			label = prop.getLocalName();
		child.setLabel(label, null);
		
		Restriction res = model.createAllValuesFromRestriction(null, prop, child);
		clss.addSuperClass(res);
		props.put(prop, res);
		
		if(required && canBeRequired(prop)) {
			Restriction req = model.createMinCardinalityRestriction(null, prop, 1);
			clss.addSuperClass(req);
			props.put(prop, req);
		}
		return child;
	}

	public OntResource createSuperClass(OntResource base) {
		OntClass child = model.createClass(namespace + base.getLocalName());
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
			EnumeratedClass enated = clss.asEnumeratedClass();
			return enated.listOneOf();
		}
		if( enumerated && classes.size() == 0) {
			return baseClass.listInstances();
		}
		return Collections.EMPTY_LIST.iterator();	
	}
	
	public void setRestrictedEnum( boolean state) {
		if( state && ! clss.isEnumeratedClass()) {
			clss.addProperty(OWL.oneOf, model.createList(baseClass.listInstances()));
		}
		else if( ! state && clss.isEnumeratedClass()){
			EnumeratedClass enated = clss.asEnumeratedClass();
			RDFList extent = enated.getOneOf();
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
			EnumeratedClass enated = clss.asEnumeratedClass();
			RDFList extent = enated.getOneOf();
			if(! extent.contains(indiv))
				enated.setOneOf(extent.cons(indiv));
		}
	}
	
	public void removeIndividual(OntResource indiv) {
		setRestrictedEnum( true );
		EnumeratedClass enated = clss.asEnumeratedClass();
		RDFList extent = enated.getOneOf();
		enated.setOneOf(extent.remove(indiv));
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

	public PropertyInfo getPropertyInfo(OntProperty prop) {

		PropertyInfo info = new PropertyInfo(clss, prop);
		Iterator jt = props.getAll(prop);
		while(jt.hasNext()) {
			info.scanRestrict((Restriction)jt.next());
		}
		return info;
	}
	
	public OntClass getSubject() {
		return clss;
	}

	public String getNamespace() {
		return namespace;
	}

	public OntClass getBaseClass() {
		return baseClass;
	}

	public boolean isEnumerated() {
		return enumerated;
	}
	
	public boolean isReference() {
		return hasStereotype(UML.byreference) || clss.hasSuperClass(MESSAGE.Reference, true);
	}

	public class PropertyInfo {
		private OntProperty prop;
		private OntClass range;
		private OntClass domain;
		private int min = 0;
		private int max = Integer.MAX_VALUE;

		private PropertyInfo(OntClass domain, OntProperty prop) {
			this.prop = prop;
			this.domain = domain;
			if(prop.isFunctionalProperty() || prop.isDatatypeProperty())
				max = 1;
		}

		public OntClass getDomain() {
			return domain;
		}

		public OntProperty getProperty() {
			return prop;
		}

		public OntClass getRange() {
			return range;
		}

		public ProfileClass createProfileClass() {
			if( range == null)
				return null;
			
			OntResource type = prop.getRange();
			if( type != null && type.canAs(OntClass.class)) 
				return new ProfileClass(range, namespace, model, type.asClass());
			else
				return new ProfileClass(range, namespace, model);
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
		private void scanRestrict(Restriction res) {
			if(res.isAllValuesFromRestriction())
				scanRestrict(res.asAllValuesFromRestriction());
			else if(res.isCardinalityRestriction())
				scanRestrict(res.asCardinalityRestriction());
			else if(res.isMinCardinalityRestriction())
				scanRestrict(res.asMinCardinalityRestriction());
			else if(res.isMaxCardinalityRestriction())
				scanRestrict(res.asMaxCardinalityRestriction());
		}
		private void scanRestrict(AllValuesFromRestriction res) {
			Resource type = res.getAllValuesFrom();
			if( type.canAs(OntClass.class))
				range = (OntClass) type.as(OntClass.class);
		}
		
		private void scanRestrict(CardinalityRestriction res) {
			if(res.getCardinality() > min)
				min = res.getCardinality();
			if(res.getCardinality() < max)
				max = res.getCardinality();
		}
		
		private void scanRestrict(MinCardinalityRestriction res) {
			if( res.getMinCardinality() > min)
				min = res.getMinCardinality();
		}
		
		private void scanRestrict(MaxCardinalityRestriction res) {
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
				return new ProfileClass((OntClass)classes.get(ix++));
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
		Iterator jt = profileModel.listNamedClasses();
		while( jt.hasNext()) {
			OntClass symbol = (OntClass) jt.next();
			if(! symbol.getNameSpace().equals(MESSAGE.NS)) {
				classes.add(fullModel.getOntClass(symbol.getURI()));
			}
		}
		return classes;
	}

	public boolean canBeRequired(OntProperty prop) {
		OntResource domain = prop.getDomain();
		return domain== null || baseClass.hasSuperClass(domain);
	}
	
	public OntResource createUnionMember(OntResource base, boolean named) {
		if(named) {
			OntResource symbol = model.createOntResource(namespace + base.getLocalName());
			if( symbol.isClass()) {
				OntClass member = symbol.asClass();
				if( member.hasSuperClass(base)) 
					return addUnionMember(member);
			}
		}

		OntClass member = model.createClass();
		member.addSuperClass(base);
		member.setLabel(base.getLocalName(), null);
		return addUnionMember(member);
	}

	public OntResource addUnionMember(OntResource child) {
//		clss.addSubClass(child);
		
		RDFNode value = clss.getPropertyValue(OWL.unionOf);
		RDFList union;
		
		if( value != null && value.canAs(RDFList.class)) 
			union = (RDFList) value.as(RDFList.class);
		else if( isPropertyRange()) 
			union = buildUnion();
		else
			union = model.createList();
				
		union = union.cons(child);
		clss.setPropertyValue(OWL.unionOf, union);

		return child;
	}

	private RDFList buildUnion() {
		RDFList union = model.createList();
		if(! classes.isEmpty()) {
			for (Iterator it = classes.iterator(); it.hasNext();) {
				OntResource sup = (OntResource) it.next();
				clss.removeSuperClass(sup);
				union = union.cons(sup);
			}
			classes = new HashSet();
		}
		if( ! props.isEmpty()) {
			OntClass member = model.createClass();
			member.addSuperClass(baseClass);
			member.addLabel(baseClass.getLocalName(), null);
			
			for (Iterator it = props.keySet().iterator(); it.hasNext();) {
				OntProperty prop = (OntProperty) it.next();
				for (Iterator iv = props.getAll(prop); iv.hasNext();) {
					Restriction res = (Restriction) iv.next();
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
			OntProperty prop = (OntProperty) it.next();
			remove(prop);
		}
	}
	
	public void removeUnionMember(OntResource child) {
//		clss.removeSubClass(child);
		
		RDFNode value = clss.getPropertyValue(OWL.unionOf);
		if( value != null && value.canAs(RDFList.class)) {
			RDFList union = (RDFList) value.as(RDFList.class);
			union = union.remove(child);
			clss.setPropertyValue(OWL.unionOf, union);
		}
		else if(isPropertyRange()) {
			if( child.equals(clss)) 
				removeAllProps();
			else 
				removeSuperClass(child);
		}
	}
	
	public List getUnionMembers() {
		List members = new ArrayList();
		
		RDFNode value = clss.getPropertyValue(OWL.unionOf);
		if( value != null && value.canAs(RDFList.class)) {
			RDFList union = (RDFList) value.as(RDFList.class);
			for (Iterator it = union.iterator(); it.hasNext();) {
				RDFNode item = (RDFNode) it.next();
				if( item.canAs(OntClass.class)) {
					members.add(new ProfileClass((OntClass)item.as(OntClass.class)));
				}
			}
		}
		else if(isPropertyRange()) {
			for (Iterator it = classes.iterator(); it.hasNext();) 
				members.add(new ProfileClass((OntClass) it.next()));
			if( ! props.isEmpty())
				members.add(new ProfileClass(clss, namespace, model, baseClass));
		}
		return members;
	}

	public boolean isPropertyRange() {
		return defaultBase != null;
	}

	public void removeSuperClass(OntResource child) {
		if(classes.remove(child))
			clss.removeSuperClass(child);
	}
}