/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import au.com.langdale.jena.Models;
import au.com.langdale.jena.OntSubject;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Add or remove a profile class from the profile class hierarchy with side effects.
 */
public class Refactory extends ProfileUtility {
	private OntModel model;
	private ProfileMap map;
	private String namespace;
	private OntModel profileModel;
	
	public Refactory(OntModel profileModel, OntModel backgroundModel, String namespace) {
		this.profileModel = profileModel;
		this.model = Models.merge(profileModel, backgroundModel);
		this.namespace = namespace;
	}
	
	public void add(ProfileClass profile) {
		if( map == null)
			buildMap();
		
		OntClass base = profile.getBaseClass();
		OntClass clss = profile.getSubject();
		map.add(base, clss);
		
		Set supers = map.findRelatedProfiles(base, false, true);
		Set subs = map.findRelatedProfiles(base, true, false);
		Set affected = new HashSet();
		Map profiles = new HashMap();
		
		// consider each super profile
		Iterator jt = supers.iterator();
		while (jt.hasNext()) {
			OntClass parent = (OntClass) jt.next();
			
			// inherit it
			clss.addSuperClass(parent);
			
			// unlink its sub profiles and mark them
			Iterator it = subs.iterator();
			while (it.hasNext()) {
				OntClass sub = (OntClass) it.next();
				if( sub.hasSuperClass(parent)) {
					sub.removeSuperClass(parent);
					affected.add(sub);
				}
			}
		}
		
		// consider each sub profile
		Iterator it = subs.iterator();
		while (it.hasNext()) {
			OntClass sub = (OntClass) it.next();
			ProfileClass subprof = new ProfileClass(sub);

			// mark it if has no supers
			Iterator kt = subprof.getSuperClasses();
			if( ! kt.hasNext()) {
				affected.add(sub);
			}
			
			// build a profileclass for any marked sub profile
			if( affected.contains(sub))
				profiles.put(sub, subprof);
		}
		
		// TODO: we could move properties around
		//PropertyAccumulator props = new PropertyAccumulator();

		// relink sub profiles
		Iterator ht = affected.iterator();
		while (ht.hasNext()) {
			OntClass sub = (OntClass) ht.next();
			ProfileClass subprof = (ProfileClass) profiles.get(sub);
			subprof.addSuperClass(clss);
			
			//removeProps(subprof, base, props);
		}
		
		//addProps(profile, props);
	}
	
	public void remove(ProfileClass profile) {
		if( map == null)
			buildMap();

		OntClass clss = profile.getSubject();
		map.removeProfile(clss);
		
		// super profiles to be inherited by sub profiles
		Set supers = asSet(clss.listSuperClasses(true));
		
		// TODO: properties to be duplicated in sub profiles
		//PropertyAccumulator props = new PropertyAccumulator();
		//collectProps(profile, props);
		
		// consider sub profiles
		Set subs = asSet(clss.listSubClasses(true));
		Iterator it = subs.iterator();
		while (it.hasNext()) {
			OntClass sub = (OntClass) it.next();

			// unlink superclass
			sub.removeSuperClass(clss);

			// aquire its properties
			//addProps(new ProfileClass(sub), props);
			
			// link its superclasses
			Iterator jt = supers.iterator();
			while (jt.hasNext()) 
				sub.addSuperClass((OntClass) jt.next());
		}
	}
	
	private void buildMap() {
		map = new ProfileMap();
		Iterator it = ProfileClass.getProfileClasses(profileModel, model);
		while (it.hasNext()) {
			ProfileClass profile = (ProfileClass) it.next();
			map.add(profile.getBaseClass(), profile.getSubject());
		}
	}
	
	public void setByReference() {
		Iterator it = ProfileClass.getProfileClasses(profileModel, model);
		while (it.hasNext()) {
			ProfileClass profile = (ProfileClass) it.next();
			setByReference(profile);
		}
	}
	
	private void setByReference(ProfileClass profile) {
		Iterator it = profile.getProperties();
		while (it.hasNext()) {
			OntProperty prop = (OntProperty) it.next();
			if( prop.isObjectProperty()) {
				PropertyInfo info = profile.getPropertyInfo(prop);
				info.createProfileClass().setReference(true);
			}
		}
	}
	
	public void setConcrete() {
		Iterator it = ProfileClass.getProfileClasses(profileModel, model);
		while (it.hasNext()) {
			ProfileClass profile = (ProfileClass) it.next();
			profile.setStereotype(UML.concrete, shouldBeConcrete(profile));
		}
	}
	
	private boolean shouldBeConcrete(ProfileClass profile) {
		if( profile.isEnumerated())
			return false;
		
		ExtendedIterator it = new OntSubject(profile.getSubject()).listSubClasses(false);
		while( it.hasNext()) {
			OntResource sub = (OntResource) it.next();
			if(! sub.equals(OWL.Nothing) && ! sub.isAnon()) {
				it.close();
				return false;
			}
		}
		
		return true;
	}
	
	public static Set asSet(Iterator it) {
		Set result = new HashSet();
		while (it.hasNext()) 
			result.add(it.next());
		return result;
	}

	public void convert(ProfileClass profile, boolean named) {
		if( ! profile.getSubject().isAnon()) 
			return;
		
		boolean hasNamed = hasNamedSuper(profile);
		
		if( !hasNamed && named)
			convertToNamed(profile);
		else if(hasNamed && !named)
			convertToUnnamed(profile);
	}

	public boolean hasNamedSuper(ProfileClass profile) {
		return profile.getSuperClasses().hasNext();
//		Iterator it = profile.getSuperClasses();
//		while (it.hasNext()) {
//			OntClass parent = (OntClass) it.next();
//			if( parent.hasSuperClass(profile.getBaseClass())) 
//				return true;
//		}
//		return false;
	}

	private void convertToNamed(ProfileClass profile) {

		//PropertyAccumulator props = new PropertyAccumulator();
		//removeProps(profile, props);
		//removeSupers(profile);
		
		ProfileClass parent = findOrCreateNamedProfile(profile.getBaseClass());
		profile.getSubject().addSuperClass(parent.getSubject());
//		allocateProperties(parent, props);
//		allocateProperties(props);
	}

	private void convertToUnnamed(ProfileClass profile) {
		removeSupers(profile);
	}

	public ProfileClass findOrCreateNamedProfile(OntClass base) {
		OntClass parent = findNamedProfile(base);
		if( parent == null ) {
			parent = model.createClass(namespace + base.getLocalName());
			parent.addSuperClass(base);
			ProfileClass profile = new ProfileClass(parent);
			add(profile);
			return profile;
		}
		return new ProfileClass(parent);
	}

	public OntClass findNamedProfile(OntClass base) {
		if( map == null)
			buildMap();

		OntClass parent = map.chooseBestProfile(base);
		return parent;
	}

//  FIXME: unused profile property operations follow.. 	
	
	private void allocateProperties(ProfileClass profile, PropertyAccumulator props) {
		Collection all = props.getAll();
		Iterator it = all.iterator();
		while (it.hasNext()) {
			PropertySpec spec = (PropertySpec) it.next();
			allocateProp(profile, spec);
			it.remove();
		}
	}
	
	private void allocateProperties(PropertyAccumulator props) {
		Collection all = props.getAll();
		while( ! all.isEmpty()) {
			PropertySpec spec = (PropertySpec) all.iterator().next();
			ProfileClass profile = findOrCreateNamedProfile(spec.base_domain);
			allocateProperties(profile, props);
		}
	}

	private void allocateProp(ProfileClass profile, PropertySpec spec) {
		if( profile.hasProperty(spec.prop))
			return;
		spec.create(profile);
	}

	private void removeSupers(ProfileClass profile) {
		Iterator it = profile.getSuperClasses();
		while (it.hasNext()) {
			OntClass parent = (OntClass) it.next();
			//profile.removeSuperClass(parent);
			profile.getSubject().removeSuperClass(parent);
		}
	}
	
	private void addProps(ProfileClass profile, PropertyAccumulator props) {
		Iterator it = props.getAll().iterator();
		while (it.hasNext()) {
			PropertySpec spec = (PropertySpec) it.next();
			spec.create(profile);
		}
	}

	private void removeProps(ProfileClass profile, OntClass base, PropertyAccumulator props) {
		Iterator targets = filterProfileForBase(profile, base).iterator();
		while (targets.hasNext()) {
			OntProperty prop = (OntProperty) targets.next();
			PropertyInfo info = profile.getPropertyInfo(prop);
			props.add(info);
			profile.remove(prop);
		}
	}

	private void removeProps(ProfileClass profile, PropertyAccumulator props) {
		Iterator targets = profile.getProperties();
		while (targets.hasNext()) {
			OntProperty prop = (OntProperty) targets.next();
			PropertyInfo info = profile.getPropertyInfo(prop);
			props.add(info);
			profile.remove(prop);
		}
	}

	private void collectProps(ProfileClass profile, PropertyAccumulator props) {
		Iterator targets = profile.getProperties();
		while (targets.hasNext()) {
			OntProperty prop = (OntProperty) targets.next();
			PropertyInfo info = profile.getPropertyInfo(prop);
			props.add(info);
		}
	}

	private static Collection filterProfileForBase(ProfileClass profile, OntClass base) {
		HashSet targets = new HashSet();
		Iterator it = profile.getProperties();
		while (it.hasNext()) {
			OntProperty prop = (OntProperty) it.next();
			OntResource domain = prop.getDomain();
			if(domain.isClass()) {
				OntClass clss = domain.asClass();
				if( clss.equals(base) || clss.hasSubClass(base))
					targets.add(prop);
			}
		}
		return targets;
	}
}
