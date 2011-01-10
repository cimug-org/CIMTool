/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cim.CIM;
import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Info;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setDefault(Info.INSTANCE_NAMESPACE.getLocalName(), "http://iec.ch/TC57/2007/network#");
		store.setDefault(Info.PROFILE_NAMESPACE.getLocalName(), "http://iec.ch/TC57/2007/profile#");
		store.setDefault(Info.MAPPING_NAMESPACE.getLocalName(), "http://langdale.com.au/2010/schema-mapping#");
		store.setDefault(Info.MAPPING_LABEL.getLocalName(), "Mappings");
		store.setDefault(Info.SCHEMA_NAMESPACE.getLocalName(), CIM.NS);
		store.setDefault(Info.PROFILE_ENVELOPE.getLocalName(), "Profile");
		store.setDefault(Info.PRESERVE_NAMESPACES.getLocalName(), true);
		store.setDefault(Info.PROBLEM_PER_SUBJECT.getLocalName(), true);
		store.setDefault(Info.USE_PACKAGE_NAMES.getLocalName(), false);
	}

}
