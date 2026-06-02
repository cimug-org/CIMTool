/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import au.com.langdale.kena.OntModel;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command-line interface for CIMTool profile transformations.
 * 
 * <p>
 * This class provides a standalone command-line tool for transforming CIM
 * profiles to various output formats using XSLT stylesheets. It can be run
 * without Eclipse.
 * </p>
 * 
 * <h3>Usage:</h3>
 * 
 * <pre>
 * java -jar CIMUtil.jar --project-dir ./MyProject --builder xsd --output ./out
 * </pre>
 * 
 * <p>
 * Run with {@code --help} to see all available options.
 * </p>
 * 
 * @see CLIOptions
 * @see CLIProfileTransformer
 * @see CLISchemaParser
 */
public class CIMToolCLI {

	/** Exit code for successful execution */
	public static final int EXIT_SUCCESS = 0;

	/** Exit code for invalid arguments */
	public static final int EXIT_INVALID_ARGS = 1;

	/** Exit code for transformation errors */
	public static final int EXIT_TRANSFORM_ERROR = 2;

	/** Exit code for I/O errors */
	public static final int EXIT_IO_ERROR = 3;

	/**
	 * Main entry point.
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		// Suppress verbose logging from UCanAccess JDBC driver and its internal
		// HSQLDB engine. These produce spurious warnings about reserved SQL words
		// (e.g. 'user') when reading Sparx EA .eap files, which are harmless but
		// distracting. Only SEVERE-level messages (genuine errors) are retained.
		Logger.getLogger("org.hsqldb").setLevel(Level.SEVERE);
		Logger.getLogger("net.ucanaccess").setLevel(Level.SEVERE);

		int exitCode = run(args);
		System.exit(exitCode);
	}

	/**
	 * Run the CLI with the given arguments.
	 * 
	 * @param args command-line arguments
	 * @return exit code
	 */
	public static int run(String[] args) {
		// Parse arguments
		CLIOptions options;
		try {
			options = CLIOptions.parse(args);
		} catch (IllegalArgumentException e) {
			System.err.println("Error: " + e.getMessage());
			System.err.println();
			System.err.println("Run with --help for usage information.");
			return EXIT_INVALID_ARGS;
		}

		// Handle help request
		if (options.isHelpRequested()) {
			System.out.println(CLIOptions.getUsage());
			return EXIT_SUCCESS;
		}

		// Handle version request
		if (options.isVersionRequested()) {
			System.out.println("CIMTool CLI version " + CLIOptions.getVersion());
			return EXIT_SUCCESS;
		}

		// Handle list-builders request
		if (options.isListBuildersRequested()) {
			System.out.println(listBuilders());
			return EXIT_SUCCESS;
		}

		// Handle empty arguments
		if (args.length == 0) {
			System.out.println(CLIOptions.getUsage());
			return EXIT_SUCCESS;
		}

		// Validate options
		try {
			options.validate();
		} catch (IllegalArgumentException e) {
			System.err.println("Error: " + e.getMessage());
			return EXIT_INVALID_ARGS;
		}

		// Execute transformation
		try {
			return executeTransform(options);
		} catch (Exception e) {
			System.err.println("Error during transformation: " + e.getMessage());
			if (System.getProperty("cimtool.debug") != null) {
				e.printStackTrace(System.err);
			}
			return EXIT_TRANSFORM_ERROR;
		}
	}

	/**
	 * Execute the transformation with the given options.
	 */
	private static int executeTransform(CLIOptions options) throws Exception {
		// Load project settings and builder preferences
		CLISettings settings;
		CLIBuilderPreferences builderPrefs;
		try {
			settings = new CLISettings(options.getProjectDir());
			builderPrefs = new CLIBuilderPreferences(options.getProjectDir());
			System.out.println("Loaded project from: " + options.getProjectDir().getAbsolutePath());
		} catch (IOException e) {
			System.err.println("Error loading project settings: " + e.getMessage());
			return EXIT_IO_ERROR;
		}

		// Get schema files from settings
		List<File> schemaFiles = settings.getSchemaFiles();
		if (schemaFiles.isEmpty()) {
			System.err.println("Error: No schema files found in .cimtool-settings");
			return EXIT_INVALID_ARGS;
		}

		// Validate schema files exist and are supported types - silently skip any
		// stale references to schema files that no longer exist on disk. This can
		// occur in older CIMTool projects where a schema was replaced but the previous
		// entry was not cleaned up from .cimtool-settings.
		List<File> validSchemaFiles = new java.util.ArrayList<>();
		for (File schemaFile : schemaFiles) {
			if (!schemaFile.exists()) {
				continue;
			}
			if (!CLISchemaParser.isSupportedSchema(schemaFile)) {
				System.err.println("Error: Unsupported schema file type: " + schemaFile.getName()
						+ ". Supported types: " + String.join(", ", CLISchemaParser.getSupportedExtensions()));
				return EXIT_INVALID_ARGS;
			}
			validSchemaFiles.add(schemaFile);
		}

		if (validSchemaFiles.isEmpty()) {
			System.err.println("Error: No valid schema files found in .cimtool-settings");
			return EXIT_IO_ERROR;
		}

		// Parse schema files
		CLISchemaParser schemaParser = new CLISchemaParser(settings, options.getProjectDir());
		OntModel mergedSchema = null;

		System.out.println("\nParsing schema(s)...");
		for (File schemaFile : validSchemaFiles) {
			System.out.println("  " + schemaFile.getName());
			try {
				OntModel schema = schemaParser.parse(schemaFile);
				if (mergedSchema == null) {
					mergedSchema = schema;
				} else {
					mergedSchema = au.com.langdale.kena.Composition.merge(mergedSchema, schema);
				}
			} catch (CLISchemaParser.SchemaParseException e) {
				System.err.println("Error parsing schema: " + e.getMessage());
				return EXIT_TRANSFORM_ERROR;
			}
		}

		if (options.isProjectMode()) {
			return executeProjectTransform(options, settings, builderPrefs, mergedSchema);
		} else {
			return executeSingleFileTransform(options, settings, builderPrefs, mergedSchema);
		}
	}

	/**
	 * Execute transformation for a single profile file.
	 */
	private static int executeSingleFileTransform(CLIOptions options, CLISettings settings,
			CLIBuilderPreferences builderPrefs, OntModel schema) throws Exception {
		CLIProfileTransformer transformer = new CLIProfileTransformer(builderPrefs);

		// Load profile
		System.out.println("Loading profile: " + options.getProfileFile().getName());
		transformer.setProfile(options.getProfileFile());

		// Set pre-parsed schema
		transformer.addSchema(schema);

		// Set output directory
		transformer.setOutputDirectory(options.getOutputDir());

		// Handle copyright options
		configureCopyright(transformer, options);

		// Execute transformation(s)
		long startTime = System.currentTimeMillis();

		if (options.getBuilderName() != null) {
			// Explicit builder specified
			System.out.println("Using builder: " + options.getBuilderName());
			transformer.setBuilder(options.getBuilderName());

			File outputFile = transformer.getOutputFile();
			System.out.println("Transforming to: " + outputFile.getName());

			transformer.transform();

			long elapsed = System.currentTimeMillis() - startTime;
			System.out.println("Transformation complete in " + elapsed + "ms");
			System.out.println("Output written to: " + outputFile.getAbsolutePath());

		} else if (options.getXsltFile() != null) {
			// Custom XSLT specified
			System.out.println("Using custom XSLT: " + options.getXsltFile().getName());
			transformer.setXslt(options.getXsltFile(), options.getOutputExtension());

			File outputFile = transformer.getOutputFile();
			System.out.println("Transforming to: " + outputFile.getName());

			transformer.transform();

			long elapsed = System.currentTimeMillis() - startTime;
			System.out.println("Transformation complete in " + elapsed + "ms");
			System.out.println("Output written to: " + outputFile.getAbsolutePath());

		} else {
			// No builder/xslt specified - use builders flagged in profile
			List<String> flaggedBuilders = transformer.getFlaggedBuilders();
			if (flaggedBuilders.isEmpty()) {
				System.err.println("Error: No builders are flagged in the profile.");
				System.err.println("Either specify --builder or enable builders in the profile OWL file.");
				return EXIT_INVALID_ARGS;
			}

			System.out.println("Using builders flagged in profile: " + flaggedBuilders);
			List<File> outputs = transformer.transformFlagged();

			long elapsed = System.currentTimeMillis() - startTime;
			System.out.println("Transformation complete in " + elapsed + "ms");
			System.out.println("Generated " + outputs.size() + " of " + flaggedBuilders.size() + " output file(s):");
			for (File output : outputs) {
				System.out.println("  " + output.getAbsolutePath());
			}
			if (outputs.size() < flaggedBuilders.size()) {
				return EXIT_TRANSFORM_ERROR;
			}
		}

		return EXIT_SUCCESS;
	}

	/**
	 * Execute transformation for all profile files in the project's Profiles directory.
	 */
	private static int executeProjectTransform(CLIOptions options, CLISettings settings,
			CLIBuilderPreferences builderPrefs, OntModel schema) throws Exception {
		File profilesDir = new File(options.getProjectDir(), "Profiles");

		// Find all .owl files in the directory
		File[] profileFiles = profilesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".owl");
			}
		});

		if (profileFiles == null || profileFiles.length == 0) {
			System.err.println("Error: No .owl profile files found in directory: " + profilesDir.getAbsolutePath());
			return EXIT_INVALID_ARGS;
		}

		System.out.println("Found " + profileFiles.length + " profile(s) in: " + profilesDir.getAbsolutePath());
		System.out.println();

		long overallStartTime = System.currentTimeMillis();
		int successCount = 0;
		int failCount = 0;
		int totalOutputs = 0;

		// Process each profile
		for (File profileFile : profileFiles) {
			System.out.println("Processing: " + profileFile.getName());

			try {
				CLIProfileTransformer transformer = new CLIProfileTransformer(builderPrefs);

				// Load profile
				transformer.setProfile(profileFile);

				// Set pre-parsed schema
				transformer.addSchema(schema);

				// Set output directory
				transformer.setOutputDirectory(options.getOutputDir());

				// Handle copyright options
				configureCopyright(transformer, options);

				// Execute transformation(s)
				long startTime = System.currentTimeMillis();

				if (options.getBuilderName() != null) {
					// Explicit builder specified
					transformer.setBuilder(options.getBuilderName());

					transformer.transform();

					File outputFile = transformer.getOutputFile();
					long elapsed = System.currentTimeMillis() - startTime;
					System.out.println("  -> " + outputFile.getName() + " (" + elapsed + "ms)");
					totalOutputs++;

				} else if (options.getXsltFile() != null) {
					// Custom XSLT specified
					transformer.setXslt(options.getXsltFile(), options.getOutputExtension());

					transformer.transform();

					File outputFile = transformer.getOutputFile();
					long elapsed = System.currentTimeMillis() - startTime;
					System.out.println("  -> " + outputFile.getName() + " (" + elapsed + "ms)");
					totalOutputs++;

				} else {
					// No builder/xslt specified - use builders flagged in profile
					List<String> flaggedBuilders = transformer.getFlaggedBuilders();
					if (flaggedBuilders.isEmpty()) {
						System.out.println("  (no builders flagged - skipped)");
					} else {
						List<File> outputs = transformer.transformFlagged();
						long elapsed = System.currentTimeMillis() - startTime;
						for (File output : outputs) {
							System.out.println("  -> " + output.getName());
						}
						System.out.println("  (" + outputs.size() + " file(s) in " + elapsed + "ms)");
						totalOutputs += outputs.size();
					}
				}

				successCount++;

			} catch (Exception e) {
				System.err.println("  Error: " + e.getMessage());
				failCount++;
			}
		}

		long overallElapsed = System.currentTimeMillis() - overallStartTime;
		System.out.println();
		System.out.println("Complete: " + successCount + " profile(s) processed, " + totalOutputs
				+ " output file(s) generated in " + overallElapsed + "ms");

		if (failCount > 0) {
			System.err.println("Warning: " + failCount + " profile(s) failed to process");
			return EXIT_TRANSFORM_ERROR;
		}

		return EXIT_SUCCESS;
	}

	/**
	 * Configure copyright settings on the profile transformer.
	 */
	private static void configureCopyright(CLIProfileTransformer transformer, CLIOptions options) throws Exception {
		if (options.isCopyrightDefaultsRequested()) {
			// Use bundled default templates
			transformer.loadDefaultCopyrightTemplates();
		} else if (options.getCopyrightMultiLineFile() != null || options.getCopyrightSingleLineFile() != null) {
			// Load empty templates first as a baseline
			transformer.loadEmptyCopyrightTemplates();
			// Then override with any specified files
			if (options.getCopyrightMultiLineFile() != null) {
				transformer.loadCopyrightMultiLine(options.getCopyrightMultiLineFile());
			}
			if (options.getCopyrightSingleLineFile() != null) {
				transformer.loadCopyrightSingleLine(options.getCopyrightSingleLineFile());
			}
		} else {
			// No copyright options - use empty templates
			transformer.loadEmptyCopyrightTemplates();
		}
	}

	/**
	 * List available builders. This can be called programmatically to get builder
	 * information.
	 * 
	 * @return formatted string listing all available builders
	 */
	public static String listBuilders() {
		CLIProfileTransformer transformer = new CLIProfileTransformer();
		return transformer.listBuilders();
	}
}
