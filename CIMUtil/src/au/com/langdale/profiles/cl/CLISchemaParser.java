/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import au.com.langdale.cim.CIM;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.logging.SchemaImportLogger;
import au.com.langdale.logging.SchemaImportLoggerFactory;
import au.com.langdale.xmi.CIMInterpreter;
import au.com.langdale.xmi.CIMInterpreterFactory;
import au.com.langdale.xmi.CIMInterpreterResult;
import au.com.langdale.xmi.EAProjectParser;
import au.com.langdale.xmi.EAProjectParserException;
import au.com.langdale.xmi.EAProjectParserFactory;
import au.com.langdale.xmi.NamespacePrefixes;
import au.com.langdale.xmi.StereotypeExtensions;
import au.com.langdale.xmi.XMIParser;

/**
 * CLI-friendly schema parser for CIMTool.
 * 
 * <p>This class parses schema files (.eap, .eapx, .qea, .qeax, .xmi) into
 * an OntModel that can be used for profile transformations. It is a simplified
 * version of the Eclipse-dependent parsing logic in {@code Task.parse()}.</p>
 * 
 * <h2>Supported File Types</h2>
 * <ul>
 *   <li>{@code .eap} - Enterprise Architect Project (JET)</li>
 *   <li>{@code .eapx} - Enterprise Architect Project (JET compressed)</li>
 *   <li>{@code .qea} - Enterprise Architect Project (SQLite)</li>
 *   <li>{@code .qeax} - Enterprise Architect Project (SQLite compressed)</li>
 *   <li>{@code .xmi} - XMI export file</li>
 * </ul>
 * 
 * <h2>Optional Related Files</h2>
 * <p>The parser looks for optional related files in the same directory as the schema:</p>
 * <ul>
 *   <li>{@code <schema>.annotation} - Additional annotations in Turtle format</li>
 *   <li>{@code <schema>.stereotype-extensions} - Stereotype extensions</li>
 *   <li>{@code <schema>.namespace-prefixes} - Namespace prefix mappings</li>
 *   <li>{@code <schema>.namespaces} - Namespace definitions</li>
 * </ul>
 * 
 * @see CLISettings
 */
public class CLISchemaParser {
	
	/** Supported EA project extensions */
	private static final String[] EA_EXTENSIONS = { "eap", "eapx", "qea", "qeax" };
	
	/** XMI extension */
	private static final String XMI_EXTENSION = "xmi";
	
	private final CLISettings settings;
	private final File projectDir;
	private boolean usePackageNames = false;
	
	/**
	 * Create a schema parser with the given settings.
	 * 
	 * @param settings the CLI settings (from .cimtool-settings)
	 * @param projectDir the project directory (for resolving relative paths)
	 */
	public CLISchemaParser(CLISettings settings, File projectDir) {
		this.settings = settings;
		this.projectDir = projectDir;
	}
	
	/**
	 * Set whether to use package names in the schema.
	 * 
	 * @param usePackageNames true to use package names
	 */
	public void setUsePackageNames(boolean usePackageNames) {
		this.usePackageNames = usePackageNames;
	}
	
	/**
	 * Check if a file is a supported schema file type.
	 * 
	 * @param file the file to check
	 * @return true if the file is a supported schema type
	 */
	public static boolean isSupportedSchema(File file) {
		String ext = getExtension(file);
		if (ext == null) {
			return false;
		}
		
		if (ext.equals(XMI_EXTENSION)) {
			return true;
		}
		
		for (String eaExt : EA_EXTENSIONS) {
			if (ext.equals(eaExt)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check if a file is an EA project file.
	 * 
	 * @param file the file to check
	 * @return true if the file is an EA project
	 */
	public static boolean isEAProject(File file) {
		String ext = getExtension(file);
		if (ext == null) {
			return false;
		}
		
		for (String eaExt : EA_EXTENSIONS) {
			if (ext.equals(eaExt)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check if a file is an XMI file.
	 * 
	 * @param file the file to check
	 * @return true if the file is an XMI file
	 */
	public static boolean isXMI(File file) {
		String ext = getExtension(file);
		return XMI_EXTENSION.equals(ext);
	}
	
	/**
	 * Get the list of supported schema file extensions.
	 * 
	 * @return array of supported extensions (without dots)
	 */
	public static String[] getSupportedExtensions() {
		String[] result = new String[EA_EXTENSIONS.length + 1];
		System.arraycopy(EA_EXTENSIONS, 0, result, 0, EA_EXTENSIONS.length);
		result[EA_EXTENSIONS.length] = XMI_EXTENSION;
		return result;
	}
	
	/**
	 * Parse a schema file into an OntModel.
	 * 
	 * @param schemaFile the schema file to parse
	 * @return the parsed OntModel
	 * @throws IOException if the file cannot be read
	 * @throws SchemaParseException if parsing fails
	 */
	public OntModel parse(File schemaFile) throws IOException, SchemaParseException {
		if (!schemaFile.exists()) {
			throw new IOException("Schema file not found: " + schemaFile.getAbsolutePath());
		}
		
		String ext = getExtension(schemaFile);
		if (ext == null) {
			throw new SchemaParseException("Schema file has no extension: " + schemaFile.getName());
		}
		
		if (ext.equals(XMI_EXTENSION)) {
			return parseXMI(schemaFile);
		} else if (isEAProject(schemaFile)) {
			CIMInterpreterResult result = parseEAProject(schemaFile);
			return result.getModel();
		} else {
			throw new SchemaParseException("Unsupported schema file type: " + ext + 
				". Supported types: " + String.join(", ", getSupportedExtensions()));
		}
	}
	
	/**
	 * Parse an XMI file.
	 */
	private OntModel parseXMI(File file) throws IOException, SchemaParseException {
		XMIParser parser = new XMIParser();
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
			parser.parse(is);
		} catch (Exception e) {
			throw new SchemaParseException("Cannot parse XMI file: " + file.getName(), e);
		}
		return interpretSchema(parser.getModel(), file);
	}
	
	/**
	 * Parse an EA project file.
	 */
	private CIMInterpreterResult parseEAProject(File file) throws IOException, SchemaParseException {
		EAProjectParser parser;
		try {
			boolean selfHealOnImport = settings.isSelfHealOnImport();
			SchemaImportLogger logger = SchemaImportLoggerFactory.getLogger(CLISchemaParser.class);
			
			// Load stereotype extensions if available
			Set<String> stereotypeExtensions = new HashSet<>();
			File stereotypeExtensionsFile = getRelatedFile(file, "stereotype-extensions");
			if (stereotypeExtensionsFile.exists()) {
				String projectName = projectDir.getName();
				StereotypeExtensions.initStereotypeExtensions(projectName, stereotypeExtensionsFile);
				stereotypeExtensions = StereotypeExtensions.getStereotypes(projectName);
			}
			
			// Load namespace prefixes if available
			File namespacePrefixesFile = getRelatedFile(file, "namespace-prefixes");
			if (namespacePrefixesFile.exists()) {
				String projectName = projectDir.getName();
				try {
					NamespacePrefixes.init(projectName, namespacePrefixesFile);
				} catch (Exception e) {
					throw new SchemaParseException(
						"Duplicate namespace prefix mapping found in file: " + namespacePrefixesFile.getName(), e);
				}
			}
			
			// Load namespaces file if available
			File namespacesFile = getRelatedFile(file, "namespaces");
			File namespacesFileArg = namespacesFile.exists() ? namespacesFile : null;
			
			// Get schema namespace
			String schemaNamespace = getSchemaNamespace(file);
			
			// Create and run parser
			parser = EAProjectParserFactory.createParser(
				schemaNamespace,
				file,
				selfHealOnImport,
				false, // validateModel
				usePackageNames,
				logger,
				namespacesFileArg,
				stereotypeExtensions
			);
			parser.parse();
			
		} catch (EAProjectParserException e) {
			throw new SchemaParseException("Cannot access EA project: " + file.getName(), e);
		}
		
		// Interpret the raw model
		return interpretSchema(parser.getModel(), file, parser.getStereotypedNamespaces());
	}
	
	/**
	 * Interpret a raw XMI/schema model into a proper CIM model.
	 */
	private OntModel interpretSchema(OntModel raw, File file) throws IOException, SchemaParseException {
		boolean mergeShadowExtensions = settings.isMergeShadowExtensions();
		String base = getSchemaNamespace(file);
		
		// Load annotation file if available
		OntModel annote = loadAnnotationModel(file, base);
		
		// Create interpreter and interpret
		CIMInterpreter interpreter = CIMInterpreterFactory.create(file);
		CIMInterpreterResult result = interpreter.interpret(
			raw, base, annote, usePackageNames, mergeShadowExtensions, false);
		
		return result.getModel();
	}
	
	/**
	 * Interpret a raw EA project model into a proper CIM model.
	 */
	private CIMInterpreterResult interpretSchema(OntModel raw, File file, 
			au.com.langdale.xmi.StereotypedNamespaces stereotypedNamespaces) throws IOException, SchemaParseException {
		boolean mergeShadowExtensions = settings.isMergeShadowExtensions();
		String base = getSchemaNamespace(file);
		
		// Load annotation file if available
		OntModel annote = loadAnnotationModel(file, base);
		
		// Create interpreter and interpret
		CIMInterpreter interpreter = CIMInterpreterFactory.create(stereotypedNamespaces, file);
		return interpreter.interpret(raw, base, annote, usePackageNames, mergeShadowExtensions, false);
	}
	
	/**
	 * Load an annotation model from a related .annotation file if it exists.
	 */
	private OntModel loadAnnotationModel(File schemaFile, String base) throws IOException {
		File annotationFile = getRelatedFile(schemaFile, "annotation");
		if (!annotationFile.exists()) {
			return null;
		}
		
		OntModel annote = ModelFactory.createMem();
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(annotationFile))) {
			IO.read(annote, is, base, Format.TURTLE.toFormat());
		}
		return annote;
	}
	
	/**
	 * Get the schema namespace for a file.
	 * Uses settings if available, otherwise derives from filename.
	 */
	private String getSchemaNamespace(File file) {
		// Try to get from settings
		String ns = settings.getSchemaNamespace(file);
		if (ns != null) {
			return ns;
		}
		
		// Default: if filename starts with "cim", use standard CIM namespace
		if (file.getName().toLowerCase().startsWith("cim")) {
			return CIM.NS;
		}
		
		// Otherwise, use file URI as namespace
		return file.toURI().toString() + "#";
	}
	
	/**
	 * Get a related file with a different extension.
	 * For example, getRelatedFile("schema.eap", "annotation") returns "schema.annotation"
	 */
	private File getRelatedFile(File file, String extension) {
		String name = file.getName();
		int lastDot = name.lastIndexOf('.');
		String baseName = (lastDot > 0) ? name.substring(0, lastDot) : name;
		return new File(file.getParentFile(), baseName + "." + extension);
	}
	
	/**
	 * Get the lowercase extension of a file.
	 */
	private static String getExtension(File file) {
		String name = file.getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot < 0 || lastDot == name.length() - 1) {
			return null;
		}
		return name.substring(lastDot + 1).toLowerCase();
	}
	
	/**
	 * Exception thrown when schema parsing fails.
	 */
	public static class SchemaParseException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public SchemaParseException(String message) {
			super(message);
		}
		
		public SchemaParseException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}