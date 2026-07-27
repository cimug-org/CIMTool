/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import java.io.File;

import au.com.langdale.profiles.cl.builders.BuilderConfig;
import au.com.langdale.profiles.cl.builders.BuilderRegistry;
import au.com.langdale.profiles.cl.builders.BuilderType;

/**
 * Container for command-line options for the CIMTool CLI transformer.
 * 
 * <p>Supports the following options:</p>
 * <ul>
 *   <li>{@code --project-dir <dir>} - CIMTool project directory (required)</li>
 *   <li>{@code --profile <file>} - Single input profile OWL file</li>
 *   <li>{@code --builder <n>} - The builder name from builders.json (e.g., "xsd", "json-schema-draft-07")</li>
 *   <li>{@code --xslt <file>} - Path to a custom XSLT stylesheet (alternative to --builder)</li>
 *   <li>{@code --type <type>} - Builder type (TEXT, ASCIIDOC, XSD, TRANSFORM) for a custom
 *       --xslt stylesheet; conditionally required (see below)</li>
 *   <li>{@code --output-ext <ext>} - Output file extension for a custom --xslt stylesheet;
 *       conditionally required (see below)</li>
 *   <li>{@code --output <dir>} - The output directory</li>
 *   <li>{@code --copyright-multi-line <file>} - File containing multi-line copyright text</li>
 *   <li>{@code --copyright-single-line <file>} - File containing single-line copyright text</li>
 *   <li>{@code --copyright-defaults} - Use bundled default copyright templates</li>
 *   <li>{@code --copyright-none} - Generate no copyright, overriding project-level templates</li>
 *   <li>{@code --list-builders} - List available builders and exit</li>
 *   <li>{@code --version} - Show version information and exit</li>
 *   <li>{@code --help} - Show usage information</li>
 * </ul>
 * 
 * <p>The project directory must contain .cimtool-settings and .builder-preferences files,
 * and may optionally contain a .cimtool-global-preferences file. Schema files are read
 * from the Schema/ subdirectory as specified in .cimtool-settings.</p>
 * 
 * <p>If --profile is not specified, the CLI operates in project mode and processes all
 * profile OWL files in the Profiles/ subdirectory of the project directory. If --output
 * is not specified, output defaults to the project's Profiles/ subdirectory.</p>
 * 
 * <p>The --builder and --xslt options are mutually exclusive; specifying both is an error.
 * The --type and --output-ext options are valid only in --xslt mode. When the stylesheet
 * filename (minus its .xsl extension) matches a builder registered in builders.json, that
 * builder's type and output extension are inherited and --type / --output-ext become
 * optional overrides. When the stylesheet matches no registered builder, both are required.</p>
 * 
 * <p>Copyright templates are always handled as a pair. At most one of the following may be
 * specified: the --copyright-multi-line and --copyright-single-line pair (both together),
 * --copyright-defaults, or --copyright-none. When none is specified and the project directory
 * contains both a .copyright-multi-line and a .copyright-single-line file, those project-level
 * templates are used. Otherwise no copyright is generated.</p>
 */
public class CLIOptions {

	/**
	 * Returns the version of the CIMTool CLI.
	 *
	 * <p>The version is read at runtime from the {@code Implementation-Version}
	 * attribute in the uber JAR's {@code META-INF/MANIFEST.MF}, which is set
	 * to the Maven project version by the Shade plugin during assembly.</p>
	 *
	 * <p>Returns {@code "unknown"} when running outside the uber JAR (e.g.
	 * directly from Eclipse during development), since no manifest attribute
	 * is available in that context.</p>
	 *
	 * @return the version string, or {@code "unknown"} if not available
	 */
	public static String getVersion() {
		String version = CLIOptions.class.getPackage().getImplementationVersion();
		return (version != null) ? version : "unknown";
	}

	private File profileFile;
	private File projectDir;
	private String builderName;
	private File xsltFile;
	private String xsltTypeValue;
	private BuilderType xsltType;
	private File outputDir;
	private String outputExtension;
	private File copyrightMultiLineFile;
	private File copyrightSingleLineFile;
	private boolean copyrightDefaultsRequested = false;
	private boolean copyrightNoneRequested = false;
	private boolean copyrightFromProject = false;
	private boolean helpRequested = false;
	private boolean listBuildersRequested = false;
	private boolean versionRequested = false;
	
	/**
	 * Parse command-line arguments into CLIOptions.
	 * 
	 * @param args the command-line arguments
	 * @return parsed options
	 * @throws IllegalArgumentException if arguments are invalid
	 */
	public static CLIOptions parse(String[] args) throws IllegalArgumentException {
		CLIOptions options = new CLIOptions();
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			
			switch (arg) {
				case "--help":
				case "-h":
					options.helpRequested = true;
					return options;

				case "--version":
				case "-v":
					options.versionRequested = true;
					return options;
				
				case "--list-builders":
				case "-l":
					options.listBuildersRequested = true;
					return options;
					
				case "--profile":
				case "-p":
					options.profileFile = new File(requireArg(args, ++i, "--profile"));
					break;
				
				case "--project-dir":
				case "-pd":
					options.projectDir = new File(requireArg(args, ++i, "--project-dir"));
					break;
					
				case "--builder":
				case "-b":
					options.builderName = requireArg(args, ++i, "--builder");
					break;
					
				case "--xslt":
				case "-x":
					options.xsltFile = new File(requireArg(args, ++i, "--xslt"));
					break;
					
				case "--type":
				case "-t":
					options.xsltTypeValue = requireArg(args, ++i, "--type");
					break;
					
				case "--output":
				case "-o":
					options.outputDir = new File(requireArg(args, ++i, "--output"));
					break;
				
				case "--output-ext":
				case "-oe":
					options.outputExtension = requireArg(args, ++i, "--output-ext");
					break;
					
				case "--copyright-multi-line":
				case "-cm":
					options.copyrightMultiLineFile = new File(requireArg(args, ++i, "--copyright-multi-line"));
					break;
				
				case "--copyright-single-line":
				case "-cs":
					options.copyrightSingleLineFile = new File(requireArg(args, ++i, "--copyright-single-line"));
					break;
				
				case "--copyright-defaults":
					options.copyrightDefaultsRequested = true;
					break;
				
				case "--copyright-none":
					options.copyrightNoneRequested = true;
					break;
				
				default:
					if (arg.startsWith("-")) {
						throw new IllegalArgumentException("Unknown option: " + arg);
					}
					throw new IllegalArgumentException("Unexpected argument: " + arg);
			}
		}
		
		return options;
	}
	
	/**
	 * Validate that all required options are present and files exist.
	 * 
	 * @throws IllegalArgumentException if validation fails
	 */
	public void validate() throws IllegalArgumentException {
		if (helpRequested || listBuildersRequested || versionRequested) {
			return;
		}
		
		// Validate project directory first (required, and needed for defaults)
		if (projectDir == null) {
			throw new IllegalArgumentException("Missing required option: --project-dir");
		}
		if (!projectDir.exists()) {
			throw new IllegalArgumentException("Project directory not found: " + projectDir.getAbsolutePath());
		}
		if (!projectDir.isDirectory()) {
			throw new IllegalArgumentException("--project-dir must be a directory: " + projectDir.getAbsolutePath());
		}
		
		// Verify required files exist in project directory
		File settingsFile = new File(projectDir, ".cimtool-settings");
		if (!settingsFile.exists()) {
			throw new IllegalArgumentException("Settings file not found: " + settingsFile.getAbsolutePath());
		}
		File builderPrefsFile = new File(projectDir, ".builder-preferences");
		if (!builderPrefsFile.exists()) {
			throw new IllegalArgumentException("Builder preferences file not found: " + builderPrefsFile.getAbsolutePath());
		}
		
		// Validate profile file if specified. When --profile is omitted the CLI
		// operates in project mode, processing all .owl files in <project-dir>/Profiles.
		if (profileFile != null && !profileFile.exists()) {
			throw new IllegalArgumentException("Profile file not found: " + profileFile.getAbsolutePath());
		}
		
		if (builderName != null && xsltFile != null) {
			throw new IllegalArgumentException("Cannot specify both --builder and --xslt");
		}
		
		if (xsltFile != null && !xsltFile.exists()) {
			throw new IllegalArgumentException("XSLT file not found: " + xsltFile.getAbsolutePath());
		}
		
		// --type and --output-ext are meaningful only for a custom --xslt stylesheet. In
		// --builder mode the registered builder is authoritative, and in flagged-builder
		// mode each flagged builder supplies its own. Rejecting them elsewhere avoids
		// silently discarding a value the user supplied. See defect #287.
		if (xsltTypeValue != null && xsltFile == null) {
			throw new IllegalArgumentException("--type is only valid when --xslt is specified");
		}
		if (outputExtension != null && xsltFile == null) {
			throw new IllegalArgumentException("--output-ext is only valid when --xslt is specified");
		}
		
		if (xsltFile != null) {
			resolveXsltTypeAndExtension();
		}
		
		// If output not specified, default to the project's Profiles subdirectory.
		// This applies in all modes (single-profile and project), matching the
		// desktop application, which writes generated artifacts alongside the
		// source profiles.
		if (outputDir == null) {
			outputDir = new File(projectDir, "Profiles");
		}
		
		resolveCopyright();
	}
	
	/**
	 * Resolve the builder type and output extension for a custom --xslt stylesheet.
	 * 
	 * <p>
	 * Each value is resolved independently using the same precedence: an explicitly
	 * specified option always wins; otherwise the value is inherited from the
	 * registered builder whose style matches the stylesheet filename; otherwise the
	 * option is required and its absence is an error.
	 * </p>
	 */
	private void resolveXsltTypeAndExtension() throws IllegalArgumentException {
		if (xsltTypeValue != null) {
			BuilderType parsedType = BuilderType.fromString(xsltTypeValue);
			if (parsedType == BuilderType.JAVA) {
				throw new IllegalArgumentException(
						"--type JAVA is not valid: JAVA builders are not XSLT-based. Valid types are: TEXT, ASCIIDOC, XSD, TRANSFORM");
			}
			if (parsedType == null) {
				throw new IllegalArgumentException("Invalid --type value: " + xsltTypeValue
						+ ". Valid types are: TEXT, ASCIIDOC, XSD, TRANSFORM");
			}
			xsltType = parsedType;
		}
		
		BuilderConfig registeredBuilder = BuilderRegistry.findBuilderForStylesheet(xsltFile);
		if (registeredBuilder != null) {
			if (xsltType == null) {
				xsltType = registeredBuilder.getType();
			}
			if (outputExtension == null) {
				outputExtension = registeredBuilder.getExtension();
			}
			return;
		}
		
		if (xsltType != null && outputExtension != null) {
			return;
		}
		
		// Nothing to inherit and at least one value missing. Report both missing values
		// in a single diagnostic so the user is not made to correct them one at a time.
		boolean missingType = (xsltType == null);
		boolean missingExtension = (outputExtension == null);
		String javaCollision = BuilderRegistry.findJavaBuilderCollision(xsltFile);
		StringBuilder message = new StringBuilder();
		if (javaCollision != null) {
			message.append("The stylesheet '").append(xsltFile.getName())
					.append("' matches the name of the JAVA builder '").append(javaCollision)
					.append("'. JAVA builders are not XSLT-based, so a type and extension cannot be inherited from it. ");
		} else {
			message.append("The stylesheet '").append(xsltFile.getName())
					.append("' does not match any registered builder. ");
		}
		if (missingType && missingExtension) {
			message.append("Both --type and --output-ext are required.");
		} else if (missingType) {
			message.append("--type is required.");
		} else {
			message.append("--output-ext is required.");
		}
		if (missingType) {
			message.append(" Valid types are: TEXT, ASCIIDOC, XSD, TRANSFORM.");
		}
		if (javaCollision != null) {
			message.append(" (To run the JAVA builder itself, use --builder ").append(javaCollision)
					.append(" instead.)");
		}
		throw new IllegalArgumentException(message.toString());
	}
	
	/**
	 * Resolve the copyright templates.
	 * 
	 * <p>
	 * Copyright templates are always handled as a pair, matching the desktop
	 * application, where they are imported as a pair. At most one selection may be
	 * made: the --copyright-multi-line and --copyright-single-line pair,
	 * --copyright-defaults, or --copyright-none. When no selection is made, the
	 * project directory is consulted for a .copyright-multi-line and
	 * .copyright-single-line pair. When that is absent as well, no copyright is
	 * generated.
	 * </p>
	 */
	private void resolveCopyright() throws IllegalArgumentException {
		boolean explicitFilesSpecified = (copyrightMultiLineFile != null || copyrightSingleLineFile != null);
		
		int selections = 0;
		if (explicitFilesSpecified) {
			selections++;
		}
		if (copyrightDefaultsRequested) {
			selections++;
		}
		if (copyrightNoneRequested) {
			selections++;
		}
		if (selections > 1) {
			throw new IllegalArgumentException(
					"At most one copyright selection may be specified: the --copyright-multi-line and "
							+ "--copyright-single-line pair, --copyright-defaults, or --copyright-none");
		}
		
		if (explicitFilesSpecified) {
			if (copyrightMultiLineFile == null) {
				throw new IllegalArgumentException(
						"Copyright templates must be specified as a pair: --copyright-multi-line is missing");
			}
			if (copyrightSingleLineFile == null) {
				throw new IllegalArgumentException(
						"Copyright templates must be specified as a pair: --copyright-single-line is missing");
			}
			if (!copyrightMultiLineFile.exists()) {
				throw new IllegalArgumentException(
						"Multi-line copyright file not found: " + copyrightMultiLineFile.getAbsolutePath());
			}
			if (!copyrightSingleLineFile.exists()) {
				throw new IllegalArgumentException(
						"Single-line copyright file not found: " + copyrightSingleLineFile.getAbsolutePath());
			}
			return;
		}
		
		if (copyrightDefaultsRequested || copyrightNoneRequested) {
			return;
		}
		
		// No explicit selection. Fall back to project-level templates when the project
		// provides a complete pair, matching the behavior of the desktop application.
		File projectMultiLine = new File(projectDir, ".copyright-multi-line");
		File projectSingleLine = new File(projectDir, ".copyright-single-line");
		boolean hasMultiLine = projectMultiLine.exists();
		boolean hasSingleLine = projectSingleLine.exists();
		
		if (hasMultiLine && hasSingleLine) {
			copyrightMultiLineFile = projectMultiLine;
			copyrightSingleLineFile = projectSingleLine;
			copyrightFromProject = true;
		} else if (hasMultiLine || hasSingleLine) {
			throw new IllegalArgumentException("The project provides only "
					+ (hasMultiLine ? ".copyright-multi-line" : ".copyright-single-line")
					+ ". Copyright templates are used as a pair, so both files are required. "
					+ "Add the missing file, or specify --copyright-defaults or --copyright-none.");
		}
	}
	
	private static String requireArg(String[] args, int index, String option) {
		if (index >= args.length) {
			throw new IllegalArgumentException("Missing argument for option: " + option);
		}
		return args[index];
	}
	
	/**
	 * Get usage information string.
	 */
	public static String getUsage() {
		return 
			"CIMTool Command Line Interface\n" +
			"\n" +
			"Usage: java -jar cimtool-cli.jar [options]\n" +
			"\n" +
			"Required options:\n" +
			"  --project-dir, -pd <dir>  CIMTool project directory containing\n" +
			"                                 .cimtool-settings and .builder-preferences\n" +
			"                                 (.cimtool-global-preferences optional)\n" +
			"                                 (schema from Schema/, profiles from Profiles/)\n" +
			"\n" +
			"Profile option (if omitted, all .owl files in <project>/Profiles/ are processed):\n" +
			"  --profile, -p <file>       Single input profile OWL file\n" +
			"\n" +
			"Output options:\n" +
			"  --output, -o <dir>         Output directory (defaults to <project>/Profiles/)\n" +
			"\n" +
			"Transform options (optional - if omitted, uses builders flagged in profile):\n" +
			"  --builder, -b <n>       Builder name from builders.json\n" +
			"                             (e.g., xsd, json-schema-draft-07, html)\n" +
			"  --xslt, -x <file>          Path to custom XSLT stylesheet\n" +
			"  --type, -t <type>          Builder type for the custom --xslt stylesheet\n" +
			"                             (one of: TEXT, ASCIIDOC, XSD, TRANSFORM)\n" +
			"  --output-ext, -oe <ext>    Output file extension for the custom --xslt stylesheet\n" +
			"  Note: --builder and --xslt are mutually exclusive; specify at most one.\n" +
			"  Note: --type and --output-ext are valid only with --xslt. If the stylesheet\n" +
			"        filename (minus .xsl) matches a registered builder, its type and\n" +
			"        extension are inherited and both options become optional overrides.\n" +
			"        Otherwise both are required.\n" +
			"\n" +
			"Copyright options:\n" +
			"  --copyright-multi-line, -cm <file>   Specify a file containing your multi-line copyright text\n" +
			"  --copyright-single-line, -cs <file>  Specify a file containing your single-line copyright text\n" +
			"  --copyright-defaults                 Use bundled default UCAIug Apache 2.0 copyright templates\n" +
			"  --copyright-none                     Generate no copyright, ignoring project-level templates\n" +
			"  Note: copyright templates are used as a pair. Specify at most one of the\n" +
			"        --copyright-multi-line and --copyright-single-line pair (both together),\n" +
			"        --copyright-defaults, or --copyright-none. When none is specified and the\n" +
			"        project contains both a .copyright-multi-line and a .copyright-single-line\n" +
			"        file, those are used automatically.\n" +
			"\n" +
			"Other options:\n" +
			"  --list-builders, -l        List available builders\n" +
			"  --version, -v              Show version information\n" +
			"  --help, -h                 Show this help message\n" +
			"\n" +
			"Examples:\n" +
			"  # Process all profiles in project's Profiles/ directory using flagged builders\n" +
			"  java -jar cimtool-cli.jar --project-dir ./MyProject\n" +
			"\n" +
			"  # Process all profiles with output to a specific directory\n" +
			"  java -jar cimtool-cli.jar --project-dir ./MyProject --output ./output\n" +
			"\n" +
			"  # Process single profile with explicit builder\n" +
			"  java -jar cimtool-cli.jar --project-dir ./MyProject \\\n" +
			"       --profile ./MyProject/Profiles/MyProfile.owl --builder xsd --output ./output\n" +
			"\n" +
			"  # Custom XSLT matching a registered builder (type and extension inherited)\n" +
			"  java -jar cimtool-cli.jar --project-dir ./MyProject \\\n" +
			"       --xslt ./my-builders/sql-rdfs-ansi92.xsl --output ./output\n" +
			"\n" +
			"  # Custom XSLT not in the registry (--type and --output-ext required)\n" +
			"  java -jar cimtool-cli.jar --project-dir ./MyProject \\\n" +
			"       --xslt ./my-custom-report.xsl --type TEXT --output-ext txt --output ./output\n" +
			"\n" +
			"  # Show version\n" +
			"  java -jar cimtool-cli.jar --version\n" +
			"\n" +
			"  # List available builders\n" +
			"  java -jar cimtool-cli.jar --list-builders\n";
	}
	
	// Getters
	
	public File getProfileFile() {
		return profileFile;
	}
	
	public File getProjectDir() {
		return projectDir;
	}
	
	/**
	 * Check if operating in project mode (processing all profiles in the project's
	 * Profiles directory) rather than single-profile mode.
	 * @return true if no single profile was specified (project mode), false if a single profile was specified
	 */
	public boolean isProjectMode() {
		return profileFile == null;
	}
	
	public String getBuilderName() {
		return builderName;
	}
	
	public File getXsltFile() {
		return xsltFile;
	}
	
	public File getOutputDir() {
		return outputDir;
	}
	
	/**
	 * Get the resolved builder type for a custom --xslt stylesheet. Populated by
	 * {@link #validate()}: either the explicitly specified --type value or the type
	 * inherited from the registered builder matching the stylesheet filename.
	 * 
	 * @return the resolved builder type, or null when not in --xslt mode
	 */
	public BuilderType getXsltType() {
		return xsltType;
	}
	
	public String getOutputExtension() {
		return outputExtension;
	}
	
	public File getCopyrightMultiLineFile() {
		return copyrightMultiLineFile;
	}
	
	public File getCopyrightSingleLineFile() {
		return copyrightSingleLineFile;
	}
	
	public boolean isCopyrightDefaultsRequested() {
		return copyrightDefaultsRequested;
	}
	
	public boolean isCopyrightNoneRequested() {
		return copyrightNoneRequested;
	}
	
	/**
	 * Check whether the copyright templates were taken from the project directory
	 * rather than specified on the command line.
	 * 
	 * @return true when project-level copyright templates are in use
	 */
	public boolean isCopyrightFromProject() {
		return copyrightFromProject;
	}
	
	public boolean isHelpRequested() {
		return helpRequested;
	}
	
	public boolean isListBuildersRequested() {
		return listBuildersRequested;
	}

	public boolean isVersionRequested() {
		return versionRequested;
	}
}
