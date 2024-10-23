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
		store.setDefault(Info.CONCRETE_CLASSES_COLOR.getLocalName(), "#FFFFE0");
		store.setDefault(Info.ABSTRACT_CLASSES_COLOR.getLocalName(), "#D3D3D3");
		store.setDefault(Info.ENUMERATIONS_COLOR.getLocalName(), "#90EE90");
		store.setDefault(Info.CIMDATATYPES_COLOR.getLocalName(), "#FFEBCD");
		store.setDefault(Info.COMPOUNDS_COLOR.getLocalName(), "#FFEBCD");
		store.setDefault(Info.PRIMITIVES_COLOR.getLocalName(), "#E6E6FF");
		store.setDefault(Info.ERRORS_COLOR.getLocalName(), "#FFC0CB");
		store.setDefault(Info.ENABLE_DARK_MODE.getLocalName(), false);
		store.setDefault(Info.ENABLE_SHADOWING.getLocalName(), true);
		store.setDefault(Info.HIDE_ENUMERATIONS.getLocalName(), false);
		store.setDefault(Info.HIDE_CIMDATATYPES.getLocalName(), true);
		store.setDefault(Info.HIDE_COMPOUNDS.getLocalName(), true);
		store.setDefault(Info.HIDE_PRIMITIVES.getLocalName(), true);
		store.setDefault(Info.HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES.getLocalName(), false);
	}

}
