package au.com.langdale.profiles.cl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;

/**
 * The minimal CIMTool project in test-resources/minimal-project: one profile,
 * MinimalProfile.owl, defined against one schema, MinimalSchema.owl. Tests copy
 * it to a temporary folder and run the CLI transformer against the copy.
 */
final class MinimalProject {

	static final String PROFILE = "MinimalProfile";

	private static final String RESOURCES = "/minimal-project/";
	private static final String PROFILE_PATH = "Profiles/" + PROFILE + ".owl";
	private static final String SCHEMA_PATH = "Schema/MinimalSchema.owl";
	private static final String SCHEMA_NS = "http://iec.ch/TC57/CIM100#";

	private MinimalProject() {
	}

	/**
	 * Copies the project into {@code project} and returns a transformer with its
	 * profile and schema loaded. The caller selects a builder or stylesheet and an
	 * output directory.
	 */
	static CLIProfileTransformer transformer(Path project) throws IOException {
		copy(PROFILE_PATH, project);
		copy(SCHEMA_PATH, project);
		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve(PROFILE_PATH).toFile());
		transformer.addSchema(readSchema(project.resolve(SCHEMA_PATH)));
		return transformer;
	}

	/**
	 * Copies a test resource to {@code target}, creating its folder.
	 */
	static void copyResource(String resource, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		try (InputStream in = MinimalProject.class.getResourceAsStream(resource)) {
			if (in == null)
				throw new IOException("Test resource not found: " + resource);
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void copy(String path, Path project) throws IOException {
		copyResource(RESOURCES + path, project.resolve(path));
	}

	private static OntModel readSchema(Path schema) throws IOException {
		OntModel model = ModelFactory.createMem();
		try (InputStream in = Files.newInputStream(schema)) {
			IO.read(model, in, SCHEMA_NS, Format.RDF_XML.toFormat());
		}
		return model;
	}
}
