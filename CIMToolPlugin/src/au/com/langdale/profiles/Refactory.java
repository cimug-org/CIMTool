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

/**
 * Add or remove a profile class from the profile class hierarchy with side effects.
 */
public class Refactory extends ProfileUtility {
	private OntModel model;
	private ProfileMap map;
	private String namespace;
	private OntModel profileModel;
	private OntModel backgroundModel;
	
	public Refactory(OntModel profileModel, OntModel backgroundModel, String namespace) {
		this.profileModel = profileModel;
		this.backgroundModel = backgroundModel;
		this.model = Models.merge(profileModel, backgroundModel);
		this.namespace = namespace;
	}
	
	public OntModel getModel() {
		return model;
	}
	
	public void refresh() {
		map = null;
		this.model = Models.merge(profileModel, backgroundModel);
	}
	
	public void add(OntClass clss, OntClass base, boolean link) {

		if( map != null) 
			map.add(base, clss);
		
		if(link)
			linkToHierarchy(base, clss);
	}

	private void linkToHierarchy(OntClass base, OntClass clss) {
		if( map == null )
			buildMap();
		
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
	
	public void remove(OntClass clss) {
		if( map != null)
			map.removeProfile(clss);
		
		// super profiles to be inherited by sub profiles
		Set supers = asSet(new ProfileClass(clss).getSuperClasses());
		
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
				sub.addSuperClass((OntResource) jt.next());
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
		
		OntClass parent = findOrCreateNamedProfile(profile.getBaseClass());
		profile.getSubject().addSuperClass(parent);
//		allocateProperties(parent, props);
//		allocateProperties(props);
	}

	private void convertToUnnamed(ProfileClass profile) {
		removeSupers(profile);
	}

	public OntClass findOrCreateNamedProfile(OntClass base) {
		OntClass clss = findNamedProfile(base);
		if( clss == null ) {
			clss = model.createClass(namespace + base.getLocalName());
			clss.addSuperClass(base);
			add(clss, base, true);
		}
		return clss;
	}
	
	public OntClass findBaseClass(OntClass clss) {
		if( map == null)
			buildMap();
		return map.getBase(clss);
	}

	public OntClass findNamedProfile(OntClass base) {
		if( map == null)
			buildMap();

		return map.chooseBestProfile(base);
	}
	
	public Collection findRelatedProfiles(OntClass base, boolean subclass, boolean unique) {
		if( map == null)
			buildMap();
		return map.findRelatedProfiles(base, subclass, unique);
	}
	
	public Collection findProfiles(OntClass base) {
		if( map == null)
			buildMap();
		return map.findProfiles(base);
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
			ProfileClass profile = new ProfileClass(findOrCreateNamedProfile(spec.base_domain));
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
