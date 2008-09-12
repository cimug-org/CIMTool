/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import au.com.langdale.jena.OntSubject;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Utilities used in analysing and manipulating profile models.
 */
public class ProfileUtility {
	/**
	 * A multi-map from base classes to profile classes.
	 */
	public static class BaseMap {
		private Map trace = new HashMap(); 		// base class to set of original profile classes
		
		public void add(OntClass base, OntClass clss) {
			// record the back trace
			Set traces = (Set) trace.get(base);
			if( traces == null ) {
				traces = new HashSet();
				trace.put(base, traces);
			}
			traces.add(clss);
		}

		public Collection findProfiles(OntClass base) {
			Set traces = (Set) trace.get(base);
			if( traces == null ) 
				return Collections.EMPTY_SET;

			return traces;
		}
		
		public void remove(OntClass base, OntClass clss) {
			findProfiles(base).remove(clss);
		}
	}
	
	/**
	 * A two-way mapping between base classes and profile classes.
	 */
	public static class ProfileMap extends BaseMap {
		private Map profiles = new HashMap();   // profile to base class
		
		@Override
		public void add(OntClass base, OntClass clss) {
			// record the back trace
			super.add(base, clss);
			// record the profile to base mapping
			profiles.put(clss, base);
		}
		
		public void removeProfile(OntClass clss) {
			OntClass base = getBase(clss);
			profiles.remove(clss);
			super.remove(base, clss);
		}

		public OntClass getBase(OntClass clss) {
			return (OntClass) profiles.get(clss);
		}

		/**
		 * Select the preferred profile from a collection of profiles.
		 * The preferred profile has the same local name as the base class.
		 * 
		 * @param profiles: the candidate profile classes
		 * @return the preferred profile if any, 
		 * otherwise a random profile or <code>null</code> if there are none
		 */
		public OntClass chooseBestProfile(Collection profiles) {
			Iterator it = profiles.iterator();
			OntClass any = null;
			while (it.hasNext()) {
				OntClass cand = (OntClass) it.next();
				if(cand.getLocalName().equals(getBase(cand).getLocalName())) 
					return cand;
				any = cand;
			}
			
			return any;
		}
		
		/**
		 * Find the preferred profile for a base class.
		 * 
		 * @param base: the base class
		 * @return the preferred profile
		 */
		public OntClass chooseBestProfile(OntClass base) {
			return chooseBestProfile(findProfiles(base));
		}

		/**
		 * Construct a set of profile classes related to an information class.
		 * 
		 * The result is a set of profile classes.  If subclass is true, these will be
		 * profiles of subclasses of the base class, otherwise profiles of superclasses. 
		 * 
		 * Only the direct sub or super classes will be included.  
		 * 
		 * If unique is true there will be only be one profile class in the result for each
		 * information class.
		 *  
		 */
		public Set findRelatedProfiles(OntClass base, boolean subclass, boolean unique) {
			HashSet result = new HashSet();
			
			// consider relatives of the base
			OntSubject subject = new OntSubject(base);
			Iterator it = subclass? subject.listSubClasses(false): subject.listSuperClasses(false);
			while (it.hasNext()) {
				OntResource related = (OntResource) it.next();
				if( related.isClass() && !related.equals(base)) {
					
					// consider the profiles of each relative 
					Collection cands = findProfiles(related.asClass());
					
					if( ! cands.isEmpty()) {
						if(unique) {
							// add 'best' profile for this base
							addBestClass(result, chooseBestProfile(cands), subclass);
						}
						else {
							// add all candidate profiles
							Iterator jt = cands.iterator();
							while (jt.hasNext()) 
								addBestClass(result, (OntClass) jt.next(), subclass);
						}
					}
				}
			}
			return result;
		}
	}

	/**
	 * A specification of a property profile. 
	 */
	public static class PropertySpec {
		public final OntProperty prop;
		public final boolean required, functional, reference;
		public final OntClass base_range, base_domain; // FIXME: base_range should be OntResource
		public final String label, comment;

		public PropertySpec(PropertyInfo info, ProfileClass range_profile) {
			prop = info.getProperty();
			required = info.isRequired();
			functional = info.isFunctional();
			reference = range_profile != null && range_profile.isReference();

			// repair domain and range
			base_domain = selectType(prop.getDomain(), info.getDomainProfile().getBaseClass());
			base_range = selectType(prop.getRange(), range_profile != null? range_profile.getBaseClass(): null);

			String l = info.getRange().getLabel(null);
			if( l != null )
				label = l;
			else
				label = prop.getLabel(null);

			comment = extractComment(info.getRange());
		}
		
		public PropertySpec(OntProperty prop, OntClass domain, OntClass range) {
			this.prop = prop;
			required = reference = false;
			functional = prop.isFunctionalProperty() || prop.isDatatypeProperty();
			base_domain = selectType(prop.getDomain(), domain);
			base_range = selectType(prop.getRange(), range);
			label = prop.getLabel(null);
			comment = prop.getComment(null);
		}

		public PropertySpec(PropertySpec lhs, PropertySpec rhs) {
			prop = lhs.prop;
			functional = lhs.functional & rhs.functional;
			required = lhs.required & rhs.required;
			reference = lhs.reference || rhs.reference;

			// merge domain and range
			base_domain = mergeTypes(prop.getDomain(), lhs.base_domain, rhs.base_domain);
			base_range = mergeTypes(prop.getRange(), lhs.base_range, rhs.base_range);

			if( lhs.label.equals(rhs.label))
				label = lhs.label;
			else
				label = prop.getLabel(null);

			comment = appendComment(lhs.comment, rhs.comment);
		}

		public void create(ProfileClass profile) {
			profile.createAllValuesFrom(prop, required);
			
			profile.setReference(reference);// FIXME: is this right?
			
			PropertyInfo info = profile.getPropertyInfo(prop);
			if(functional)
				info.setMaxCardinality(1);
			if( label != null )
				info.getRange().setLabel(label, null);
			if( comment != null )
				info.getRange().setComment(comment, null);
			
			OntResource native_range = prop.getRange();
			if( base_range != null && (native_range == null || ! base_range.equals(native_range)))
				info.getRange().addSuperClass(base_range);
		}
	}

	/**
	 * A mapping of property to <code>ProertySpec</code>.  As property profiles
	 * are added in the form of <code>PropertyInfo</code> objects, a
	 * <code>PropertySpec</code> is constructed for each property that is
	 * a superset of all the properties profiles. 
	 * 
	 */
	public static class PropertyAccumulator {
		private Map props = new HashMap(); // property to property info
	
		public void add(PropertySpec spec) {
			PropertySpec extant = (PropertySpec)props.get(spec.prop);
			if( extant != null )
				spec = new PropertySpec(spec, extant);
			props.put(spec.prop, spec);
		}
		
		public void add(OntProperty prop, OntClass domain, OntClass range ) {
			add(new PropertySpec(prop, domain, range));
		}
		
		public ProfileClass add(PropertyInfo info) {
			ProfileClass range_profile = null;
			if( ! info.getProperty().isDatatypeProperty()) 
				range_profile = info.createProfileClass();

			PropertySpec spec = new PropertySpec(info, range_profile);
			add(spec);
			return range_profile;	
		}
		
		public PropertySpec get(OntResource prop) {
			return (PropertySpec) props.get(prop);
		}
		
		public boolean containsKey(OntResource prop) {
			return props.containsKey(prop);
		}
		
		public Collection getAll() {
			return props.values();
		}
	}
	
	public static class EnumAccumulator {
		private Map enums = new HashMap();
		
		public void add(OntClass base, Iterator insts) {
			Set extent = creatExtent(base);
			while (insts.hasNext()) {
				extent.add((OntResource) insts.next());
			}
		}
		
		public void add(OntClass base, OntResource inst) {
			Set extent = creatExtent(base);
			extent.add(inst);
		}

		private Set creatExtent(OntClass base) {
			Set extent = (Set) enums.get(base);
			if( extent == null) {
				extent = new HashSet();
				enums.put(base, extent);
			}
			return extent;
		}
		
		public Collection get(OntClass base) {
			Set extent = (Set) enums.get(base);
			if( extent == null)
				return Collections.EMPTY_SET;
			else
				return extent;
		}
	}

	public static OntClass selectType(OntResource prop_type, OntClass profile_type) {
		if( prop_type != null && prop_type.hasRDFType(OWL.Class)) {
			if (profile_type != null && (
					profile_type.hasSuperClass(prop_type) 
					|| profile_type.hasSubClass(prop_type)))
				return profile_type;
			else
				return prop_type.asClass();
		}
		else
			return profile_type;
	}

	public static OntClass mergeTypes(OntResource type, OntClass lhs, OntClass rhs) {

		// choose the broadest class
		if( lhs != null && rhs != null) {
			if( lhs.equals(rhs))
				return lhs;
			if( lhs.hasSuperClass(rhs))
				return rhs;
			else if( rhs.hasSuperClass(lhs))
				return lhs;
		}

		// choose the base class or null for a datatype
		if( type != null && type.hasRDFType(OWL.Class))
			return type.asClass();
		else
			return null;
	}

	/**
	 * Add class to set of classes if it is not less (not greater) than any member.
	 * Remove any members less (greater) than the class.
	 */
	public static void addBestClass(Set result, OntClass clss, boolean greater) {
		if(! result.contains(clss)) {
		
			// compare candidate with each extant result
			Iterator kt = result.iterator();
			while (kt.hasNext()) {
				OntClass extant = (OntClass) kt.next();
				
				
				if( greater ) {
					
					// if candidate greater extant then remove extant
					if( clss.hasSubClass(extant)) {
						kt.remove();
					}
					
					// if candidate less then ignore it
					else if( clss.hasSuperClass(extant)) {
						return;
					}
					
				}
				
				// super instead of subclass
				else {
					if( clss.hasSuperClass(extant)) {
						kt.remove();
					}
					else if( clss.hasSubClass(extant)) {
						return;
					}
				}
			}

			result.add(clss);
		}
	}

	public static String appendComment(String comment, OntResource subject) {
		if( subject == null)
			return comment;

		return appendComment(comment, subject.getComment(null));
	}

	public static String appendComment(String comment, String addendum) {
		if( addendum == null || addendum.length() == 0)
			return comment;
		if( comment == null || comment.length() == 0)
			return addendum;

		return comment + "\n" + addendum;
	}

	public static String extractComment(OntResource subject) {
		return appendComment(null, subject);
	}

}
