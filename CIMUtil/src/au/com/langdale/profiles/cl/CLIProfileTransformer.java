/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl;

import au.com.langdale.kena.Composition;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.OWLGenerator;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileSerializer;
import au.com.langdale.profiles.RDFSBasedGenerator;
import au.com.langdale.profiles.RDFSGenerator;
import au.com.langdale.profiles.cl.builders.BuilderConfig;
import au.com.langdale.profiles.cl.builders.BuilderRegistry;
import au.com.langdale.profiles.cl.builders.BuilderType;
import au.com.langdale.profiles.cl.builders.JavaGeneratorType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Eclipse-free profile transformer for command-line use.
 * 
 * <p>
 * This class provides the core transformation logic for converting CIM profiles
 * to various output formats using XSLT stylesheets. It can be used standalone
 * without any Eclipse dependencies.
 * </p>
 * 
 * <p>
 * The output filename is derived from the profile filename, with the extension
 * determined by the builder configuration or specified explicitly for custom
 * XSLT.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * CLIProfileTransformer transformer = new CLIProfileTransformer();
 * transformer.setProfile(new File("MyProfile.owl"));
 * transformer.addSchema(schemaModel); // Pre-parsed schema OntModel
 * transformer.setBuilder("xsd"); // Sets output extension to "xsd"
 * transformer.setOutputDirectory(new File("./output"));
 * transformer.transform(); // Creates ./output/MyProfile.xsd
 * </pre>
 */
public class CLIProfileTransformer {

	/**
	 * Namespace for buildlet identifiers stored in profile OWL files. When a
	 * builder is enabled, the profile contains:
	 * {@code <http://langdale.com.au/2007/Buildlet#extension> rdf:type <http://langdale.com.au/2005/Message#Flag>}
	 */
	public static final String BUILDLET_NS = "http://langdale.com.au/2007/Buildlet#";

	private File profileFile;
	private OntModel profileModel;
	private OntModel backgroundModel;
	private ProfileModel messageModel;

	private String builderName;
	private File xsltFile;
	private File outputDir;
	private String outputExtension;

	private String copyrightMultiLine = "";
	private String copyrightSingleLine = "";

	private BuilderRegistry builderRegistry;
	private BuilderType builderType;
	private CLIBuilderPreferences builderPreferences;

	/**
	 * Create a new CLIProfileTransformer using the default BuilderRegistry.
	 */
	public CLIProfileTransformer() {
		this.builderRegistry = BuilderRegistry.getInstance();
	}

	/**
	 * Create a new CLIProfileTransformer using the default BuilderRegistry.
	 */
	public CLIProfileTransformer(CLIBuilderPreferences builderPrefs) {
		this.builderPreferences = builderPrefs;
		this.builderRegistry = BuilderRegistry.getInstance();
	}

	/**
	 * Create a new CLIProfileTransformer with a custom BuilderRegistry.
	 * 
	 * @param registry the builder registry to use
	 */
	public CLIProfileTransformer(CLIBuilderPreferences builderPrefs, BuilderRegistry registry) {
		this.builderPreferences = builderPrefs;
		this.builderRegistry = registry;
	}

	/**
	 * Set the profile file to transform.
	 * 
	 * @param profileFile the profile OWL file
	 * @throws IOException if the file cannot be read
	 */
	public void setProfile(File profileFile) throws IOException {
		this.profileFile = profileFile;
		this.profileModel = loadOntModel(profileFile);
	}

	/**
	 * Add a schema file to the background model. Multiple schema files can be added
	 * and will be merged.
	 * 
	 * @param schemaFile the schema OWL file
	 * @throws IOException if the file cannot be read
	 * @deprecated Use {@link #addSchema(OntModel)} with a pre-parsed schema instead
	 */
	@Deprecated
	public void addSchema(File schemaFile) throws IOException {
		OntModel schema = loadOntModel(schemaFile);
		if (backgroundModel == null) {
			backgroundModel = schema;
		} else {
			backgroundModel = Composition.merge(backgroundModel, schema);
		}
	}

	/**
	 * Add a pre-parsed schema model to the background model. Multiple schema models
	 * can be added and will be merged.
	 * 
	 * @param schema the parsed schema OntModel
	 */
	public void addSchema(OntModel schema) {
		if (backgroundModel == null) {
			backgroundModel = schema;
		} else {
			backgroundModel = Composition.merge(backgroundModel, schema);
		}
	}

	/**
	 * Set multiple schema files at once.
	 * 
	 * @param schemaFiles list of schema OWL files
	 * @throws IOException if any file cannot be read
	 * @deprecated Use {@link #addSchema(OntModel)} with pre-parsed schemas instead
	 */
	@Deprecated
	public void setSchemas(List<File> schemaFiles) throws IOException {
		backgroundModel = null;
		for (File schemaFile : schemaFiles) {
			addSchema(schemaFile);
		}
	}

	/**
	 * Set the builder to use by name (from builders.json).
	 * 
	 * @param builderName the builder name (e.g., "xsd", "json-schema-draft-07")
	 * @throws IllegalArgumentException if the builder is not found
	 */
	public void setBuilder(String builderName) {
		if (!builderRegistry.hasBuilder(builderName)) {
			throw new IllegalArgumentException(
					"Unknown builder: " + builderName + ". Available builders: " + builderRegistry.getBuilderNames());
		}
		this.builderName = builderName;
		this.xsltFile = null;
		// Capture the output extension from the builder config
		BuilderConfig config = builderRegistry.getBuilder(builderName);
		this.builderType = config.getType();
		this.outputExtension = config.getExtension();
	}

	/**
	 * Sets the builder preferences associated with the project.
	 * 
	 * @param builderPreferences the builder preferences to be used during XSLT
	 *                           transformations.
	 */
	public void setBuilderPreferences(CLIBuilderPreferences builderPreferences) {
		this.builderPreferences = builderPreferences;
	}

	/**
	 * Set a custom XSLT file to use for transformation.
	 * 
	 * @param xsltFile        the XSLT stylesheet file
	 * @param outputExtension the file extension for output (e.g., "xml", "json")
	 */
	public void setXslt(File xsltFile, String outputExtension) {
		this.xsltFile = xsltFile;
		this.builderName = null;
		this.outputExtension = outputExtension;
	}

	/**
	 * Set the output directory path. The actual output filename will be derived
	 * from the profile filename with the extension from the builder configuration.
	 * 
	 * @param outputDir the output directory
	 */
	public void setOutputDirectory(File outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * Get the computed output file path. This is the profile filename with the
	 * builder's extension, placed in the output directory.
	 * 
	 * @return the output file, or null if profile or output directory not set
	 */
	public File getOutputFile() {
		if (profileFile == null || outputDir == null || outputExtension == null) {
			return null;
		}
		String profileName = profileFile.getName();
		int dotIndex = profileName.lastIndexOf('.');
		if (dotIndex > 0) {
			profileName = profileName.substring(0, dotIndex);
		}
		return new File(outputDir, profileName + "." + outputExtension);
	}

	/**
	 * Set the multi-line copyright text.
	 * 
	 * @param copyright the copyright text
	 */
	public void setCopyrightMultiLine(String copyright) {
		this.copyrightMultiLine = copyright != null ? copyright : "";
	}

	/**
	 * Set the single-line copyright text.
	 * 
	 * @param copyright the copyright text
	 */
	public void setCopyrightSingleLine(String copyright) {
		this.copyrightSingleLine = copyright != null ? copyright : "";
	}

	/**
	 * Load multi-line copyright text from a file.
	 * 
	 * @param copyrightFile the file containing multi-line copyright text
	 * @throws IOException if the file cannot be read
	 */
	public void loadCopyrightMultiLine(File copyrightFile) throws IOException {
		String copyright = new String(Files.readAllBytes(copyrightFile.toPath()), StandardCharsets.UTF_8);
		setCopyrightMultiLine(copyright);
	}

	/**
	 * Load single-line copyright text from a file.
	 * 
	 * @param copyrightFile the file containing single-line copyright text
	 * @throws IOException if the file cannot be read
	 */
	public void loadCopyrightSingleLine(File copyrightFile) throws IOException {
		String copyright = new String(Files.readAllBytes(copyrightFile.toPath()), StandardCharsets.UTF_8);
		// Ensure it's actually single-line by replacing any newlines
		copyright = copyright.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim();
		setCopyrightSingleLine(copyright);
	}

	/**
	 * Load empty copyright templates from bundled resources. This sets both
	 * multi-line and single-line copyrights to empty.
	 * 
	 * @throws IOException if the bundled resources cannot be read
	 */
	public void loadEmptyCopyrightTemplates() throws IOException {
		String emptyCopyright = loadBundledCopyrightTemplate("empty-copyright-template.txt");
		setCopyrightMultiLine(emptyCopyright);
		setCopyrightSingleLine(emptyCopyright);
	}

	/**
	 * Load default copyright templates from bundled resources.
	 * 
	 * @throws IOException if the bundled resources cannot be read
	 */
	public void loadDefaultCopyrightTemplates() throws IOException {
		String singleLine = loadBundledCopyrightTemplate("default-copyright-template-single-line.txt");
		setCopyrightSingleLine(singleLine);
		//
		String multiLine = loadBundledCopyrightTemplate("default-copyright-template-multi-line.txt");
		setCopyrightMultiLine(multiLine);
	}

	/**
	 * Load a copyright template from bundled resources.
	 */
	private String loadBundledCopyrightTemplate(String templateName) throws IOException {
		// The builders/ folder is on the Bundle-ClassPath, so files are at classpath
		// root
		String resourcePath = "/" + templateName;
		try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
			if (is == null) {
				throw new IOException("Bundled copyright template not found: " + templateName);
			}
			byte[] bytes = new byte[is.available()];
			int totalRead = 0;
			while (totalRead < bytes.length) {
				int read = is.read(bytes, totalRead, bytes.length - totalRead);
				if (read < 0)
					break;
				totalRead += read;
			}
			return new String(bytes, 0, totalRead, StandardCharsets.UTF_8);
		}
	}

	/**
	 * Execute the transformation.
	 *
	 * <p>
	 * If the configured builder is of type {@link BuilderType#JAVA}, execution is
	 * dispatched directly to {@link #executeJavaBuilder} without going through the
	 * XSLT pipeline. Otherwise the standard XSLT path is followed.
	 * </p>
	 * 
	 * @throws IOException          if files cannot be read or written
	 * @throws TransformerException if the XSLT transformation fails
	 */
	public void transform() throws IOException, TransformerException {
		validateState();

		// Dispatch Java-based builders directly — no XSLT pipeline needed
		if (builderName != null) {
			BuilderConfig config = builderRegistry.getBuilder(builderName);
			if (config != null && config.getType() == BuilderType.JAVA) {
				ProfileModel pm = null;
				if (config.getJavaGenerator() == JavaGeneratorType.PROFILE_SERIALIZER) {
					pm = new ProfileModel();
					pm.setOntModel(profileModel);
					pm.setBackgroundModel(backgroundModel);
				}
				executeJavaBuilder(config, pm);
				return;
			}
		}

		// XSLT path
		// Compute the output file path
		File outputFile = getOutputFile();

		// Build the message model
		messageModel = new ProfileModel();
		messageModel.setOntModel(profileModel);
		messageModel.setBackgroundModel(backgroundModel);

		// Create serializer
		ProfileSerializer serializer = new ProfileSerializer(messageModel);

		// Enable console error output for CLI transformations
		serializer.enableConsoleErrorOutput();

		serializer.setBaseURI(messageModel.getNamespace());
		serializer.setOntologyURI(getOntologyURI());
		serializer.setVersion("Beta");
		serializer.setCopyrightMultiLine(copyrightMultiLine);
		serializer.setCopyrightSingleLine(copyrightSingleLine);
		serializer.setFileName(outputFile.getName().substring(0, outputFile.getName().indexOf(".")));

		// Set builder specific preferences passed to the serializer as parameters.
		serializer.setBuilderParameters(builderPreferences.getBuilderParameters(getOutputFile()));
		serializer.setNamespacePrefixesBuilderParameter(
				getNamespacePrefixesBuilderParameter(messageModel.getNsPrefixMap()));

		// Load stylesheet
		InputStream xsltStream = getXsltInputStream();
		try {
			serializer.setStyleSheet(xsltStream, ProfileSerializer.XSDGEN);
			if (builderType == BuilderType.TEXT) {
				serializer.addStyleSheet("indent");
			}
		} catch (TransformerConfigurationException e) {
			throw new TransformerException("Failed to load XSLT stylesheet", e);
		} finally {
			xsltStream.close();
		}

		// Ensure output directory exists
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		// Execute transformation
		try (OutputStream out = new FileOutputStream(outputFile)) {
			serializer.write(out);
		}

		// Check for XSLT transformation errors
		if (serializer.hasTransformationErrors()) {
			throw new TransformerException("XSLT transformation completed with errors for: " + outputFile.getName());
		}
	}

	/**
	 * Execute transformations for all builders flagged in the profile.
	 * 
	 * <p>
	 * This method reads the profile OWL file to determine which builders are
	 * enabled (flagged), then runs each one. XSLT-based builders are executed via
	 * the XSLT pipeline. Java-based builders ({@link BuilderType#JAVA}) are
	 * executed directly via their generator classes without XSLT.
	 * </p>
	 * 
	 * <p>
	 * Each builder is executed independently. If one fails, the error and full
	 * stack trace are printed to {@code stderr} and processing continues with the
	 * remaining builders. A summary of successes and failures is printed at the
	 * end. The return value contains only the files that were successfully
	 * generated.
	 * </p>
	 * 
	 * @return list of output files successfully generated
	 * @throws IOException if files cannot be read
	 */
	public List<File> transformFlagged() throws IOException {
		validateStateForFlagged();

		List<String> flaggedBuilders = getFlaggedBuilders();
		if (flaggedBuilders.isEmpty()) {
			throw new IllegalStateException("No builders are flagged in the profile. "
					+ "Use --builder to specify one, or enable builders in the profile within the 'Profile Summary' tab.");
		}

		List<File> outputs = new ArrayList<>();
		List<String> failures = new ArrayList<>();

		// Lazily constructed ProfileModel — only needed for the xml/PROFILE_SERIALIZER
		// builder; all other builders use OntModel directly.
		ProfileModel lazyProfileModel = null;

		for (String builder : flaggedBuilders) {
			System.out.println("  Running builder: " + builder);
			try {
				BuilderConfig config = builderRegistry.getBuilder(builder);
				if (config != null && config.getType() == BuilderType.JAVA) {
					// Java-based builder — execute directly without XSLT
					if (config.getJavaGenerator() == JavaGeneratorType.PROFILE_SERIALIZER) {
						if (lazyProfileModel == null) {
							lazyProfileModel = new ProfileModel();
							lazyProfileModel.setOntModel(profileModel);
							lazyProfileModel.setBackgroundModel(backgroundModel);
						}
					}
					File outputFile = executeJavaBuilder(config, lazyProfileModel);
					if (outputFile != null) {
						outputs.add(outputFile);
					}
				} else {
					// XSLT-based builder
					setBuilder(builder);
					transform();
					outputs.add(getOutputFile());
				}
			} catch (Exception e) {
				failures.add(builder);
				System.err.println("  ERROR: Builder '" + builder + "' failed: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}

		if (!failures.isEmpty()) {
			System.err.println("\nFailed builder(s): " + String.join(", ", failures));
		}

		return outputs;
	}

	/**
	 * Execute a Java-based builder directly using its generator class.
	 *
	 * <p>
	 * For {@link JavaGeneratorType#OWL} and {@link JavaGeneratorType#RDFS}
	 * builders, the generator is constructed from the profile and background
	 * {@link OntModel}s and the result is written directly to the output file.
	 * </p>
	 * <p>
	 * For {@link JavaGeneratorType#COPY} builders, the profile model is written
	 * directly in the configured RDF serialization format.
	 * </p>
	 * <p>
	 * For {@link JavaGeneratorType#PROFILE_SERIALIZER} builders, the supplied
	 * {@link ProfileModel} is serialized via {@link ProfileSerializer} with the
	 * {@code indent-xml.xsl} post-processor.
	 * </p>
	 *
	 * @param config       the Java builder configuration
	 * @param profileModel the lazily-constructed ProfileModel; only non-null when
	 *                     {@code config.getJavaGenerator() == PROFILE_SERIALIZER}
	 * @return the output file that was written
	 * @throws IOException          if the output file cannot be written
	 * @throws TransformerException if ProfileSerializer fails (PROFILE_SERIALIZER
	 *                              only)
	 */
	private File executeJavaBuilder(BuilderConfig config, ProfileModel profileModel)
			throws IOException, TransformerException {

		String baseName = profileFile.getName();
		int dot = baseName.lastIndexOf('.');
		if (dot > 0)
			baseName = baseName.substring(0, dot);

		File outputFile = new File(outputDir, baseName + "." + config.getExtension());

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		boolean preserveNS = builderPreferences != null && "true".equalsIgnoreCase(builderPreferences
				.getPreference(profileFile, au.com.langdale.preferences.GlobalPreferences.PREF_PRESERVE_NAMESPACES));

		JavaGeneratorType generatorType = config.getJavaGenerator();
		String format = config.getFormat();

		switch (generatorType) {

		case OWL: {
			RDFSBasedGenerator owlGenerator = new OWLGenerator(this.profileModel, backgroundModel, preserveNS,
					config.isWithInverses());
			owlGenerator.run();
			OntModel generatedModel = owlGenerator.getResult();
			try (java.io.OutputStream out = new FileOutputStream(outputFile)) {
				IO.write(generatedModel, out, owlGenerator.getOntURI(), format, null);
			}
			break;
		}

		case RDFS: {
			RDFSBasedGenerator rdfsGenerator = new RDFSGenerator(this.profileModel, backgroundModel, preserveNS,
					config.isWithInverses());
			rdfsGenerator.run();
			OntModel generatedModel = rdfsGenerator.getResult();
			try (java.io.OutputStream out = new FileOutputStream(outputFile)) {
				IO.write(generatedModel, out, rdfsGenerator.getOntURI(), format, null);
			}
			break;
		}

		case COPY: {
			try (java.io.OutputStream out = new FileOutputStream(outputFile)) {
				IO.write(this.profileModel, out, getOntologyURI(), format, null);
			}
			break;
		}

		case PROFILE_SERIALIZER: {
			if (profileModel == null) {
				throw new IllegalStateException("ProfileModel must be provided for PROFILE_SERIALIZER builder");
			}
			ProfileSerializer serializer = new ProfileSerializer(profileModel);
			serializer.enableConsoleErrorOutput();
			serializer.setBaseURI(profileModel.getNamespace());
			serializer.setOntologyURI(getOntologyURI());
			serializer.setVersion("Beta");
			serializer.setCopyrightMultiLine(copyrightMultiLine);
			serializer.setCopyrightSingleLine(copyrightSingleLine);
			serializer.setFileName(baseName);
			if (builderPreferences != null) {
				serializer.setBuilderParameters(builderPreferences.getBuilderParameters(outputFile));
				serializer.setNamespacePrefixesBuilderParameter(
						getNamespacePrefixesBuilderParameter(profileModel.getNsPrefixMap()));
			}
			try {
				serializer.setStyleSheet((String) null);
			} catch (TransformerConfigurationException e) {
				throw new TransformerException("Failed to configure ProfileSerializer stylesheet", e);
			}
			try (java.io.OutputStream out = new FileOutputStream(outputFile)) {
				serializer.write(out);
			}
			if (serializer.hasTransformationErrors()) {
				throw new TransformerException("ProfileSerializer completed with errors for: " + outputFile.getName());
			}
			break;
		}

		default:
			throw new IllegalArgumentException("Unsupported JavaGeneratorType: " + generatorType);
		}

		return outputFile;
	}

	/**
	 * Get the list of builder names that are flagged (enabled) in the profile.
	 * 
	 * <p>
	 * A builder is flagged when the profile OWL contains:
	 * {@code <http://langdale.com.au/2007/Buildlet#extension> rdf:type <http://langdale.com.au/2005/Message#Flag>}
	 * </p>
	 * 
	 * @return list of flagged builder names (extensions)
	 */
	public List<String> getFlaggedBuilders() {
		List<String> flagged = new ArrayList<>();

		if (profileModel == null) {
			return flagged;
		}

		for (String builderName : builderRegistry.getBuilderNames()) {
			if (isBuilderFlagged(builderName)) {
				flagged.add(builderName);
			}
		}

		return flagged;
	}

	/**
	 * Check if a specific builder is flagged (enabled) in the profile.
	 * 
	 * @param builderName the builder name (extension)
	 * @return true if the builder is flagged in the profile
	 */
	public boolean isBuilderFlagged(String builderName) {
		if (profileModel == null) {
			return false;
		}

		BuilderConfig config = builderRegistry.getBuilder(builderName);
		if (config == null) {
			return false;
		}

		// The identifier is based on the file extension
		String extension = config.getExtension();
		Resource identifier = ResourceFactory.createResource(BUILDLET_NS + extension);

		return profileModel.contains(identifier, RDF.type, MESSAGE.Flag);
	}

	/**
	 * Check if any builders are flagged in the profile.
	 * 
	 * @return true if at least one builder is flagged
	 */
	public boolean hasAnyFlaggedBuilders() {
		return !getFlaggedBuilders().isEmpty();
	}

	/**
	 * Get the XSLT input stream, either from a builder or custom file.
	 */
	private InputStream getXsltInputStream() throws IOException {
		if (xsltFile != null) {
			return new FileInputStream(xsltFile);
		}

		InputStream is = builderRegistry.getBuilderXslStream(builderName);
		if (is == null) {
			throw new IOException("Could not load XSL for builder: " + builderName);
		}
		return is;
	}

	/**
	 * Get the ontology (schema) base URI.
	 */
	private String getOntologyURI() {
		if (backgroundModel != null) {
			OntResource ont = backgroundModel.getValidOntology();
			if (ont != null && ont.isURIResource()) {
				return ont.getURI();
			}
		}
		return "";
	}

	/**
	 * Build namespace prefixes string for XSLT parameter.
	 */
	public String getNamespacePrefixesBuilderParameter(Map<String, String> prefix2NSMap) {
		StringBuffer namespacePrefixesBuilderParameter = new StringBuffer("");
		Iterator<String> prefixes = prefix2NSMap.keySet().iterator();
		while (prefixes.hasNext()) {
			String prefix = prefixes.next();
			namespacePrefixesBuilderParameter.append(prefix2NSMap.get(prefix)).append("=").append(prefix)
					.append(prefixes.hasNext() ? "|" : "");
		}
		return namespacePrefixesBuilderParameter.toString();
	}

	/**
	 * Validate that all required state is set before transformation.
	 */
	private void validateState() {
		if (profileModel == null) {
			throw new IllegalStateException("Profile not set. Call setProfile() first.");
		}
		if (backgroundModel == null) {
			throw new IllegalStateException("Schema not set. Call addSchema() or setSchemas() first.");
		}
		if (builderName == null && xsltFile == null) {
			throw new IllegalStateException("No builder or XSLT set. Call setBuilder() or setXslt() first.");
		}
		if (outputDir == null) {
			throw new IllegalStateException("Output directory not set. Call setOutputDirectory() first.");
		}
		if (outputExtension == null) {
			throw new IllegalStateException(
					"Output extension not set. This should be set via setBuilder() or setXslt().");
		}
		if (builderPreferences == null) {
			throw new IllegalStateException(
					"Builder preferences not set. This should be set via setBuilderPreferences().");
		}
	}

	/**
	 * Validate state for transformFlagged() - does not require builder/xslt to be
	 * preset.
	 */
	private void validateStateForFlagged() {
		if (profileModel == null) {
			throw new IllegalStateException("Profile not set. Call setProfile() first.");
		}
		if (backgroundModel == null) {
			throw new IllegalStateException("Schema not set. Call addSchema() or setSchemas() first.");
		}
		if (outputDir == null) {
			throw new IllegalStateException("Output directory not set. Call setOutputDirectory() first.");
		}
		if (builderPreferences == null) {
			throw new IllegalStateException(
					"Builder preferences not set. This should be set via setBuilderPreferences().");
		}
	}

	/**
	 * Load an OWL file into an OntModel.
	 */
	private OntModel loadOntModel(File file) throws IOException {
		OntModel model = ModelFactory.createMem();
		String namespace = guessNamespace(file);
		String syntax = guessSyntax(file);

		try (InputStream is = new FileInputStream(file)) {
			IO.read(model, is, namespace, syntax);
		}

		return model;
	}

	/**
	 * Guess the RDF syntax based on file extension.
	 */
	private String guessSyntax(File file) {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".ttl") || name.endsWith(".turtle")) {
			return Format.TURTLE.toFormat();
		} else if (name.endsWith(".n3")) {
			return Format.N3.toFormat();
		} else {
			return Format.RDF_XML.toFormat();
		}
	}

	/**
	 * Guess a namespace from file name.
	 */
	private String guessNamespace(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		if (dot > 0) {
			name = name.substring(0, dot);
		}
		return "http://example.com/" + name + "#";
	}

	/**
	 * Get information about available builders.
	 * 
	 * @return formatted string listing all available builders
	 */
	public String listBuilders() {
		StringBuilder sb = new StringBuilder();
		sb.append("Available builders:\n");

		for (String name : builderRegistry.getBuilderNames()) {
			BuilderConfig config = builderRegistry.getBuilder(name);
			sb.append("  ").append(name);
			sb.append(" (").append(config.getType()).append(")");
			sb.append(" -> .").append(config.getExtension());
			sb.append("\n");
		}

		return sb.toString();
	}
}