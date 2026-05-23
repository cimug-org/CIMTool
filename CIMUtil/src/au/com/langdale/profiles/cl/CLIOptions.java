/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import java.io.File;

/**
 * Container for command-line options for the CIMTool CLI transformer.
 * 
 * <p>Supports the following options:</p>
 * <ul>
 *   <li>{@code --project-dir <dir>} - CIMTool project directory (required)</li>
 *   <li>{@code --profile <file>} - Single input profile OWL file</li>
 *   <li>{@code --profiles-dir <dir>} - Directory containing profile OWL files (mutually exclusive with --profile)</li>
 *   <li>{@code --builder <n>} - The builder name from builders.json (e.g., "xsd", "json-schema-draft-07")</li>
 *   <li>{@code --xslt <file>} - Path to a custom XSLT stylesheet (alternative to --builder)</li>
 *   <li>{@code --output <dir>} - The output directory</li>
 *   <li>{@code --copyright-multi-line <file>} - File containing multi-line copyright text</li>
 *   <li>{@code --copyright-single-line <file>} - File containing single-line copyright text</li>
 *   <li>{@code --copyright-defaults} - Use bundled default copyright templates</li>
 *   <li>{@code --help} - Show usage information</li>
 * </ul>
 * 
 * <p>The project directory must contain .cimtool-settings and .builder-preferences files.
 * Schema files are read from the Schema/ subdirectory as specified in .cimtool-settings.</p>
 * 
 * <p>If neither --profile nor --profiles-dir is specified, defaults to the Profiles/
 * subdirectory of the project directory. If --output is not specified in 
 * directory mode, output is written to the same directory as the profiles.</p>
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
	private File profilesDir;
	private File projectDir;
	private String builderName;
	private File xsltFile;
	private File outputDir;
	private String outputExtension;
	private File copyrightMultiLineFile;
	private File copyrightSingleLineFile;
	private boolean copyrightDefaultsRequested = false;
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
				
				case "--profiles-dir":
				case "-pf":
					options.profilesDir = new File(requireArg(args, ++i, "--profiles-dir"));
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
		
		// Validate profile options - mutually exclusive
		if (profileFile != null && profilesDir != null) {
			throw new IllegalArgumentException("Cannot specify both --profile and --profiles-dir");
		}
		
		// If neither profile nor profiles-dir specified, default to project's Profiles subdirectory
		if (profileFile == null && profilesDir == null) {
			profilesDir = new File(projectDir, "Profiles");
		}
		
		// Validate profile file if specified
		if (profileFile != null && !profileFile.exists()) {
			throw new IllegalArgumentException("Profile file not found: " + profileFile.getAbsolutePath());
		}
		
		// Validate profiles directory if specified
		if (profilesDir != null) {
			if (!profilesDir.exists()) {
				throw new IllegalArgumentException("Profiles directory not found: " + profilesDir.getAbsolutePath());
			}
			if (!profilesDir.isDirectory()) {
				throw new IllegalArgumentException("--profiles-dir must be a directory: " + profilesDir.getAbsolutePath());
			}
		}
		
		if (builderName != null && xsltFile != null) {
			throw new IllegalArgumentException("Cannot specify both --builder and --xslt");
		}
		
		if (xsltFile != null && !xsltFile.exists()) {
			throw new IllegalArgumentException("XSLT file not found: " + xsltFile.getAbsolutePath());
		}
		
		if (xsltFile != null && outputExtension == null) {
			throw new IllegalArgumentException("--output-ext is required when using --xslt");
		}
		
		// If output not specified in directory mode, default to profiles directory
		if (outputDir == null) {
			if (profilesDir != null) {
				outputDir = profilesDir;
			} else {
				throw new IllegalArgumentException("Missing required option: --output");
			}
		}
		
		if (copyrightDefaultsRequested && (copyrightMultiLineFile != null || copyrightSingleLineFile != null)) {
			throw new IllegalArgumentException("Cannot specify --copyright-defaults with --copyright-multi-line or --copyright-single-line");
		}
		
		if (copyrightMultiLineFile != null && !copyrightMultiLineFile.exists()) {
			throw new IllegalArgumentException("Multi-line copyright file not found: " + copyrightMultiLineFile.getAbsolutePath());
		}
		
		if (copyrightSingleLineFile != null && !copyrightSingleLineFile.exists()) {
			throw new IllegalArgumentException("Single-line copyright file not found: " + copyrightSingleLineFile.getAbsolutePath());
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
			"                                 (schema from Schema/, profiles from Profiles/)\n" +
			"\n" +
			"Profile options (mutually exclusive - if neither specified, uses <project>/Profiles/):\n" +
			"  --profile, -p <file>       Single input profile OWL file\n" +
			"  --profiles-dir, -pf <dir>  Directory containing profile OWL files\n" +
			"\n" +
			"Output options:\n" +
			"  --output, -o <dir>         Output directory (defaults to profiles directory)\n" +
			"\n" +
			"Transform options (optional - if omitted, uses builders flagged in profile):\n" +
			"  --builder, -b <n>       Builder name from builders.json\n" +
			"                             (e.g., xsd, json-schema-draft-07, html)\n" +
			"  --xslt, -x <file>          Path to custom XSLT stylesheet\n" +
			"  --output-ext, -oe <ext>    Output file extension (required when specifying --xslt)\n" +
			"\n" +
			"Copyright options:\n" +
			"  --copyright-multi-line, -cm <file>   Specify a file containing your multi-line copyright text\n" +
			"  --copyright-single-line, -cs <file>  Specify a file containing your single-line copyright text\n" +
			"  --copyright-defaults                 Use bundled default UCAIug Apache 2.0 copyright templates\n" +
			"\n" +
			"Other options:\n" +
			"  --param <key=value>        Additional XSLT parameter (can specify multiple)\n" +
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
	
	public File getProfilesDir() {
		return profilesDir;
	}
	
	public File getProjectDir() {
		return projectDir;
	}
	
	/**
	 * Check if operating in directory mode (processing multiple profiles).
	 * @return true if processing a directory of profiles, false if processing a single profile
	 */
	public boolean isDirectoryMode() {
		return profilesDir != null;
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