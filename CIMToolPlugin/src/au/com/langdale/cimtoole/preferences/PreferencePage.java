/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.preferences;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.FurnishedPropertyPage;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Template;
import static au.com.langdale.ui.builder.Templates.*;

public class PreferencePage extends FurnishedPropertyPage {

	public PreferencePage() {
		setPreferenceStore(CIMToolPlugin.getDefault().getPreferenceStore());
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(
						Group(
								Label("Default Schema Namespace:"), 
								new Preference(Info.SCHEMA_NAMESPACE, Validators.NAMESPACE)),
						Group(
								Label("Default Instance Namespace:"),
								new Preference(Info.INSTANCE_NAMESPACE, Validators.NAMESPACE)),
						Group(
								Label("Default Profile Namespace:"),
								new Preference(Info.PROFILE_NAMESPACE, Validators.NAMESPACE)),
						Group(
								Label("Default Envelope Element Name:"),
								new Preference(Info.PROFILE_ENVELOPE, Validators.NCNAME)),
						Group( 
								new PreferenceOption(Info.PRESERVE_NAMESPACES, 
								"Preserve schema namespaces in profiles")),
						Group( 
								new PreferenceOption(Info.USE_PACKAGE_NAMES, 
								"Add package names to default schema namespace")),
						Group( 
								new PreferenceOption(Info.PROBLEM_PER_SUBJECT, 
								"Limit validation output by subject and message type"))
				);
			}
		};
	}
}