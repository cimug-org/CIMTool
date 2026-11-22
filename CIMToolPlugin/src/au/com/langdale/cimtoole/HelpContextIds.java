package au.com.langdale.cimtoole;

/**
 * Help context ids for the CIMTool views and Profile Editor tabs.
 *
 * Each id is attached to its user interface element with
 * <code>IWorkbenchHelpSystem.setHelp</code>. The description and help links for
 * each id are defined in <code>contexts_CIMTool.xml</code> in the CIMToolHelp
 * plug-in, which is registered for this plug-in, where the ids appear without
 * the plug-in prefix.
 */
public final class HelpContextIds {

	private static final String PREFIX = CIMToolPlugin.PLUGIN_ID + ".";

	public static final String PROJECT_MODEL_VIEW = PREFIX + "project_model_view";
	public static final String DOCUMENTATION_VIEW = PREFIX + "documentation_view";
	public static final String PROFILE_PREVIEW_VIEW = PREFIX + "profile_preview_view";
	public static final String VALIDATION_PROJECTS_VIEW = PREFIX + "validation_projects_view";

	public static final String PROFILE_EDITOR = PREFIX + "profile_editor";
	public static final String PROFILE_EDITOR_ADD_REMOVE = PREFIX + "profile_editor_add_remove";
	public static final String PROFILE_EDITOR_RESTRICTION = PREFIX + "profile_editor_restriction";
	public static final String PROFILE_EDITOR_DOCUMENTATION = PREFIX + "profile_editor_documentation";
	public static final String PROFILE_EDITOR_DESCRIPTION = PREFIX + "profile_editor_description";
	public static final String PROFILE_EDITOR_STEREOTYPES = PREFIX + "profile_editor_stereotypes";
	public static final String PROFILE_EDITOR_SUMMARY = PREFIX + "profile_editor_summary";

	public static final String SEARCH_WIZARD = PREFIX + "search_wizard";
	public static final String SEARCH_WIZARD_ADD_REMOVE = PREFIX + "search_wizard_add_remove";

	private HelpContextIds() {
	}
}
