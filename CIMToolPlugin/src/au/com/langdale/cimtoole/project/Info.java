/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
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

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.registries.ModelParserRegistry;
import au.com.langdale.ui.builder.ColorUtils;
import au.com.langdale.util.Jobs;

/**
 * A set of utilities that define the file locations, file types, properties and
 * preferences used in a CIMTool project.
 */
public class Info {

	private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");

	// properties and preferences
	public static final QualifiedName PROFILE_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "profile_path");
	public static final QualifiedName BASE_MODEL_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "base_model_path");
	public static final QualifiedName SCHEMA_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "schema_namespace");
	public static final QualifiedName MERGE_SHADOW_EXTENSIONS = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"merge_shadow_extensions");
	public static final QualifiedName SELF_HEAL_ON_IMPORT = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"self_heal_on_import");
	public static final QualifiedName INSTANCE_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"instance_namespace");
	public static final QualifiedName MERGED_SCHEMA_PATH = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"merged_schema_path");
	public static final QualifiedName PRESERVE_NAMESPACES = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"preserve_namespaces");
	public static final QualifiedName USE_PACKAGE_NAMES = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"user_package_names");
	public static final QualifiedName PROBLEM_PER_SUBJECT = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"problem_per_subject");

	// these are preferences only: use as file properties is deprecated
	public static final QualifiedName PROFILE_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"profile_namespace");
	public static final QualifiedName PROFILE_ENVELOPE = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "profile_envelope");

	public static final QualifiedName MAPPING_NAMESPACE = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"mapping_namespace");
	public static final QualifiedName MAPPING_LABEL = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "mapping_label");
	
	/**
	 * PlantUML preferences below. When adding a new QualifiedName to the list here
	 * be sure to also add the new entry to the Info.getPlantUMLParameters() method
	 * to be passed along to the ProfileSerializer class.
	 */
	public static final QualifiedName PLANTUML_THEME = new QualifiedName(CIMToolPlugin.PLUGIN_ID, "plantuml_theme");
	public static final QualifiedName DOCROOT_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_docroot_classes_color");
	public static final QualifiedName CONCRETE_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_concrete_classes_color");
	public static final QualifiedName ABSTRACT_CLASSES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_abstract_classes_color");
	public static final QualifiedName ENUMERATIONS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_enumerations_color");
	public static final QualifiedName CIMDATATYPES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_cimdatatypes_color");
	public static final QualifiedName COMPOUNDS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_compounds_color");
	public static final QualifiedName PRIMITIVES_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_primitives_color");
	public static final QualifiedName ERRORS_COLOR = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_errors_color");
	public static final QualifiedName ENABLE_DARK_MODE = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_enable_dark_mode");
	public static final QualifiedName ENABLE_SHADOWING = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_enable_shadowing");
	public static final QualifiedName HIDE_ENUMERATIONS = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_hide_enumerations");
	public static final QualifiedName HIDE_CIMDATATYPES = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_hide_cimdatatypes");
	public static final QualifiedName HIDE_COMPOUNDS = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_hide_compounds");
	public static final QualifiedName HIDE_PRIMITIVES = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_hide_primitives");
	public static final QualifiedName HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES = new QualifiedName(
			CIMToolPlugin.PLUGIN_ID, "plantuml_hide_cardinality_for_required_attributes");
	public static final QualifiedName HORIZONTAL_SPACING = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_nodesep_horizontal_spacing");
	public static final QualifiedName VERTICAL_SPACING = new QualifiedName(CIMToolPlugin.PLUGIN_ID,
			"plantuml_ranksep_vertical_spacing");

	public static final List<String> themes = new LinkedList<String>();
	static {
		/*  Below are the current PlantUML themes supported OOTB */
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
	public static final String COPYRIGHT_MULTI_LINE_EXTENSION = "copyright-multi-line";
	public static final String COPYRIGHT_SINGLE_LINE_EXTENSION = "copyright-single-line";

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
		return isFile(resource, "Schema", "owl", "xmi", "eap", "eapx", "qea", "qeax", "feap")
				|| isFile(resource, "Schema", ModelParserRegistry.INSTANCE.getExtensions());
	}
	
	public static boolean isSettings(IResource resource) {
		String extension = resource.getFileExtension();
		boolean isSettings = (extension != null ? SETTINGS_EXTENSION.equals(extension) : false);
		return isSettings;
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

	/*
	 * public static boolean isXMI(IFile file) { String ext =
	 * file.getFileExtension(); if( ext == null) return false; ext =
	 * ext.toLowerCase(); return ext.equals("xmi"); }
	 */
	public static boolean isParseable(IFile file) {
		String ext = file.getFileExtension();
		if (ext == null)
			return false;
		ext = ext.toLowerCase();

		return ext.equals("xmi") || ext.equals("eap") || ext.equals("eapx") || ext.equals("qea") || ext.equals("qeax")
				|| ext.equals("feap") || ext.equals("owl") || ext.equals("n3") || ext.equals("simple-owl")
				|| ext.equals("merged-owl") || ext.equals("diagnostic") || ext.equals("cimtool-settings")
				|| ext.equals("repair") || ext.equals("mapping-ttl") || ext.equals("mapping-owl")
				|| ModelParserRegistry.INSTANCE.hasParserForExtension(ext);
	}

	public static IFile findMasterFor(IFile file) {
		String ext = file.getFileExtension();
		if (ext != null && ext.equalsIgnoreCase("annotation")) {
			IFile master = null;

			String[] schemaExt = new String[] { "xmi", "eap", "eapx", "qea", "qeax", "feap" };
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

	public static IFolder getDocumentationImages(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Images") : (IFolder) null;
	}
	
	public static IFolder getDocumentationIncludes(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Includes") : (IFolder) null;
	}
	
	public static IFolder getDocumentationStyles(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Styles") : (IFolder) null;
	}
	
	public static IFolder getDocumentationThemes(IProject project) {
		IFolder documentation = getDocumentationFolder(project);
		return documentation != null ? documentation.getFolder("Themes") : (IFolder) null;
	}
	
	public static IFolder getSchemaFolder(IProject project) {
		return project != null ? project.getFolder("Schema") : null;
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

	public static IFile getSettings(IProject project) {
		return project != null ? project.getFile("." + SETTINGS_EXTENSION) : null;
	}

	public static IFile getMultiLineCopyrightFile(IProject project) {
		IFile file = (project != null ? project.getFile("." + COPYRIGHT_MULTI_LINE_EXTENSION) : null);
		return file;
	}

	public static IFile getAsciidocThemeForBuilderFile(IProject project, String themeFileName) {
		IFile file = (project != null ? project.getFile("." + themeFileName) : null);
		return file;
	}

	private static InputStream openBundledFile(final String pathWithinBundle) throws CoreException {
		// Below attempts to load a file located within a bundle shipped as part of the
		// CIMTool product.
		InputStream source;
		try {
			Bundle cimtooleBundle = Platform.getBundle(CIMToolPlugin.PLUGIN_ID);
			URL url = cimtooleBundle.getEntry(pathWithinBundle);
			URL fileUrl = FileLocator.toFileURL(url);
			source = fileUrl.openStream();
		} catch (IOException e) {
			throw error("can't load file from within CIMTool bundle:  " + pathWithinBundle, e);
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
			source = openBundledFile("builders/default-copyright-template-empty.txt");
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
			source = openBundledFile("builders/default-copyright-template-multi-line.txt");
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
			source = openBundledFile("builders/default-copyright-template-single-line.txt");
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

	public static void putProperty(IResource resource, QualifiedName symbol, String value) {
		CIMToolPlugin.getSettings().putSetting(resource, symbol, value);
	}

	public static String getPreference(QualifiedName symbol) {
		return CIMToolPlugin.getDefault().getPluginPreferences().getString(symbol.getLocalName());
	}

	public static boolean getPreferenceOption(QualifiedName symbol) {
		return CIMToolPlugin.getDefault().getPluginPreferences().getBoolean(symbol.getLocalName());
	}

	public static String getSchemaNamespace(IResource resource) throws CoreException {
		IResource[] schemas = getSchemaFolder(resource.getProject()).members();
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

	public static Boolean isMergeShadowExtensionsEnabled(IResource resource) {
		try {
			return Boolean.valueOf(getProperty(resource, Info.MERGE_SHADOW_EXTENSIONS));
		} catch (CoreException e) {
			return Boolean.FALSE; // Should not happen, but defaults to enabled.
		}
	}
	
	public static Boolean isSelfHealingOnSchemaImportEnabled(IResource resource) {
		try {
			return Boolean.valueOf(getProperty(resource, Info.SELF_HEAL_ON_IMPORT));
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

	public static String getPlantUMLParameters(IFile file) {
		StringBuffer plantUMLParameters = new StringBuffer();
		// 
		// Set diagram preferences used by any PlantUML diagram builders...
		plantUMLParameters.append("plantUMLTheme=" + Info.getPropertyNoException(file, PLANTUML_THEME) + "|");
		plantUMLParameters.append("docRootClassesColor=" + Info.getPropertyNoException(file, DOCROOT_CLASSES_COLOR) + "|");
		plantUMLParameters.append("docRootClassesFontColor="
				+ ColorUtils.getHexFontColor(Info.getPropertyNoException(file, DOCROOT_CLASSES_COLOR)) + "|");
		plantUMLParameters.append("concreteClassesColor=" + Info.getPropertyNoException(file, CONCRETE_CLASSES_COLOR) + "|");
		plantUMLParameters.append("concreteClassesFontColor="
				+ ColorUtils.getHexFontColor(Info.getPropertyNoException(file, CONCRETE_CLASSES_COLOR)) + "|");
		plantUMLParameters.append("abstractClassesColor=" + Info.getPropertyNoException(file, ABSTRACT_CLASSES_COLOR) + "|");
		plantUMLParameters.append("abstractClassesFontColor="
				+ ColorUtils.getHexFontColor(Info.getPropertyNoException(file, ABSTRACT_CLASSES_COLOR)) + "|");
		plantUMLParameters.append("enumerationsColor=" + Info.getPropertyNoException(file, ENUMERATIONS_COLOR) + "|");
		plantUMLParameters.append(
				"enumerationsFontColor=" + ColorUtils.getHexFontColor(Info.getPropertyNoException(file, ENUMERATIONS_COLOR)) + "|");
		plantUMLParameters.append("cimDatatypesColor=" + Info.getPropertyNoException(file, CIMDATATYPES_COLOR) + "|");
		plantUMLParameters.append(
				"cimDatatypesFontColor=" + ColorUtils.getHexFontColor(Info.getPropertyNoException(file, CIMDATATYPES_COLOR)) + "|");
		plantUMLParameters.append("compoundsColor=" + Info.getPropertyNoException(file, COMPOUNDS_COLOR) + "|");
		plantUMLParameters
				.append("compoundsFontColor=" + ColorUtils.getHexFontColor(Info.getPropertyNoException(file, COMPOUNDS_COLOR)) + "|");
		plantUMLParameters.append("primitivesColor=" + Info.getPropertyNoException(file, PRIMITIVES_COLOR) + "|");
		plantUMLParameters.append(
				"primitivesFontColor=" + ColorUtils.getHexFontColor(Info.getPropertyNoException(file, PRIMITIVES_COLOR)) + "|");
		plantUMLParameters.append("errorsColor=" + Info.getPropertyNoException(file, ERRORS_COLOR) + "|");
		plantUMLParameters
				.append("errorsFontColor=" + ColorUtils.getHexFontColor(Info.getPropertyNoException(file, ERRORS_COLOR)) + "|");
		plantUMLParameters.append("enableDarkMode=" + Boolean.parseBoolean(Info.getPropertyNoException(file, ENABLE_DARK_MODE)) + "|");
		plantUMLParameters
				.append("enableShadowing=" + Boolean.parseBoolean(Info.getPropertyNoException(file, ENABLE_SHADOWING)) + "|");
		plantUMLParameters
				.append("hideEnumerations=" + Boolean.parseBoolean(Info.getPropertyNoException(file, HIDE_ENUMERATIONS)) + "|");
		plantUMLParameters
				.append("hideCIMDatatypes=" + Boolean.parseBoolean(Info.getPropertyNoException(file, HIDE_CIMDATATYPES)) + "|");
		plantUMLParameters.append("hideCompounds=" + Boolean.parseBoolean(Info.getPropertyNoException(file, HIDE_COMPOUNDS)) + "|");
		plantUMLParameters.append("hidePrimitives=" + Boolean.parseBoolean(Info.getPropertyNoException(file, HIDE_PRIMITIVES)) + "|");
		plantUMLParameters.append("hideCardinalityForRequiredAttributes="
				+ Boolean.parseBoolean(Info.getPropertyNoException(file, HIDE_CARDINALITY_FOR_REQUIRED_ATTRIBUTES)) + "|");
		plantUMLParameters.append("horizontalSpacing=" + Info.getPropertyNoException(file, HORIZONTAL_SPACING) + "|");
		plantUMLParameters.append("verticalSpacing=" + Info.getPropertyNoException(file, VERTICAL_SPACING) + "|");
		
		return plantUMLParameters.toString();
	}
}
