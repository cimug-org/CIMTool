/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.preferences;

import java.util.Map;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.QualifiedNames;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer implements QualifiedNames {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = CIMToolPlugin.getDefault().getPreferenceStore();
		Map<String, Object> defaults = getPreferenceDefaults();
		for (Map.Entry<String, Object> entry : defaults.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Boolean b) {
				store.setDefault(entry.getKey(), b);
			} else if (value instanceof Float f) {
				store.setDefault(entry.getKey(), f);
			} else if (value instanceof Double d) {
				store.setDefault(entry.getKey(), d);
			} else if (value instanceof Long l) {
				store.setDefault(entry.getKey(), l);
			} else if (value instanceof Integer i) {
				store.setDefault(entry.getKey(), i);
			} else {
				store.setDefault(entry.getKey(), value.toString());
			}
		}
	}

}
