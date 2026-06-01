/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
//import org.apache.commons.lang3.CharEncoding;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

import com.healthmarketscience.jackcess.DatabaseBuilder;

import au.com.langdale.cimtoole.CIMNature;
import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.registries.ModelParserRegistry;
import au.com.langdale.colors.util.ColorUtils;
import au.com.langdale.util.Jobs;

/**
 * A set of utilities that define the file locations, file types, properties and
 * preferences used in a CIMTool project.
 */
public class Info implements QualifiedNames {

	protected static final String CIMUTIL_PLUGIN_ID = "au.com.langdale.cimutil";

	private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");

	public static final List<String> themes = new LinkedList<String>();
	static {
		/* Below are the current PlantUML themes supported OOTB */
		themes.add("_none_");
		themes.add("amiga");
		themes.add("aws-orange");
		themes.add("black-knight");
		themes.add("blueprint");
		// themes.add("carbon-gray"); // unrecognized by asciidoc plugin
		themes.add("cerulean");
		themes.add("cerulean-outline");
		// themes.add("cloudscape-design"); // unrecognized by asciidoc plugin
		themes.add("crt-amber");
		themes.add("crt-green");
		themes.add("cyborg");
		themes.add("cyborg-outline");
		themes.add("hacker");
		themes.add("lightgray");
		// themes.add("mars"); // unrecognized by asciidoc plugin
		themes.add("materia");
		themes.add("materia-outline");
		themes.add("metal");
		themes.add("mimeograph");
		themes.add("minty");
		themes.add("mono");
		themes.add("plain");
		themes.add("reddress-darkblue");
		themes.add("reddress-darkgreen");
		themes.add("reddress-darkorange");
		themes.add("reddress-darkred");
		themes.add("reddress-lightblue");
		themes.add("reddress-lightgreen");
		themes.add("reddress-lightorange");
		themes.add("reddress-lightred");
		themes.add("sandstone");
		themes.add("silver");
		themes.add("sketchy");
		themes.add("sketchy-outline");
		themes.add("spacelab");
		themes.add("spacelab-white");
		// themes.add("sunlust"); // unrecognized by asciidoc plugin
		themes.add("superhero");
		themes.add("superhero-outline");
		themes.add("toy");
		themes.add("united");
		// themes.add("vibrant"); // unrecognized by asciidoc plugin
	}

	public static final String SETTINGS_EXTENSION = "cimtool-settings";
	public static final String BUILDER_PREFERENCES_EXTENSION = "builder-preferences";
	public static final String GLOBAL_PREFERENCES_EXTENSION = "cimtool-global-preferences";
	public static final String COPYRIGHT_MULTI_LINE_EXTENSION = "copyright-multi-line";
	public static final String COPYRIGHT_SINGLE_LINE_EXTENSION = "copyright-single-line";

	public static boolean isCIMToolProject(IProject project) {
		try {
			if (project == null || !project.isOpen()) {
				return false;
			}
			// Check for nature first (preferred)
			if (project.hasNature(CIMNature.NATURE_ID)) {
				return true;
			}
			// Fall back to settings file for legacy projects
			return getSettings(project).exists();
		} catch (CoreException e) {
			return false;
		}
	}

	public static boolean isProfile(IResource resource) {
		return isFile(resource, "Profiles", "owl", "n3");
	}

	public static boolean isPlantUML(IResource resource) {
		return isFile(resource, "Profiles", "puml");
	}

	public static boolean isRuleSet(IResource resource) {
		return isFile(resource, "Profiles", "split-rules", "inc-rules");
	}

	public static boolean isRuleSet(IResource resource, String type) {
		return isFile(resource, "Profiles", type);
	}

	public static boolean isSchema(IResource resource) {
		return isFile(resource, "Schema", "owl", "xmi", "eap", "eapx", "qea", "qeax")
				|| isFile(resource, "Schema", ModelParserRegistry.INSTANCE.getExtensions());
	}

	public static boolean isEAProject(IResource resource) {
		return isFile(resource, "Schema", "eap", "eapx", "qea", "qeax");
	}

	public static boolean isEAProject(IFile file) {
		String[] eaExts = new String[] { "eap", "eapx", "qea", "qeax" };
		String ext = file.getFileExtension();
		if (ext == null)
			return false;
		ext = ext.toLowerCase();
		for (String anExt : eaExts) {
			if (ext.equals(anExt))
				return true;
		}
		return false;
	}

	public static boolean isSettings(IResource resource) {
		String extension = resource.getFileExtension();
		boolean isSettings = (extension != null ? SETTINGS_EXTENSION.equals(extension) : false);
		return isSettings;
	}

	public static boolean isBuilderPreferences(IResource resource) {
		String extension = resource.getFileExtension();
		boolean isBuilderPreferences = (extension != null ? BUILDER_PREFERENCES_EXTENSION.equals(extension) : false);
		return isBuilderPreferences;
	}

	public static boolean isSchemaFolder(IResource resource) {
		return isFolder(resource, "Schema");
	}

	public static boolean isInstance(IResource resource) {
		return isFile(resource, "Instances", "rdf", "xml");
	}

	public static boolean isSplitInstance(IResource resource) {
		return isFolder(resource, "Instances");
	}

	public static boolean isIncremental(IResource resource) {
		return isFolder(resource, "Incremental");
	}

	public static boolean isDiagnostic(IResource resource) {
		return isFile(resource, "Instances", "diagnostic");
	}

	public static boolean isRepair(IResource resource) {
		return isFile(resource, "Profiles", "repair");
	}

	private static boolean isFile(IResource resource, String location, String... types) {
		boolean hasExt = false;
		for (String t : types)
			hasExt = hasExt | hasExt(resource, t);
		return isFile(resource, location) && hasExt;
	}

	private static boolean isFile(IResource resource, String location) {
		IPath path = resource.getProjectRelativePath();
		return (resource instanceof IFile) && path.segmentCount() == 2 && path.segment(0).equals(location);
	}

	private static boolean hasExt(IResource file, String type) {
		String ext = file.getFileExtension();
		return ext != null && ext.equals(type);
	}

	public static boolean isFolder(IResource resource, String location) {
		IPath path = resource.getProjectRelativePath();
		return (resource instanceof IFolder) && path.segmentCount() == 2 && path.segment(0).equals(location);
	}

	public static boolean isXMI(IFile file) {
		String ext = file.getFileExtension();
		if (ext == null)
			return false;
		ext = ext.toLowerCase();
		return ext.equals("xmi");
	}

	public static boolean isParseable(IFile file) {
		String ext = file.getFileExtension();
		if (ext == null)
			return false;
		ext = ext.toLowerCase();

		return ext.equals("xmi") || ext.equals("eap") || ext.equals("eapx") || ext.equals("qea") || ext.equals("qeax")
				|| ext.equals("owl") || ext.equals("n3") || ext.equals("simple-owl") || ext.equals("merged-owl")
				|| ext.equals("diagnostic") || ext.equals("cimtool-settings") || ext.equals("repair")
				|| ext.equals("mapping-ttl") || ext.equals("mapping-owl")
				|| ModelParserRegistry.INSTANCE.hasParserForExtension(ext);
	}

	public static IFile findMasterFor(IFile file) {
		String ext = file.getFileExtension();
		if (ext != null && ext.equalsIgnoreCase("annotation")) {
			IFile master = null;

			String[] schemaExt = new String[] { "xmi", "eap", "eapx", "qea", "qeax" };
			for (String s : schemaExt) {
				master = getRelated(file, s);
				if (master.exists())
					return master;
			}

			for (String s : ModelParserRegistry.INSTANCE.getExtensions()) {
				master = getRelated(file, s);
				if (master.exists())
					return master;
			}
		}
		return null;
	}

	public static IFolder getRelatedFolder(IResource file) {
		IPath path = file.getFullPath().removeFileExtension();
		return file.getWorkspace().getRoot().getFolder(path);
	}

	public static IFile getRelated(IResource file, String ext) {
		// NOTE: Given that Eclipse's implementation of IResource always attempts to
		// locate the last index of the "." when removing a file extension we perform a
		// recursive call to ensure that an extension is no longer at the end.
		IPath path = removeFileExtension(file.getFullPath()).addFileExtension(ext);
		return file.getWorkspace().getRoot().getFile(path);
	}

	public static IFile getRelated(IResource file, String ext, boolean recursive) {
		// This method allows for the specification of whether to make a recursive
		// call or not...
		IPath path = null;
		if (recursive)
			path = removeFileExtension(file.getFullPath()).addFileExtension(ext);
		else
			path = file.getFullPath().removeFileExtension().addFileExtension(ext);
		return file.getWorkspace().getRoot().getFile(path);
	}

	/**
	 * Method to recursively remove all "extensions" up through to the final "."
	 * 
	 * @param path
	 * @return
	 */
	public static IPath removeFileExtension(IPath path) {
		return (path.getFileExtension() != null ? removeFileExtension(path.removeFileExtension()) : path);
	}

	public static IFolder getDocumentationFolder(IProject project) {
		return project != null ? project.getFolder("Documentation") : (IFolder) null;
	}

	public static IFolder getDocumentationImagesFolder(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Images") : (IFolder) null;
	}

	public static IFolder getDocumentationIncludesFolder(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Includes") : (IFolder) null;
	}

	public static IFolder getDocumentationStylesFolder(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Styles") : (IFolder) null;
	}

	public static IFolder getDocumentationThemesFolder(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Themes") : (IFolder) null;
	}

	public static IFolder getSchemaFolder(IProject project) {
		return project != null ? project.getFolder("Schema") : null;
	}

	public static IFolder getSchemaImportReportFolder(IProject project) {
		IFolder schema = getSchemaFolder(project);
		return schema != null ? schema.getFolder(".import-reports") : (IFolder) null;
	}

	public static IFolder getProfileFolder(IProject project) {
		return project != null ? project.getFolder("Profiles") : null;
	}

	public static IFolder getInstanceFolder(IProject project) {
		return project != null ? project.getFolder("Instances") : null;
	}

	public static IFolder getIncrementalFolder(IProject project) {
		return project != null ? project.getFolder("Incremental") : null;
	}

	public static boolean doesFolderExistOnDiskEFS(IFolder folder) {
		try {
			URI uri = folder.getLocationURI(); // null for virtual/unmapped
			if (uri == null)
				return false;
			IFileStore store = EFS.getStore(uri);
			IFileInfo info = store.fetchInfo();
			return info.exists() && info.isDirectory();
		} catch (CoreException e) {
			// log appropriately
			return false;
		}
	}

	public static IFile getSettings(IProject project) {
		return project != null ? project.getFile("." + SETTINGS_EXTENSION) : null;
	}

	public static IFile getBuilderPreferences(IProject project) {
		return project != null ? project.getFile("." + BUILDER_PREFERENCES_EXTENSION) : null;
	}

	public static IFile getGlobalPreferences(IProject project) {
		return project != null ? project.getFile("." + GLOBAL_PREFERENCES_EXTENSION) : null;
	}

	public static IFile getMultiLineCopyrightFile(IProject project) {
		IFile file = (project != null ? project.getFile("." + COPYRIGHT_MULTI_LINE_EXTENSION) : null);
		return file;
	}

	public static IFile getAsciidocThemeForBuilderFile(IProject project, String themeFileName) {
		IFile file = (project != null ? project.getFile("." + themeFileName) : null);
		return file;
	}

	/**
	 * Opens an input stream to a file from the bundle associated with the specified
	 * pluginID. Used for resources such as builders and copyright templates.
	 */
	protected static InputStream openBundledFile(String pluginID, final String pathWithinBundle) throws CoreException {
		InputStream source;
		try {
			Bundle bundle = Platform.getBundle(pluginID);
			URL url = bundle.getEntry(pathWithinBundle);
			URL fileUrl = FileLocator.toFileURL(url);
			source = fileUrl.openStream();
		} catch (IOException e) {
			throw error("Unable to load file from within bundle:  " + pathWithinBundle, e);
		}
		return source;
	}

	/**
	 * Method that retrieves the multiline copyright header template.
	 */
	public static String getDefaultEmptyCopyrightTemplate() throws CoreException {
		String copyright = "";

		InputStream source = null;
		try {
			source = openBundledFile(CIMUTIL_PLUGIN_ID, "builders/empty-copyright-template.txt");
			copyright = new String(IOUtils.toByteArray(new InputStreamReader(source), Charset.forName("UTF-8")));
		} catch (IOException e) {
			// We currently do nothing on error. This should typically not occur...
		} finally {
			if (source != null) {
				try {
					source.close();
				} catch (IOException e) {
				}
			}
		}

		if ("".equals(copyright.trim())) {
			if ((copyright.contains("\r") || copyright.contains("\n")))
				copyright = "";
		}

		return copyright;
	}

	/**
	 * Method that retrieves the multiline copyright header template.
	 */
	public static String getDefaultMultiLineCopyrightTemplate() throws CoreException {
		String copyright = "";

		InputStream source = null;
		try {
			source = openBundledFile(CIMUTIL_PLUGIN_ID, "builders/default-copyright-template-multi-line.txt");
			copyright = new String(IOUtils.toByteArray(new InputStreamReader(source), Charset.forName("UTF-8")));
		} catch (IOException e) {
			// We currently do nothing on error. This should typically not occur...
		} finally {
			if (source != null) {
				try {
					source.close();
				} catch (IOException e) {
				}
			}
		}

		if ("".equals(copyright.trim())) {
			if ((copyright.contains("\r") || copyright.contains("\n")))
				copyright = "";
		}

		return copyright;
	}

	/**
	 * Method that retrieves the single-line copyright header template.
	 */
	public static String getDefaultSingleLineCopyrightTemplate() throws CoreException {
		String copyright = "";

		InputStream source = null;
		try {
			source = openBundledFile(CIMUTIL_PLUGIN_ID, "builders/default-copyright-template-single-line.txt");
			copyright = new String(IOUtils.toByteArray(new InputStreamReader(source), Charset.forName("UTF-8")));
		} catch (IOException e) {
			// We currently do nothing on error. This should typically not occur...
		} finally {
			if (source != null) {
				try {
					source.close();
				} catch (IOException e) {
				}
			}
		}

		if ("".equals(copyright.trim())) {
			if ((copyright.contains("\r") || copyright.contains("\n")))
				copyright = "";
		}

		return copyright;
	}

	/**
	 * Method that retrieves the multiline copyright header template for the
	 * specified project and populates it with the current year.
	 */
	public static String getMultiLineCopyrightText(IProject project) throws CoreException {
		String currentYear = YEAR_FORMAT.format(new Date());
		String copyright = getMultiLineCopyrightTemplate(project);
		copyright = copyright.replace("${year}", currentYear).replace("${YEAR}", currentYear);
		return copyright;
	}

	/**
	 * Method that retrieves the Asciidoc theme (if applicable) for the specified
	 * builder.
	 */
	public static void getAsciidocThemesForBuilder(IProject project, String themeName) throws CoreException {
		if (project != null) {
			/**
			 * Extract the root name of the theme. This is determined due to convention
			 * whereby all themes are to be named the same as the builder itself with the
			 * exception of the extension. In this case, currently *.yml for PDF themes.
			 */
			String theThemeName = themeName.substring(0, themeName.indexOf(".") - 1);

			/**
			 * Currently, the PDF themes (i.e. *.yml) are bundled with the plugin.
			 */
			IFile themeFile = getAsciidocThemeForBuilderFile(project, theThemeName + ".yml");

			/**
			 * The following addresses the scenario where the project workspace may be out
			 * of sync with the file system. If an asciidoc theme file does not exist in the
			 * project for the specified builder then we need to load and add the
			 * corresponding asciidoc theme for it. This small section of code is purely for
			 * backwards compatibility needed by older projects that were created before the
			 * Asciidoc plugin and feature set was added.
			 */
			themeFile.refreshLocal(IResource.DEPTH_ZERO, null);

			/**
			 * Finally we can check to see if the file actually exists or not and if not it
			 * is loaded via a run and wait Job...
			 */
			if (!themeFile.exists()) {
				// Jobs.runWait(Task.saveAsciidocThemesForBuilder(project), project, theme);
			}
		}
	}

	/**
	 * Method that retrieves the multiline copyright header template.
	 */
	public static String getMultiLineCopyrightTemplate(IProject project) throws CoreException {
		String copyright = "";

		if (project != null) {
			IFile copyrightFile = getMultiLineCopyrightFile(project);

			/**
			 * The following addresses the scenario where the project workspace may be out
			 * of sync with the file system. If a copyright header file does not exist for
			 * the project we need to load and add the default copyright header to it. This
			 * small section of code is purely for backwards compatibility for older
			 * projects that were created before the new copyright feature was added.
			 */
			copyrightFile.refreshLocal(IResource.DEPTH_ZERO, null);
			if (!copyrightFile.exists()) {
				Jobs.runWait(Task.saveDefaultMultiLineCopyrightTemplate(project), project);
			}

			if (copyrightFile.exists()) {
				InputStream source = null;
				try {
					source = copyrightFile.getContents();
					copyright = new String(
							IOUtils.toByteArray(new InputStreamReader(source), Charset.forName("UTF-8")));
				} catch (IOException e) {
					// We currently do nothing on error. This should typically not occur...
				} finally {
					if (source != null) {
						try {
							source.close();
						} catch (IOException e) {
						}
					}
				}
			}

			if ("".equals(copyright.trim())) {
				if ((copyright.contains("\r") || copyright.contains("\n")))
					copyright = "";
			}
		}

		return copyright;
	}

	public static IFile getSingleLineCopyrightFile(IProject project) {
		IFile file = (project != null ? project.getFile("." + COPYRIGHT_SINGLE_LINE_EXTENSION) : null);
		return file;
	}

	/**
	 * Method that retrieves the single-line copyright header template and populates
	 * it with the current year.
	 */
	public static String getSingleLineCopyrightText(IProject project) throws CoreException {
		String currentYear = YEAR_FORMAT.format(new Date());
		String copyright = getSingleLineCopyrightTemplate(project);
		copyright = copyright.replace("${year}", currentYear).replace("${YEAR}", currentYear);
		return copyright;
	}

	/**
	 * Method that retrieves the single-line copyright header template.
	 */
	public static String getSingleLineCopyrightTemplate(IProject project) throws CoreException {
		String copyright = "";

		if (project != null) {
			IFile copyrightFile = getSingleLineCopyrightFile(project);

			/**
			 * The following addresses the scenario where the project workspace may be out
			 * of sync with the file system. If a copyright header file does not exist for
			 * the project we need to load and add the default copyright header to it. This
			 * small section of code is purely for backwards compatibility for older
			 * projects that were created before the new copyright feature was added.
			 */
			copyrightFile.refreshLocal(IResource.DEPTH_ZERO, null);
			if (!copyrightFile.exists()) {
				Jobs.runWait(Task.saveDefaultSingleLineCopyrightTemplate(project), project);
			}

			if (copyrightFile.exists()) {
				InputStream source = null;
				try {
					source = copyrightFile.getContents();
					copyright = new String(
							IOUtils.toByteArray(new InputStreamReader(source), Charset.forName("UTF-8")));
				} catch (IOException e) {
					// We currently do nothing on error. This should typically not occur...
				} finally {
					if (source != null) {
						try {
							source.close();
						} catch (IOException e) {
						}
					}
				}
			}

			if ("".equals(copyright.trim())) {
				if ((copyright.contains("\r") || copyright.contains("\n")))
					copyright = "";
			}
		}

		return copyright;
	}

	public static IResource getInstanceFor(IResource result) {
		IResource instance = getRelated(result, "ttl");
		if (!instance.exists())
			instance = getRelated(result, "rdf");
		if (!instance.exists())
			instance = getRelated(result, "xml");
		if (!instance.exists())
			instance = getRelatedFolder(result);
		if (!instance.exists()) {
			instance = null;
		}
		return instance;

	}

	public static IFile getProfileFor(IResource resource) throws CoreException {
		IResource instance = getBaseModelFor(resource);
		if (instance != null)
			resource = instance;

		String path = getProperty(resource, Info.PROFILE_PATH);
		if (path.length() == 0)
			return null;

		IFile profile = getProfileFolder(resource.getProject()).getFile(path);
		if (!profile.exists())
			return null;

		return profile;
	}

	public static IFile getRulesFor(IResource resource) throws CoreException {
		IFile profile = getProfileFor(resource);
		if (profile == null)
			return null;

		String type;
		if (isInstance(resource))
			type = "simple-rules";
		else if (isSplitInstance(resource))
			type = "split-rules";
		else if (isIncremental(resource))
			type = "inc-rules";
		else
			return null;

		IFile rules = getRelated(profile, type);
		if (!rules.exists())
			return null;

		return rules;
	}

	public static IResource getBaseModelFor(IResource resource) throws CoreException {
		String path = getProperty(resource, Info.BASE_MODEL_PATH);
		if (path.length() == 0)
			return null;

		IFolder instance = getInstanceFolder(resource.getProject()).getFolder(path);
		if (!instance.exists())
			return null;

		return instance;
	}

	public static String getProperty(IResource resource, QualifiedName symbol) throws CoreException {
		String value;
		if (resource.exists()) {
			Settings settings = CIMToolPlugin.getSettings();
			value = settings.getSetting(resource, symbol);
			if (value == null) {
				value = getPreference(symbol);
				settings.putSetting(resource, symbol, value);
			}
		} else {
			value = getPreference(symbol);
		}
		return value;
	}

	public static String getPropertyNoException(IResource resource, QualifiedName symbol) {
		String value = "";
		try {
			value = getProperty(resource, symbol);
		} catch (CoreException ce) {
		}
		return value;
	}

	public static String getBuilderPreference(IResource resource, QualifiedName symbol) {
		String value = CIMToolPlugin.getBuilderPreferences().getPreference(resource, symbol);
		if (value == null) {
			value = getPreference(symbol);
		}
		return value;
	}

	public static void putBuilderPreference(IResource resource, QualifiedName symbol, String value) {
		CIMToolPlugin.getBuilderPreferences().putPreference(resource, symbol, value);
	}

	public static void putProperty(IResource resource, QualifiedName symbol, String value) {
		CIMToolPlugin.getSettings().putSetting(resource, symbol, value);
	}

	public static String getPreference(QualifiedName symbol) {
		return CIMToolPlugin.getDefault().getPluginPreferences().getString(symbol.getLocalName());
	}

	public static String getBuilderPreference(QualifiedName symbol) {
		// Note that the call to CIMToolPlugin.getDefault().getPluginPreferences() is
		// intentional.
		return CIMToolPlugin.getDefault().getPluginPreferences().getString(symbol.getLocalName());
	}

	public static boolean getPreferenceOption(QualifiedName symbol) {
		return CIMToolPlugin.getDefault().getPluginPreferences().getBoolean(symbol.getLocalName());
	}

	/**
	 * Method that applied hierarchical precedence for preference setting can be
	 * configured to hierarchically override:
	 * 
	 * <pre>
	 * global default preferences -> project-level preference -> profile-level preference
	 * </pre>
	 * 
	 * @param resource The resource to determine the default for.
	 * @param symbol   The QualifedName for the preference.
	 * @return The preference value.
	 */
	public static String getHierarchicalBuilderPreference(IResource resource, QualifiedName symbol) {
		String style = null;
		if (isProfile(resource)) {
			// Step 1: Check first at the profile-level to determine if the preference is
			// specified...
			style = CIMToolPlugin.getBuilderPreferences().getPreference(resource, symbol);
			if (style == null || style.isEmpty()) {
				// Step 2: When not, we next check to see if the preference is specified at the
				// project-level...
				IProject project = resource.getProject();
				style = CIMToolPlugin.getBuilderPreferences().getPreference(project, symbol);
				if (style == null || style.isEmpty()) {
					// Step 3: If not, then the preference's global default is used...
					style = getPreference(symbol);
				}
			}
		}
		return style;
	}

	/**
	 * Note for this method we do not resort to a "fallback" default value as we
	 * intentionally want to know if a value is explicitly set at the level
	 * (profile, project or global) of the resource passed in.
	 * 
	 * @param resource The resource to retrieve a builder preference value for.
	 * @param symbol   The QualifedName for the preference.
	 * @return The preference value.
	 */
	public static String getHierarchicalBuilderPreferenceWithoutDefault(IResource resource, QualifiedName symbol) {
		if (isProfile(resource)) {
			// Resource is a profile so we get the profile-level preference and return it.
			// Note for this method we do not resort to a "fallback" default hierarchically
			// as we intentionally want to know if a value is explicitly set or not.
			return CIMToolPlugin.getBuilderPreferences().getPreference(resource, symbol);
		} else if (resource instanceof IProject) {
			// Resource is a profile so we get the profile-level preference and return it.
			return CIMToolPlugin.getBuilderPreferences().getPreference(resource, symbol);
		} else {
			return getPreference(symbol);
		}
	}

	public static String getSchemaNamespace(IProject project) throws CoreException {
		IResource[] schemas = getSchemaFolder(project.getProject()).members();
		for (int ix = 0; ix < schemas.length; ix++) {
			IResource schema = schemas[ix];
			if (isSchema(schema)) {
				Settings settings = CIMToolPlugin.getSettings();
				String ns = settings.getSetting(schema, SCHEMA_NAMESPACE);
				if (ns != null)
					return ns;
			}
		}
		return getPreference(SCHEMA_NAMESPACE);
	}

	public static String getSchemaNamespace(IResource schemaFile) throws CoreException {
		IResource[] schemas = getSchemaFolder(schemaFile.getProject()).members();
		for (int ix = 0; ix < schemas.length; ix++) {
			IResource schema = schemas[ix];
			if (isSchema(schema) && schema.getName().equals(schemaFile.getName())) {
				Settings settings = CIMToolPlugin.getSettings();
				String ns = settings.getSetting(schema, SCHEMA_NAMESPACE);
				if (ns != null)
					return ns;
			}
		}
		return getPreference(SCHEMA_NAMESPACE);
	}

	public static Boolean isMergeShadowExtensionsEnabled(IResource resource) {
		try {
			// The 'merge shadow extensions' setting is a project level setting in the
			// .cimtool-settings file.
			return Boolean.valueOf(getProperty(resource.getProject(), Info.MERGE_SHADOW_EXTENSIONS));
		} catch (CoreException e) {
			return Boolean.FALSE; // Should not happen, but defaults to enabled.
		}
	}

	public static Boolean isSelfHealingOnSchemaImportEnabled(IResource resource) {
		try {
			// The 'self heal' setting is a project level setting in the .cimtool-settings
			// file.
			return Boolean.valueOf(getProperty(resource.getProject(), Info.SELF_HEAL_ON_IMPORT));
		} catch (CoreException e) {
			return Boolean.FALSE; // Should not happen, but defaults to enabled.
		}
	}

	public static CoreException error(String m) {
		return new CoreException(new Status(Status.ERROR, CIMToolPlugin.PLUGIN_ID, m));
	}

	public static CoreException error(String m, Exception e) {
		return new CoreException(new Status(Status.ERROR, CIMToolPlugin.PLUGIN_ID, m, e));
	}

	public static String checkValidEAProject(File source) {
		try {
			DatabaseBuilder.open(source).close();
			return null;
		} catch (Exception e) {
			return "The EAP/EAPX file appears to be incompatible with CIMTool.";
		}
	}

	public static String getBuilderParameters(IFile file) {
		StringBuffer builderParameters = new StringBuffer();
		// Currently we only have PlantUML builder parameters...
		if (isFile(file, "Profiles", "puml") || isFile(file, "Profiles", "adoc") || isProfile(file)) {
			//
			// Set diagram preferences used by any PlantUML diagram builders...
			/**
			 * IMPORTANT:
			 * 
			 * To properly support the CIMTool CLI, it requires that we maintain two
			 * instances of this method. This one and another located in the
			 * CLIBuilderPreference class in the CIMUtil project. Whenever any type of
			 * update is made here the parallel instance must be updated as well to ensure
			 * it matches in the CLI.
			 */
			builderParameters.append("plantUMLTheme=" + Info.getBuilderPreference(file, PLANTUML_THEME) + "|");
			builderParameters
					.append("docRootClassesColor=" + Info.getBuilderPreference(file, DOCROOT_CLASSES_COLOR) + "|");
			builderParameters.append("anonymousCompoundsColor="
					+ ColorUtils.lighten(Info.getBuilderPreference(file, COMPOUNDS_COLOR), 60f) + "|");
			builderParameters.append("anonymousEnumerationsColor="
					+ ColorUtils.lighten(Info.getBuilderPreference(file, ENUMERATIONS_COLOR), 60f) + "|");
			builderParameters.append("anonymousComplexTypesColor="
					+ ColorUtils.lighten(Info.getBuilderPreference(file, CONCRETE_CLASSES_COLOR), 60f) + "|");
			builderParameters.append("docRootClassesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, DOCROOT_CLASSES_COLOR)) + "|");
			builderParameters
					.append("concreteClassesColor=" + Info.getBuilderPreference(file, CONCRETE_CLASSES_COLOR) + "|");
			builderParameters.append("concreteClassesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, CONCRETE_CLASSES_COLOR)) + "|");
			builderParameters
					.append("abstractClassesColor=" + Info.getBuilderPreference(file, ABSTRACT_CLASSES_COLOR) + "|");
			builderParameters.append("abstractClassesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, ABSTRACT_CLASSES_COLOR)) + "|");
			builderParameters.append("enumerationsColor=" + Info.getBuilderPreference(file, ENUMERATIONS_COLOR) + "|");
			builderParameters.append("enumerationsFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, ENUMERATIONS_COLOR)) + "|");
			builderParameters.append("cimDatatypesColor=" + Info.getBuilderPreference(file, CIMDATATYPES_COLOR) + "|");
			builderParameters.append("cimDatatypesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, CIMDATATYPES_COLOR)) + "|");
			builderParameters.append("compoundsColor=" + Info.getBuilderPreference(file, COMPOUNDS_COLOR) + "|");
			builderParameters.append("compoundsFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, COMPOUNDS_COLOR)) + "|");
			builderParameters.append("primitivesColor=" + Info.getBuilderPreference(file, PRIMITIVES_COLOR) + "|");
			builderParameters.append("primitivesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, PRIMITIVES_COLOR)) + "|");
			builderParameters.append("choicesColor=" + Info.getBuilderPreference(file, CHOICES_COLOR) + "|");
			builderParameters.append("choicesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, CHOICES_COLOR)) + "|");
			builderParameters.append("refsColor=" + Info.getBuilderPreference(file, REFS_COLOR) + "|");
			builderParameters.append(
					"refsFontColor=" + ColorUtils.getHexFontColor(Info.getBuilderPreference(file, REFS_COLOR)) + "|");
			builderParameters
					.append("shadowClassesColor=" + Info.getBuilderPreference(file, SHADOW_CLASSES_COLOR) + "|");
			builderParameters.append("shadowClassesFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, SHADOW_CLASSES_COLOR)) + "|");
			builderParameters.append("errorsColor=" + Info.getBuilderPreference(file, ERRORS_COLOR) + "|");
			builderParameters.append("errorsFontColor="
					+ ColorUtils.getHexFontColor(Info.getBuilderPreference(file, ERRORS_COLOR)) + "|");
			builderParameters.append("setAnonymousClassesColorWhite="
					+ Boolean.parseBoolean(Info.getBuilderPreference(file, ANONYMOUS_CLASSES_COLOR_WHITE)) + "|");
			builderParameters.append(
					"enableDarkMode=" + Boolean.parseBoolean(Info.getBuilderPreference(file, ENABLE_DARK_MODE)) + "|");
			builderParameters.append(
					"enableShadowing=" + Boolean.parseBoolean(Info.getBuilderPreference(file, ENABLE_SHADOWING)) + "|");
			builderParameters.append("hideEnumerations="
					+ Boolean.parseBoolean(Info.getBuilderPreference(file, HIDE_ENUMERATIONS)) + "|");
			builderParameters.append("hideCIMDatatypes="
					+ Boolean.parseBoolean(Info.getBuilderPreference(file, HIDE_CIMDATATYPES)) + "|");
			builderParameters.append(
					"hideCompounds=" + Boolean.parseBoolean(Info.getBuilderPreference(file, HIDE_COMPOUNDS)) + "|");
			builderParameters.append(
					"hidePrimitives=" + Boolean.parseBoolean(Info.getBuilderPreference(file, HIDE_PRIMITIVES)) + "|");
			builderParameters.append("errorAssistance=" + isProfile(file) + "|");
			builderParameters.append("hideCardinalityForRequiredAttributes="
					+ Boolean.parseBoolean(Info.getBuilderPreference(file, HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES))
					+ "|");
			builderParameters.append("horizontalSpacing=" + Info.getBuilderPreference(file, HORIZONTAL_SPACING) + "|");
			builderParameters.append("verticalSpacing=" + Info.getBuilderPreference(file, VERTICAL_SPACING) + "|");
		}

		return builderParameters.toString();
	}

	public static String getNamespacePrefixesBuilderParameter(Map<String, String> prefix2NSMap) {
		StringBuffer namespacePrefixesBuilderParameter = new StringBuffer("");
		Iterator<String> prefixes = prefix2NSMap.keySet().iterator();
		while (prefixes.hasNext()) {
			String prefix = prefixes.next();
			namespacePrefixesBuilderParameter.append(prefix2NSMap.get(prefix)).append("=").append(prefix)
					.append(prefixes.hasNext() ? "|" : "");
		}
		return namespacePrefixesBuilderParameter.toString(); // Empty parameter
	}

}
