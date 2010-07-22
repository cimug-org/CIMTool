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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.util.MultiMap;

import au.com.langdale.kena.OntResource;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Utilities used in analysing and manipulating profile models.
 */
public class ProfileUtility {
	/**
	 * A multi-map from base classes to profile classes.
	 */
	public static class BaseMap extends MultiMap {
		public void add(OntResource base, OntResource clss) {
			putRaw(base, clss);
		}
	}
	
	/**
	 * A two-way mapping between base classes and profile classes.
	 */
	public static class ProfileMap extends BaseMap {
		private Map profiles = new HashMap();   // profile to base class
		
		@Override
		public void add(OntResource base, OntResource clss) {
			// record the back trace
			super.add(base, clss);
			// record the profile to base mapping
			profiles.put(clss, base);
		}
		
		public void removeProfile(OntResource clss) {
			OntResource base = getBase(clss);
			profiles.remove(clss);
			super.remove(base, clss);
		}

		public OntResource getBase(OntResource clss) {
			return (OntResource) profiles.get(clss);
		}

		/**
		 * Select the preferred profile from a collection of profiles.
		 * The preferred profile has the same local name as the base class.
		 * 
		 * @param profiles: the candidate profile classes
		 * @return the preferred profile if any, 
		 * otherwise a random profile or <code>null</code> if there are none
		 */
		public OntResource chooseBestProfile(Collection profiles) {
			Iterator it = profiles.iterator();
			OntResource any = null;
			while (it.hasNext()) {
				OntResource cand = (OntResource) it.next();
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
		public OntResource chooseBestProfile(OntResource base) {
			return chooseBestProfile(find(base));
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
		public Set findRelatedProfiles(OntResource base, boolean subclass, boolean unique) {
			HashSet result = new HashSet();
			
			// consider relatives of the base
			OntResource subject = base;
			Iterator it = subclass? subject.listSubClasses(false): subject.listSuperClasses(false);
			while (it.hasNext()) {
				OntResource related = (OntResource) it.next();
				if( related.isClass() && !related.equals(base)) {
					
					// consider the profiles of each relative 
					Collection cands = find(related);
					
					if( ! cands.isEmpty()) {
						if(unique) {
							// add 'best' profile for this base
							addBestClass(result, chooseBestProfile(cands), subclass);
						}
						else {
							// add all candidate profiles
							Iterator jt = cands.iterator();
							while (jt.hasNext()) 
								addBestClass(result, (OntResource) jt.next(), subclass);
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
	public static class PropertyGroup {
		private final OntResource prop;
		private PropertySpec summary;
		private Collection restrictions = new LinkedList();
		public PropertyGroup(PropertySpec spec) {
			summary = spec;
			prop = spec.prop;
			restrictions.add(spec);
		}
		public OntResource getProperty() {
			return prop;
		}
		public PropertySpec getSummary() {
			return summary;
		}
		public Collection getRestrictions() {
			return restrictions;
		}
		
		public void add(PropertySpec other) {
			mergeSummary(other);
			addRestriction(other);
		}
		
		private void mergeSummary(PropertySpec other) {
			if( summary.base_domain.equals(other.base_domain)) {
				summary = new PropertySpec(summary, other);
			}
			else {
				PropertySpec dominant = summary.selectDominant(other);
				if( dominant != null) 
					summary = dominant;
				else 
					summary = new PropertySpec(prop, null, null);
			}
		}
		
		private void addRestriction(PropertySpec other) {
			for (Iterator jt = restrictions.iterator(); jt.hasNext();) {
				PropertySpec extant = (PropertySpec) jt.next();
				if( other.base_domain.equals(extant.base_domain) ) {
					other = new PropertySpec(extant, other);
					jt.remove();
					break;
				}
			}
			restrictions.add(other);
		}
	}	
	
	/**
	 * A specification of a property profile. 
	 */
	public static class PropertySpec {
		public final OntResource prop;
		public final boolean required, functional, reference;
		public final OntResource base_range, base_domain; // FIXME: base_range should be OntResource
		public final String label, comment;

		public PropertySpec(PropertyInfo info, ProfileClass range_profile) {
			prop = info.getProperty();
			required = info.isRequired();
			functional = info.isFunctional();
			reference = range_profile != null && range_profile.isReference();

			// repair domain and range
			base_domain = selectType(prop.getDomain(), info.getDomainProfile().getBaseClass());
			base_range = selectType(prop.getRange(), range_profile != null? range_profile.getBaseClass(): null);

			OntResource range = info.getRange();
			String l = range != null? range.getLabel(null): null;
			if( l != null )
				label = l;
			else
				label = prop.getLabel(null);

			comment = extractComment(range);
		}
		
		public PropertySpec(OntResource prop, OntResource domain, OntResource range) {
			this.prop = prop;
			required = reference = false;
			functional = prop.isFunctionalProperty() || prop.isDatatypeProperty();
			base_domain = selectType(prop.getDomain(), domain);
			base_range = selectType(prop.getRange(), range);
			label = prop.getLabel(null);
			comment = "";
		}

		/**
		 * Merge two property specifications
		 */
		private PropertySpec(PropertySpec lhs, PropertySpec rhs) {
			prop = lhs.prop; // == rhs.prop
			base_domain = lhs.base_domain; // == rhs.base_domain
			
			// take the greater restriction
			functional = lhs.functional || rhs.functional;
			reference = lhs.reference || rhs.reference;
			required = lhs.required || rhs.required;
			base_range = mergeRange(prop.getRange(), lhs.base_range, rhs.base_range);
			
			// take the profile label if both sides agree
			if( lhs.label.equals(rhs.label))
				label = lhs.label;
			else
				label = prop.getLabel(null);
			
			comment = "";
		}

		private PropertySpec selectDominant(PropertySpec other) {
			// choose the narrowest class
			if( base_domain != null && other.base_domain != null) {
				if( base_domain.hasSuperClass(other.base_domain))
					return other;
				else if( other.base_domain.hasSuperClass(base_domain))
					return this;
			}
			
			return null;
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
	public static class PropertyAccumulator extends MultiMap {
		private Map props = new HashMap(); // property to property collection
	
		public void add(PropertySpec spec) {
			PropertyGroup extant = (PropertyGroup)props.get(spec.prop);
			if( extant != null )
				extant.add(spec);
			else
				props.put(spec.prop, new PropertyGroup(spec));
		}
		
		public void add(OntResource prop, OntResource domain, OntResource range ) {
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
		
		public Collection getGroups() {
			return props.values();
		}
	}
	
	public static class EnumAccumulator {
		private Map enums = new HashMap();
		
		public void add(OntResource base, Iterator insts) {
			Set extent = creatExtent(base);
			while (insts.hasNext()) {
				extent.add((OntResource) insts.next());
			}
		}
		
		public void add(OntResource base, OntResource inst) {
			Set extent = creatExtent(base);
			extent.add(inst);
		}

		private Set creatExtent(OntResource base) {
			Set extent = (Set) enums.get(base);
			if( extent == null) {
				extent = new HashSet();
				enums.put(base, extent);
			}
			return extent;
		}
		
		public Collection get(OntResource base) {
			Set extent = (Set) enums.get(base);
			if( extent == null)
				return Collections.EMPTY_SET;
			else
				return extent;
		}
	}

	private static OntResource selectType(OntResource prop_type, OntResource profile_type) {
		if( prop_type != null && prop_type.hasRDFType(OWL.Class)) {
			if (profile_type != null && (
					profile_type.hasSuperClass(prop_type) 
					|| profile_type.hasSubClass(prop_type)))
				return profile_type;
			else
				return prop_type;
		}
		else
			return profile_type;
	}

	private static OntResource mergeRange(OntResource type, OntResource lhs, OntResource rhs) {

		// choose the narrowest class
		if( lhs != null && rhs != null) {
			if( lhs.equals(rhs))
				return lhs;
			if( lhs.hasSuperClass(rhs))
				return lhs;
			else if( rhs.hasSuperClass(lhs))
				return rhs;
		}

		// choose the base class or null for a datatype
		if( type != null && type.hasRDFType(OWL.Class))
			return type;
		else
			return null;
	}

	/**
	 * Add class to set of classes if it is not less (not greater) than any member.
	 * Remove any members less (greater) than the class.
	 */
	public static void addBestClass(Set result, OntResource clss, boolean greater) {
		if(! result.contains(clss)) {
		
			// compare candidate with each extant result
			Iterator kt = result.iterator();
			while (kt.hasNext()) {
				OntResource extant = (OntResource) kt.next();
				
				
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
