/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.xmi.UML;

/**
 * Transform a profile model to conform with CIM/XML RDFS schema rules as defined
 * in IEC 61970-501 Ed 1.0 with the caveat that the min and max cardinalities of
 * properties will be reorganized to confirm to that as already defined in the 
 * profile. Meaning, the cardinality will not be reset to 
 * 
 * In the resulting profile model, each base class or property is represented
 * by at most one profile class or property.
 */
public class ProfileReorganizer extends Reorganizer {

	public ProfileReorganizer(OntModel profile, OntModel background, boolean useRefs) {
		super(profile, background, useRefs);
	}
	
	@Override
	protected SelectionOptions getSelectionOptions(boolean required) {
		return new SelectionOptions(SelectionOption.UseProfileCardinality, (required ? SelectionOption.PropertyRequired : SelectionOption.NoOp));
	}
	
	@Override
	protected void emitObjectProperty(String uri, OntResource profileProp, String domain,
			String range, PropertySpec propSpec) {
		boolean functional = propSpec.functional;
		OntResource prop = model.createResource(profileProp.getURI());
		ProfileClass profile = (ProfileClass) classes.get(domain);
		
		OntResource proxy = profile.createAllValuesFrom(prop, propSpec.min, propSpec.max);
		proxy.addSuperClass(model.createResource(range));
		proxies.put(uri, proxy);
		
		PropertyInfo info = profile.getPropertyInfo(prop);
		if(functional)
			info.setMaxCardinality(1);
		
		ProfileClass range_profile = (ProfileClass) classes.get(range);
		if(useRefs && ! range_profile.isEnumerated())
			proxy.addProperty(UML.hasStereotype, UML.byreference);
	}

}
