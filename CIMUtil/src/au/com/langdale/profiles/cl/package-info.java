/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */

/**
 * Command-line interface for CIMTool profile transformations.
 * 
 * <p>This package provides a standalone command-line tool for transforming CIM profiles
 * to various output formats using XSLT stylesheets. It can be run without Eclipse,
 * making CIMTool's transformation capabilities available for CI/CD pipelines,
 * scripting, and other automation scenarios.</p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link au.com.langdale.profiles.cl.CIMToolCLI} - Main entry point</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLIOptions} - Command-line options</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLIProfileTransformer} - Core transformation logic</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLISchemaParser} - Schema file parsing (.eap, .qea, .xmi)</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLISettings} - Project settings reader</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLIBuilderPreferences} - Builder preferences reader</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLIGlobalPreferences} - Global preferences reader</li>
 *   <li>{@link au.com.langdale.profiles.cl.CLIGlobalPreferenceDefaults} - Default preference values</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * <pre>
 * # Process all profiles in project's Profiles/ directory using flagged builders
 * java -jar CIMUtil.jar --project-dir ./MyProject
 * 
 * # Process all profiles with output to a specific directory
 * java -jar CIMUtil.jar --project-dir ./MyProject --output ./output
 * 
 * # Process single profile using flagged builders
 * java -jar CIMUtil.jar --project-dir ./MyProject \
 *     --profile ./MyProject/Profiles/MyProfile.owl --output ./output
 * 
 * # Process single profile with explicit builder
 * java -jar CIMUtil.jar --project-dir ./MyProject \
 *     --profile ./MyProject/Profiles/MyProfile.owl --builder xsd --output ./output
 * 
 * # List available builders
 * java -jar CIMUtil.jar --list-builders
 * 
 * # Show help
 * java -jar CIMUtil.jar --help
 * </pre>
 * 
 * <h2>Programmatic Usage</h2>
 * <pre>
 * // Load settings and parse schema from project
 * CLISettings settings = new CLISettings(new File("./MyProject"));
 * CLISchemaParser schemaParser = new CLISchemaParser(settings, new File("./MyProject"));
 * 
 * // Parse schema file (automatically found from .cimtool-settings)
 * File schemaFile = settings.getSchemaFile();
 * OntModel schema = schemaParser.parse(schemaFile);
 * 
 * // Transform profile
 * CLIProfileTransformer transformer = new CLIProfileTransformer();
 * transformer.setProfile(new File("MyProfile.owl"));
 * transformer.addSchema(schema);
 * transformer.setBuilder("xsd");
 * transformer.setOutputDirectory(new File("./output"));
 * transformer.transform();
 * </pre>
 * 
 * @since 2.3.0
 */
package au.com.langdale.profiles.cl;