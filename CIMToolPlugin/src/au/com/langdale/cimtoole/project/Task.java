/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;

import au.com.langdale.cim.CIM;
import au.com.langdale.cimtoole.CIMNature;
import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.CIMBuilder;
import au.com.langdale.cimtoole.builder.ProfileBuildlets;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.registries.ModelParser;
import au.com.langdale.cimtoole.registries.ModelParserRegistry;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.cimtoole.reporting.CIMModellingGuideViolationsReportGenerator;
import au.com.langdale.cimtoole.reporting.ReportGenerationSettings;
import au.com.langdale.easyrules.rules.RuleViolation;
import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.logging.SchemaImportLogger;
import au.com.langdale.logging.SchemaImportLoggerFactory;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.util.Jobs;
import au.com.langdale.validation.RepairMan;
import au.com.langdale.validation.ValidatorUtil;
import au.com.langdale.workspace.ResourceOutputStream;
import au.com.langdale.xmi.CIMInterpreter;
import au.com.langdale.xmi.CIMInterpreterFactory;
import au.com.langdale.xmi.CIMInterpreterResult;
import au.com.langdale.xmi.EAProjectParser;
import au.com.langdale.xmi.EAProjectParserException;
import au.com.langdale.xmi.EAProjectParserFactory;
import au.com.langdale.xmi.NamespacePrefixes;
import au.com.langdale.xmi.StereotypeExtensions;
import au.com.langdale.xmi.StereotypedNamespaces;
import au.com.langdale.xmi.UML;
import au.com.langdale.xmi.XMIParser;

/**
 * Utility tasks for CIMTool plugin. Tasks are instances of
 * <code>IWorkspaceRunnable</code> and in some cases, plain methods.
 */
public class Task extends Info {

	public static class EmptyStream extends InputStream {
		@Override
		public int read() throws IOException {
			return -1;
		}
	}

	public static IWorkspaceRunnable chain(final IWorkspaceRunnable first, final IWorkspaceRunnable second) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				first.run(monitor);
				second.run(monitor);
			}
		};
	}

	public static IWorkspaceRunnable createProfile(final IFile file, final String namespace, final String envname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				OntModel model = ModelFactory.createMem();
				OntModel backgroundModel = getBackgroundModel(file);
				addNamespacePrefixesToModel(file.getProject(), model);
				initProfile(model, backgroundModel, namespace, envname, null);
				monitor.worked(1);
				writeProfile(file, model, monitor);
				putProperty(file, PROFILE_NAMESPACE, namespace);
				monitor.worked(1);
			}
		};
	}

	public static IWorkspaceRunnable createProject(final IProject project, final URI location) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IProjectDescription description = ResourcesPlugin.getWorkspace()
						.newProjectDescription(project.getName());
				description.setLocationURI(location);
				description.setNatureIds(new String[] { CIMNature.NATURE_ID });
				ICommand command = description.newCommand();
				command.setBuilderName(CIMBuilder.BUILDER_ID);
				description.setBuildSpec(new ICommand[] { command });
				project.create(description, monitor);
				project.open(monitor);
				monitor.worked(1);
				getDocumentationFolder(project).create(false, true, monitor);
				getDocumentationImagesFolder(project).create(false, true, monitor);
				getDocumentationIncludesFolder(project).create(false, true, monitor);
				getDocumentationStylesFolder(project).create(false, true, monitor);
				getDocumentationThemesFolder(project).create(false, true, monitor);
				getSchemaFolder(project).create(false, true, monitor);
				getSchemaImportReportFolder(project).create(false, true, monitor);
				getProfileFolder(project).create(false, true, monitor);
				getInstanceFolder(project).create(false, true, monitor);
				getIncrementalFolder(project).create(false, true, monitor);
				//
				saveSparxEAStylePlantUMLFile(project).run(monitor);
				saveAsciiDoctorConfigFile(project).run(monitor);
				saveImportReportsAsciidocDependencies(project).run(monitor);
				saveSettings(project, ModelFactory.createMem()).run(monitor);
				saveBuilderPreferences(project, ModelFactory.createMem()).run(monitor);
				saveGlobalBuilderPreferences(project).run(monitor);
				saveDefaultSingleLineCopyrightTemplate(project).run(monitor);
				saveDefaultMultiLineCopyrightTemplate(project).run(monitor);
				//
				monitor.worked(1);
			}
		};
	}

	public static IWorkspaceRunnable createImportReportDependencies(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				getSchemaImportReportFolder(project).create(false, true, monitor);
				//
				saveSparxEAStylePlantUMLFile(project).run(monitor);
				saveAsciiDoctorConfigFile(project).run(monitor);
				saveImportReportsAsciidocDependencies(project).run(monitor);
				saveSettings(project, ModelFactory.createMem()).run(monitor);
				saveBuilderPreferences(project, ModelFactory.createMem()).run(monitor);
				saveDefaultSingleLineCopyrightTemplate(project).run(monitor);
				saveDefaultMultiLineCopyrightTemplate(project).run(monitor);
				// 5
				monitor.worked(1);
			}
		};
	}

	public static IWorkspaceRunnable createRules(final IFile file, final String template) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				InputStream source;
				if (template != null)
					source = getClass().getResourceAsStream("/au/com/langdale/profiles/" + template + ".xsl");
				else
					source = new EmptyStream();
				monitor.worked(1);

				writeFile(file, source, monitor);
			}

		};
	}

	public static IWorkspaceRunnable createValidationRules(final IFile file, final String template) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				InputStream source;
				if (template != null)
					source = ValidatorUtil.openStandardRules(template);
				else
					source = new EmptyStream();
				monitor.worked(1);

				writeFile(file, source, monitor);
			}
		};
	}

	public static IWorkspaceRunnable importSchema(final IFile file, final String pathname, final String namespace,
			final Boolean mergeShadowExtensions, final Boolean selfHealingOnImport) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {

				/** Replace spaces with underscores if present in the name of the schema file */
				IFile targetFile = file;
				if (file.getLocation().lastSegment().contains(" "))
					targetFile = replaceSpacesInFileName(file);
				importFile(file, targetFile, pathname, monitor);
				//
				if (isEAProject(file)) {
					File relatedFileToImport = getSchemaRelatedFile(pathname, "stereotype-extensions");
					if (relatedFileToImport.exists()) {
						IFile stereotypeExtensionsFile = Info.getSchemaFolder(file.getProject())
								.getFile(relatedFileToImport.getName());
						targetFile = stereotypeExtensionsFile;
						if (stereotypeExtensionsFile.getLocation().lastSegment().contains(" ")) {
							targetFile = replaceSpacesInFileName(stereotypeExtensionsFile);
						}
						importFile(stereotypeExtensionsFile, targetFile, relatedFileToImport.getAbsolutePath(),
								monitor);
					}
					//
					IFile namespacePrefixesFile = null;
					relatedFileToImport = getSchemaRelatedFile(pathname, "namespace-prefixes");
					if (relatedFileToImport.exists()) {
						namespacePrefixesFile = Info.getSchemaFolder(file.getProject())
								.getFile(relatedFileToImport.getName());
						targetFile = namespacePrefixesFile;
						if (namespacePrefixesFile.getLocation().lastSegment().contains(" ")) {
							targetFile = replaceSpacesInFileName(namespacePrefixesFile);
						}
						importFile(namespacePrefixesFile, targetFile, relatedFileToImport.getAbsolutePath(), monitor);
					}
					//
					IFile namespacesFile = null;
					relatedFileToImport = getSchemaRelatedFile(pathname, "namespaces");
					if (relatedFileToImport.exists()) {
						namespacesFile = Info.getSchemaFolder(file.getProject()).getFile(relatedFileToImport.getName());
						targetFile = namespacesFile;
						if (namespacesFile.getLocation().lastSegment().contains(" ")) {
							targetFile = replaceSpacesInFileName(namespacesFile);
						}
						importFile(namespacesFile, targetFile, relatedFileToImport.getAbsolutePath(), monitor);
					}
				}
				//
				putProperty(file, SCHEMA_NAMESPACE, namespace);

				// The next two settings are project-specific settings and therefore require
				// a call to file.getProject() to ensure we're passing in the project and not
				// the resource.
				putProperty(file.getProject(), MERGE_SHADOW_EXTENSIONS,
						(mergeShadowExtensions != null ? mergeShadowExtensions.toString()
								: getPreference(MERGE_SHADOW_EXTENSIONS)));
				putProperty(file.getProject(), SELF_HEAL_ON_IMPORT,
						(selfHealingOnImport != null ? selfHealingOnImport.toString()
								: getPreference(SELF_HEAL_ON_IMPORT)));
			}
		};
	}

	public static IWorkspaceRunnable generateImportSchemaReport(final IFile schemaFile,
			final ReportGenerationSettings rgs) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {

				/** Replace spaces with underscores if present in the name of the schema file */
				IFile targetSchemaFile = schemaFile;
				if (schemaFile.getLocation().lastSegment().contains(" "))
					targetSchemaFile = replaceSpacesInFileName(schemaFile);

				if (isEAProject(targetSchemaFile)) {
					IFile namespacesFile = getRelated(targetSchemaFile, "namespaces", false);
					IFile importReportFile = getRelated(targetSchemaFile, "adoc", false);
					//
					CIMInterpreterResult result = parseEAProject(targetSchemaFile, rgs.shouldGenerateReport());
					List<RuleViolation> violations = result.getRuleViolations();
					//
					// We recreate a new instance of the report generation settings and include a
					// namespace mapping file if present
					ReportGenerationSettings reportGenerationSettings = new ReportGenerationSettings.Builder() //
							.schemaFile(targetSchemaFile) //
							.shouldGenerateReport(rgs.shouldGenerateReport()) //
							.isMergeShadowExtensionsEnabled(rgs.isMergeShadowExtensionsEnabled()) //
							.isSelfHealingOnSchemaImportEnabled(rgs.isSelfHealingOnSchemaImportEnabled()) //
							.stereotypedNamespaces((namespacesFile.exists() ? namespacesFile : null)) //
							.includeExtensions(rgs.includeExtensions()) //
							.includeNormative(rgs.includeNormative()) //
							.includeGrid(rgs.includeGrid()) //
							.includeEnterprise(rgs.includeEnterprise()) //
							.includeMarket(rgs.includeMarket()) //
							.build();
					//
					String importReport = CIMModellingGuideViolationsReportGenerator
							.generateReport(reportGenerationSettings, violations);
					InputStream inputStream = new ByteArrayInputStream(importReport.getBytes(StandardCharsets.UTF_8));
					Jobs.runWait(importInputStreamToFile(importReportFile, inputStream), importReportFile);
					// importInputStreamToFile(importReportFile, inputStream);
				}
			}
		};
	}

	/**
	 * Convenience method for constructing a File representation for a file
	 * "related" to the schema file passed in. The relatedFileExt parameter
	 * indicates what the file extension is on the related file. The file is
	 * expected to have the same name as the original file but with a different
	 * extension.
	 * 
	 * @param schemaFullPath
	 * @param relatedFileExt
	 * @return The derived result of the File path for the related file with the
	 *         specified file extensions.
	 */
	private static File getSchemaRelatedFile(String schemaFullPath, String relatedFileExt) {
		relatedFileExt = (relatedFileExt.startsWith(".") ? relatedFileExt : "." + relatedFileExt);
		java.nio.file.Path schemaPath = Paths.get(schemaFullPath);
		java.nio.file.Path parentDir = schemaPath.getParent();
		String fileName = schemaPath.getFileName().toString();
		int lastDotIndex = fileName.lastIndexOf('.');
		String baseFileName = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);
		java.nio.file.Path ratedFile = parentDir.resolve(baseFileName + relatedFileExt);
		return ratedFile.toFile();
	}

	public static IWorkspaceRunnable importTransformBuilder(final TransformBuildlet buildlet, final File xslFile,
			final File xslImportFile) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				ProfileBuildletConfigUtils.addTransformBuilderConfigEntry(buildlet, xslFile, xslImportFile);
				/**
				 * Given that we've imported a new buildlet, we need to reload the "cached"
				 * available profile buildlets. This will allow the new buildlet to appear in
				 * the "Profile Summary" tab.
				 */
				ProfileBuildlets.reload();
			}
		};
	}

	public static IWorkspaceRunnable importProfile(final IFile file, final String pathname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				String defaultName = getPreference(PROFILE_ENVELOPE);
				String namespace = getPreference(PROFILE_NAMESPACE);
				OntModel model = parse(openExternalFile(pathname, monitor), "owl", namespace);
				model = fixupProfile(model, getBackgroundModel(file), defaultName, namespace);
				writeProfile(file, model, monitor);
				putProperty(file, PROFILE_NAMESPACE, namespace);
			}
		};
	}

	public static IWorkspaceRunnable importMultiLineCopyright(final IFile file, final String pathname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IProject project = file.getProject();
				IFile file = Info.getMultiLineCopyrightFile(project);
				importFile(file, pathname, monitor);
			}
		};
	}

	public static IWorkspaceRunnable importInputStreamToFile(final IFile file, final InputStream inputStream) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, inputStream, monitor);
			}
		};
	}

	public static IWorkspaceRunnable importSingleLineCopyright(final IFile file, final String pathname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IProject project = file.getProject();
				IFile file = Info.getSingleLineCopyrightFile(project);
				importFile(file, pathname, monitor);
			}
		};
	}

	public static IWorkspaceRunnable importRules(final IFile file, final String pathname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, pathname, monitor);
			}
		};
	}

	public static IWorkspaceRunnable importModel(final IFile file, final String pathname, final String namespace,
			final IFile profile) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				importFile(file, pathname, monitor);
				putProperty(file, PROFILE_PATH, profile.getName());
				putProperty(file, INSTANCE_NAMESPACE, namespace);
			}
		};
	}

	public static OntModel parse(IFile file) throws CoreException {
		String ext = file.getFileExtension().toLowerCase();
		if (ext.equals("xmi")) {
			return parseXMI(file);
		} else if (isEAProject(file)) {
			CIMInterpreterResult result = parseEAProject(file, false);
			return result.getModel();
		} else {
			if (ModelParserRegistry.INSTANCE.hasParserForExtension(ext)) {
				ModelParser[] parsers = ModelParserRegistry.INSTANCE.getParsersForExtension(ext);
				/*
				 * For now we'll just pick the first one so we don't start adding too much UI
				 * stuff...
				 */
				ModelParser parser = parsers[0];
				parser.setFile(file);
				try {
					parser.run();
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID, e.getMessage(), e));
				}
				return interpretSchema(parser.getModel(), file);

			} else {
				return parseOWL(file);
			}
		}
	}

	public static TreeModelBase createTreeModel(IResource resource) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		if (resource instanceof IProject)
			return createUMLTreeModel(cache.getMergedOntologyWait(getSchemaFolder((IProject) resource)));
		else if (isSchemaFolder(resource))
			return createUMLTreeModel(cache.getMergedOntologyWait((IFolder) resource));
		else if (isSchema(resource))
			return createUMLTreeModel(cache.getOntologyWait((IFile) resource));
		else if (isProfile(resource)) {
			IFile file = (IFile) resource;
			return getMessageModel(file);
		} else
			return new UMLTreeModel();
	}

	private static TreeModelBase createUMLTreeModel(OntModel model) {
		UMLTreeModel tree = new UMLTreeModel();
		tree.setOntModel(model);
		tree.setRootResource(UML.global_package);
		return tree;
	}

	public static ProfileModel getMessageModel(IFile file) throws CoreException {
		ProfileModel model = new ProfileModel();
		model.setOntModel(getProfileModel(file));
		model.setBackgroundModel(getBackgroundModel(file));
		return model;
	}

	private static OntModel parseXMI(IFile file) throws CoreException {
		XMIParser parser = new XMIParser();
		try {
			parser.parse(new BufferedInputStream(file.getContents()));
		} catch (Exception e) {
			throw error("Can't parse model file " + file.getName(), e);
		}
		return interpretSchema(parser.getModel(), file);
	}

	private static CIMInterpreterResult parseEAProject(IFile file, boolean validateModel) throws CoreException {
		EAProjectParser parser;
		try {
			boolean selfHealingOnImportEnabled = Info.isSelfHealingOnSchemaImportEnabled(file);
			boolean usePackageNames = getPreferenceOption(USE_PACKAGE_NAMES);
			//
			SchemaImportLogger logger = SchemaImportLoggerFactory.getLogger(Task.class);
			//
			Set<String> stereotypeExtensions = new HashSet<>();
			IFile stereotypeExtensionsFile = getRelated(file, "stereotype-extensions", false);
			if (stereotypeExtensionsFile.getLocation().toFile().exists()) {
				StereotypeExtensions.initStereotypeExtensions(file.getProject().getName(),
						stereotypeExtensionsFile.getLocation().toFile());
				stereotypeExtensions = StereotypeExtensions.getStereotypes(file.getProject().getName());
			}
			//
			IFile namespacePrefixesFile = getRelated(file, "namespace-prefixes", false);
			if (namespacePrefixesFile.exists()) {
				try {
					NamespacePrefixes.init(file.getProject().getName(), namespacePrefixesFile.getLocation().toFile());
				} catch (Exception e) {
					throw new CoreException(new Status(IStatus.ERROR, CIMToolPlugin.PLUGIN_ID,
							String.format("Duplicate namespace prefix mapping found in file: %s",
									namespacePrefixesFile.getLocation().toFile().getName()),
							e));
				}
			}
			//
			IFile namespacesFile = getRelated(file, "namespaces", false);
			//
			parser = EAProjectParserFactory.createParser( //
					Task.getSchemaNamespace(file.getProject()), //
					file.getLocation().toFile(), //
					selfHealingOnImportEnabled, //
					validateModel, //
					usePackageNames, //
					logger, //
					(namespacesFile.getLocation().toFile().exists() ? namespacesFile.getLocation().toFile() : null), //
					stereotypeExtensions);
			parser.parse();
		} catch (EAProjectParserException e) {
			throw error("Can't access EA project", e);
		}
		//
		CIMInterpreterResult result = interpretSchema(parser.getModel(), file, parser.getStereotypedNamespaces(),
				validateModel);
		if (validateModel) {
			result.getRuleViolations().addAll(parser.getRuleViolations());
		}
		return result;
	}

	private static OntModel interpretSchema(OntModel raw, IFile file) throws CoreException {
		Boolean mergeShadowExtensionsEnabled = Info.isMergeShadowExtensionsEnabled(file);
		String base = getProperty(file, SCHEMA_NAMESPACE);
		if (base == null) {
			if (file.getName().toLowerCase().startsWith("cim"))
				base = CIM.NS;
			else
				base = file.getLocationURI().toString() + "#";
		}
		//
		IFile auxfile = getRelated(file, "annotation", false);
		OntModel annote;
		if (auxfile.exists()) {
			annote = ModelFactory.createMem();
			IO.read(annote, auxfile.getContents(), base, Format.TURTLE.toFormat());
		} else
			annote = null;
		CIMInterpreter interpreter = CIMInterpreterFactory.create(file.getLocation().toFile());

		CIMInterpreterResult result = interpreter.interpret(raw, base, annote, getPreferenceOption(USE_PACKAGE_NAMES),
				mergeShadowExtensionsEnabled, false);
		return result.getModel();
	}

	private static CIMInterpreterResult interpretSchema(OntModel raw, IFile file,
			StereotypedNamespaces stereotypedNamespaces, boolean validateModel) throws CoreException {
		Boolean mergeShadowExtensionsEnabled = Info.isMergeShadowExtensionsEnabled(file);
		String base = getProperty(file, SCHEMA_NAMESPACE);
		if (base == null) {
			if (file.getName().toLowerCase().startsWith("cim"))
				base = CIM.NS;
			else
				base = file.getLocationURI().toString() + "#";
		}
		//
		IFile auxfile = getRelated(file, "annotation", false);
		OntModel annote;
		if (auxfile.exists()) {
			annote = ModelFactory.createMem();
			IO.read(annote, auxfile.getContents(), base, Format.TURTLE.toFormat());
		} else {
			annote = null;
		}

		CIMInterpreter interprepter = CIMInterpreterFactory.create(stereotypedNamespaces, file.getLocation().toFile());

		CIMInterpreterResult result = interprepter.interpret(raw, base, annote, getPreferenceOption(USE_PACKAGE_NAMES),
				mergeShadowExtensionsEnabled, validateModel);

		return result;
	}

	private static OntModel parseOWL(IFile file) throws CoreException {
		OntModel model;
		String extn = file.getFileExtension();

		if (extn == null)
			extn = "";
		else
			extn = extn.toLowerCase();

		String base;
		if (isProfile(file)) {
			base = getProperty(file, PROFILE_NAMESPACE);
		} else if (extn.equals(SETTINGS_EXTENSION) || extn.equals(BUILDER_PREFERENCES_EXTENSION)) {
			base = CIMToolPlugin.PROJECT_NS;
		} else {
			base = file.getLocationURI().toString() + "#";
		}
		try {
			model = parse(new BufferedInputStream(file.getContents()), extn, base);
			if (isProfile(file)) {
				addNamespacePrefixesToModel(file.getProject(), model);
			}
		} catch (Exception ex) {
			throw error("Can't parse model file " + file.getName(), ex);
		}
		return model;
	}

	public static OntModel parse(InputStream contents, String ext, String base) {
		OntModel model = ModelFactory.createMem();
		String syntax;
		if (ext.equals("n3"))
			syntax = Format.N3.toFormat();
		else if (ext.equals("diagnostic") || ext.equals(Info.SETTINGS_EXTENSION)
				|| ext.equals(Info.GLOBAL_PREFERENCES_EXTENSION) || ext.equals(Info.BUILDER_PREFERENCES_EXTENSION)
				|| ext.equals("mapping-ttl"))
			syntax = Format.TURTLE.toFormat();
		else if (ext.equals("owl") || ext.equals("repair"))
			syntax = Format.RDF_XML_WITH_NODEIDS.toFormat();
		else
			syntax = Format.RDF_XML.toFormat();
		IO.read(model, contents, base, syntax);
		return model;
	}

	public static void write(OntModel model, String namespace, boolean xmlbase, IFile file, String format,
			IProgressMonitor monitor) throws CoreException {
		OutputStream stream = new ResourceOutputStream(file, monitor, false, false);
		write(model, namespace, xmlbase, format, stream, Info.getMultiLineCopyrightText(file.getProject()));
	}

	public static void write(OntModel model, String namespace, boolean xmlbase, String format, OutputStream stream)
			throws CoreException {
		write(model, namespace, xmlbase, format, stream, null);
	}

	public static void write(OntModel model, String namespace, boolean xmlbase, String format, OutputStream stream,
			String copyright) throws CoreException {
		try {
			HashMap style = new HashMap();
			if (format != null && format.equals(Format.RDF_XML_ABBREV.toFormat()))
				style.put("prettyTypes", PrettyTypes.PRETTY_TYPES);
			if (namespace != null) {
				if (namespace.endsWith("#"))
					namespace = namespace.substring(0, namespace.length() - 1);
				if (xmlbase)
					style.put("xmlbase", namespace);
				style.put("relativeURIs", "same-document");
			}
			style.put("showXmlDeclaration", "true");

			if ((copyright != null && !"".equals(copyright.trim())) && Format.isXML(format)) {
				IO.write(model, stream, namespace, format, style, copyright);
			} else {
				IO.write(model, stream, namespace, format, style);
			}

			stream.close();
		} catch (IOException e) {
			throw error("can't write model to stream", e);
		}
	}

	public static IWorkspaceRunnable exportSchema(final IProject project, final String pathname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFolder folder = Info.getSchemaFolder(project);
				OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(folder);
				OutputStream output;
				try {
					output = new BufferedOutputStream(new FileOutputStream(pathname));
				} catch (IOException ex) {
					throw error("can't write to " + pathname);
				}
				writeOntology(output, schema, Format.RDF_XML.toFormat(), monitor);
			}
		};
	}

	public static IWorkspaceRunnable saveMappings(final IFile file, final OntModel model) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				writeMappings(file, model, monitor);

			}
		};
	}

	public static IWorkspaceRunnable repairProfile(final IFile file, final RepairMan repairs) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				if (repairs.size() > 0) {
					IFile related = Info.getRelated(file, "owl");
					OntModel model = Task.getProfileModel(related);
					model = repairs.apply(model);
					writeProfile(related, model, monitor);
				}
			}
		};
	}

	public static IWorkspaceRunnable saveProfile(final IFile file, final OntModel model) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				addNamespacePrefixesToModel(file.getProject(), model);
				writeProfile(file, model, monitor);
			}
		};
	}

	protected static void addNamespacePrefixesToModel(final IProject project, final OntModel model) {
		if (project == null)
			return;

		Map<String, String> prefixMappings = NamespacePrefixes.getPrefixToNamespaceMap(project.getName());
		for (String prefix : prefixMappings.keySet()) {
			// We only add the prefix if the prefix/namespace is not already in the
			// NsPrefixMap...
			if (model.getNsPrefixMap().getNsURIPrefix(prefixMappings.get(prefix)) == null
					&& model.getNsPrefixMap().getNsPrefixURI(prefix) == null) {
				model.setNsPrefix(prefix, prefixMappings.get(prefix));
			}
		}
	}

	public static IWorkspaceRunnable saveSettings(final IProject project, final OntModel model) {
		model.setNsPrefix("tool", CIMToolPlugin.SETTING_NS);
		model.setNsPrefix("project", CIMToolPlugin.PROJECT_NS);
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				write(model, CIMToolPlugin.PROJECT_NS, true, getSettings(project), Format.TURTLE.toFormat(), monitor);
			}
		};
	}

	/**
	 * Returns an {@link IWorkspaceRunnable} that saves the given builder
	 * preferences model to the project's {@code .builder-preferences} file.
	 *
	 * <p>
	 * A shallow refresh of the project's root directory is performed before writing
	 * to ensure Eclipse's workspace model is in sync with the actual filesystem
	 * state. This prevents a {@code CoreException} (wrapped as
	 * {@code java.io.IOException}) that would otherwise occur if the
	 * {@code .builder-preferences} file was deleted outside of Eclipse while the
	 * project was closed, leaving Eclipse's cached resource state stale.
	 * </p>
	 */
	public static IWorkspaceRunnable saveBuilderPreferences(final IProject project, final OntModel model) {
		model.setNsPrefix("prefs", BUILDER_PREFS_NS);
		model.setNsPrefix("project", CIMToolPlugin.PROJECT_NS);
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile prefsFile = getBuilderPreferences(project);
				if (prefsFile != null) {
					prefsFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
				}
				write(model, CIMToolPlugin.PROJECT_NS, true, prefsFile, Format.TURTLE.toFormat(), monitor);
			}
		};
	}

	public static IWorkspaceRunnable saveGlobalBuilderPreferences(final IProject project) {
		IPreferenceStore store = CIMToolPlugin.getDefault().getPreferenceStore();

		// Build the RDF model
		OntModel model = ModelFactory.createMem();
		OntResource subject = model.createResource(GLOBAL_PREFS_NS);

		// Export each preference
		for (QualifiedName symbol : PREFERENCES_SYMBOLS) {
			String value = store.getString(symbol.getLocalName());
			if (value != null && !value.isEmpty()) {
				Property property = createProperty(symbol);
				subject.addProperty(property, value);
			}
		}

		model.setNsPrefix("prefs", BUILDER_PREFS_NS);
		model.setNsPrefix("project", CIMToolPlugin.PROJECT_NS);
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				write(model, CIMToolPlugin.PROJECT_NS, true, getGlobalPreferences(project), Format.TURTLE.toFormat(), monitor);
			}
		};
	}

	/**
	 * Create a property for the builder preference symbol.
	 */
	protected static Property createProperty(QualifiedName symbol) {
		String qualifier = symbol.getQualifier();
		
		if (qualifier == null || CIMToolPlugin.PLUGIN_ID.equals(qualifier))
			qualifier = BUILDER_PREFS_NS;
		else
			qualifier += "#";

		return ResourceFactory.createProperty(qualifier + symbol.getLocalName());
	}

	public static IWorkspaceRunnable saveDefaultMultiLineCopyrightTemplate(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile file = Info.getMultiLineCopyrightFile(project);
				importFileFromBundle(CIMUTIL_PLUGIN_ID, file, "builders/empty-copyright-template.txt", monitor);
			}
		};
	}

	public static IWorkspaceRunnable saveSparxEAStylePlantUMLFile(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// prefix with dot to make hidden on the Windows file system.
				IFile file = Info.getSchemaFolder(project).getFile(".sparx-ea-style.puml");
				importFileFromBundle(CIMUTIL_PLUGIN_ID, file, "/import-reports/puml/sparx-ea-style.puml", monitor);
			}
		};
	}

	public static IWorkspaceRunnable saveAsciiDoctorConfigFile(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// prefix with dot to make hidden on the Windows file system.
				IFile file = Info.getSchemaFolder(project).getFile(".asciidoctorconfig.adoc");
				importFileFromBundle(CIMUTIL_PLUGIN_ID, file, "/import-reports/schema/asciidoctorconfig.adoc", monitor);
			}
		};
	}

	public static IWorkspaceRunnable saveImportReportsAsciidocDependencies(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// prefix with dot to make hidden on the Windows file system.
				IPath projectTargetPath = Info.getSchemaImportReportFolder(project).getProjectRelativePath();
				copyFolderFromBundleToProject(CIMUTIL_PLUGIN_ID, project, "/import-reports/asciidoc", projectTargetPath, monitor);
			}
		};
	}

	private static void copyFolderFromBundleToProject(final String pluginID, IProject project, String bundleFolderSourcePath,
			IPath projectTargetPath, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = Platform.getBundle(pluginID);
			URL entry = bundle.getEntry(bundleFolderSourcePath);
			if (entry == null)
				throw new FileNotFoundException("Bundle resource not found: " + bundleFolderSourcePath);

			URL resolvedUrl = FileLocator.toFileURL(entry);
			File sourceDir = new File(resolvedUrl.getPath());

			if (!sourceDir.exists() || !sourceDir.isDirectory()) {
				throw new IOException("Invalid source directory in bundle: " + sourceDir.getAbsolutePath());
			}

			// Recursively copy contents
			Files.walk(sourceDir.toPath()).forEach(source -> {
				try {
					Path relativePath = sourceDir.toPath().relativize(source);
					IPath targetPath = projectTargetPath
							.append(relativePath.toString().replace(File.separatorChar, '/'));
					IFile targetFile = project.getFile(targetPath);

					if (Files.isDirectory(source)) {
						IFolder folder = project.getFolder(targetPath);
						if (!folder.exists())
							folder.create(true, true, monitor);
					} else {
						InputStream in = new FileInputStream(source.toFile());
						if (targetFile.exists()) {
							targetFile.setContents(in, true, true, monitor);
						} else {
							targetFile.create(in, true, monitor);
						}
						in.close();
					}
				} catch (Exception e) {
					throw new UncheckedIOException(
							new IOException("Error copying from " + source + ": " + e.getMessage(), e));
				}
			});
		} catch (IOException e) {
			error("Unable to copy the import reports asciidoc resources into the new project.", e);
		}
		monitor.worked(1);
	}

	public static IWorkspaceRunnable saveDefaultSingleLineCopyrightTemplate(final IProject project) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile file = Info.getSingleLineCopyrightFile(project);
				importFileFromBundle(CIMUTIL_PLUGIN_ID, file, "builders/empty-copyright-template.txt", monitor);
			}
		};
	}

	public static IWorkspaceRunnable delete(final IResource resource) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				resource.delete(false, monitor);
				monitor.worked(1);
			}
		};
	}

	public static IWorkspaceRunnable delete(final IResource[] resources) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				for (int ix = 0; ix < resources.length; ix++) {
					resources[ix].delete(false, monitor);
					monitor.worked(1);
				}
			}
		};
	}

	private static void importFile(final IFile file, final String pathname, IProgressMonitor monitor)
			throws CoreException {
		InputStream source = openExternalFile(pathname, monitor);
		writeFile(file, source, monitor);
		monitor.worked(1);
	}

	private static void importFile(final IFile file, final IFile targetFile, final String pathname,
			IProgressMonitor monitor) throws CoreException {
		InputStream source = openExternalFile(pathname, monitor);
		writeFile(targetFile, source, monitor);
		monitor.worked(1);
	}

	private static void importFile(final IFile file, final InputStream source, IProgressMonitor monitor)
			throws CoreException {
		writeFile(file, source, monitor);
		monitor.worked(1);
	}
	
	private static void importFileFromBundle(final String pluginID, final IFile file, final String pathWithinBundle,
			IProgressMonitor monitor) throws CoreException {
		InputStream source = openBundledFile(pluginID, pathWithinBundle, monitor);
		writeFile(file, source, monitor);
		monitor.worked(1);
	}

	public static InputStream inputStreamFromBundle(final String pluginID, final String pathWithinBundle, IProgressMonitor monitor)
			throws CoreException {
		InputStream source = openBundledFile(pluginID, pathWithinBundle, monitor);
		monitor.worked(1);
		return source;
	}

	private static InputStream openExternalFile(final String pathname, IProgressMonitor monitor) throws CoreException {
		InputStream source;
		try {
			source = new BufferedInputStream(new FileInputStream(pathname));
		} catch (FileNotFoundException e) {
			throw error("can't open " + pathname, e);
		}
		monitor.worked(1);
		return source;
	}

	private static InputStream openBundledFile(final String pluginID, final String pathWithinBundle, IProgressMonitor monitor)
			throws CoreException {
		// Below attempts to load a file located within a bundle shipped as part of the
		// CIMTool product.
		InputStream source;
		try {
			Bundle cimtooleBundle = Platform.getBundle(pluginID);
			URL url = cimtooleBundle.getEntry(pathWithinBundle);
			URL fileUrl = FileLocator.toFileURL(url);
			source = fileUrl.openStream();
		} catch (IOException e) {
			throw error("Unable to load file from within CIMTool bundle:  " + pathWithinBundle, e);
		}
		monitor.worked(1);
		return source;
	}

	private static void writeFile(final IFile file, InputStream source, IProgressMonitor monitor) throws CoreException {
		if (file.exists())
			file.setContents(source, false, true, monitor);
		else
			file.create(source, false, monitor);
	}

	private static IFile replaceSpacesInFileName(IFile file) {
		String modifiedFileName = file.getName().replace(" ", "_");
		IContainer parent = file.getParent();
		return parent.getFile(new org.eclipse.core.runtime.Path(modifiedFileName));
	}

	public static void writeProfile(IFile file, OntModel model, IProgressMonitor monitor) throws CoreException {
		writeOntology(file, model, Format.RDF_XML_WITH_NODEIDS.toFormat(),
				Info.getMultiLineCopyrightText(file.getProject()), monitor);
	}

	public static void writeOntology(IFile file, OntModel model, String format, IProgressMonitor monitor)
			throws CoreException {
		OutputStream stream = new ResourceOutputStream(file, monitor, false, false);
		writeOntology(stream, model, format, monitor);
	}

	public static void writeOntology(IFile file, OntModel model, String format, String copyright,
			IProgressMonitor monitor) throws CoreException {
		OutputStream stream = new ResourceOutputStream(file, monitor, false, false);
		writeOntology(stream, model, format, copyright, monitor);
	}

	public static void writeOntology(OutputStream stream, OntModel model, String format, IProgressMonitor monitor)
			throws CoreException {
		OntResource ont = model.getValidOntology();
		String namespace;
		if (ont != null) {
			namespace = ont.getURI() + "#";
			model.setNsPrefix("", namespace);
		} else
			namespace = null;
		write(model, namespace, true, format, stream);
	}

	public static void writeOntology(OutputStream stream, OntModel model, String format, String copyright,
			IProgressMonitor monitor) throws CoreException {
		OntResource ont = model.getValidOntology();
		String namespace;
		if (ont != null) {
			namespace = ont.getURI() + "#";
			model.setNsPrefix("", namespace);
		} else
			namespace = null;
		write(model, namespace, true, format, stream, copyright);
	}

	public static OntModel getBackgroundModel(IFile file) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		IFolder schema = getSchemaFolder(file.getProject());
		return cache.getMergedOntologyWait(schema);
	}

	public static OntModel getProfileModel(IFile file) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		return fixupProfile(file, cache.getOntologyWait(file), getBackgroundModel(file));
	}

	public static OntModel fixupProfile(IFile file, OntModel model, OntModel backgroundModel) {

		String defaultName, namespace;
		try {
			defaultName = getProperty(file, Info.PROFILE_ENVELOPE);
			namespace = getProperty(file, PROFILE_NAMESPACE);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

		return fixupProfile(model, backgroundModel, defaultName, namespace);
	}

	private static OntModel fixupProfile(OntModel model, OntModel backgroundModel, String defaultName,
			String defaultNamespace) {
		OntResource header = model.getValidOntology();
		if (header != null)
			return model;

		model = Composition.copy(model);

		String namespace = defaultNamespace;
		String label = defaultName;
		String comment = null;

		// harvest and remove old style, mal-formed or repeated headers
		Iterator it = model.listSubjectsWithProperty(RDF.type, OWL2.Ontology).toSet().iterator();
		while (it.hasNext()) {
			OntResource ont = (OntResource) it.next();
			String candLabel = ont.getLabel();
			if (candLabel != null && !candLabel.equals(defaultName))
				label = candLabel;
			String candComment = ont.getComment();
			if (candComment != null && candComment.length() > 0)
				comment = candComment;

			if (ont.isURIResource()) {
				String uri = ont.getURI();
				if (!uri.contains("#") || uri.endsWith("#"))
					namespace = uri;
			}
			ont.remove();
		}

		// remove any untyped, old style header
		MESSAGE.profile.inModel(model).remove();

		initProfile(model, backgroundModel, namespace, label, comment);
		return model;
	}

	public static void initProfile(OntModel profileModel, OntModel backgroundModel, String namespace, String envname,
			String comment) {
		OntResource header = initOntology(profileModel, namespace, envname, comment);

		// add the import to the CIM
		if (backgroundModel != null) {
			OntResource backOnt = backgroundModel.getValidOntology();
			if (backOnt != null) {
				header.addProperty(OWL.imports, backOnt);
				profileModel.setNsPrefix("cim", backOnt.getURI() + "#");
			}
		}
	}

	protected static OntResource initOntology(OntModel model, String namespace, String label, String comment) {
		// add standard ontology header
		if (!namespace.endsWith("#"))
			namespace += "#";
		String uri = namespace.substring(0, namespace.length() - 1);
		OntResource header = model.createResource(uri);
		header.addRDFType(OWL.Ontology);
		header.addLabel(label, null);
		if (comment != null)
			header.addComment(comment, null);
		model.setNsPrefix("", namespace);
		return header;
	}

	public static IWorkspaceRunnable createMappings(final IFile file, final String namespace, final String envname) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				OntModel model = ModelFactory.createMem();
				initOntology(model, namespace, envname, null);
				monitor.worked(1);
				writeMappings(file, model, monitor);
				monitor.worked(1);
			}
		};
	}

	protected static void writeMappings(final IFile file, final OntModel model, IProgressMonitor monitor)
			throws CoreException {
		String ext = file.getFileExtension();
		String format = ext != null && ext.equals("mapping-ttl") ? Format.TURTLE.toFormat() : Format.RDF_XML.toFormat();
		writeOntology(file, model, format, monitor);
	}
}