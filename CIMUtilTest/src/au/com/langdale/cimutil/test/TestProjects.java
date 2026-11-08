package au.com.langdale.cimutil.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.cl.CLIBuilderPreferences;
import au.com.langdale.profiles.cl.CLIProfileTransformer;
import au.com.langdale.profiles.cl.CLISchemaParser;
import au.com.langdale.profiles.cl.CLISettings;

/**
 * The CIMTool projects the CIMUtilTest tests run against.
 *
 * <ul>
 * <li><b>Minimal project</b>: one profile class and one attribute, defined
 * against a hand-written schema of the same size, in
 * {@code test-resources/minimal-project}. Quick to load, for tests that only
 * need a builder to run.</li>
 * <li><b>Sample project</b>: the profile in
 * {@code test-resources/sample-project}, defined against the CGMES CIM17
 * schema. The schema and its {@code .namespaces} file are taken from
 * {@code CIMToolTest/CIMToolTestFiles/CIMToolTestFiles.zip}, so no second copy
 * is kept in the repository. The project is set up as the New Project wizard
 * sets up a project whose schema is an EA project file: shadow and mix-in
 * extensions merged, self-healing enabled, and the CIM 17 (CIM100)
 * namespace.</li>
 * <li><b>Unmerged sample project</b>: the same project, with the profile in
 * {@code test-resources/sample-project-unmerged}, as it is when a user leaves
 * the wizard's option to merge shadow and mix-in extensions unchecked. A class
 * then keeps a shadow class as a second parent. That setup is a mistake for an
 * EA project schema, but builders must not fail on it.</li>
 * </ul>
 *
 * <p>
 * Each sample project is assembled and its schema parsed once per test run,
 * since parsing the CGMES schema takes several seconds. Tests must treat both
 * the project folder and the schema model as read-only and write their output
 * elsewhere.
 * </p>
 */
public final class TestProjects {

	/** The profile name of both projects' profile files, without extension. */
	public static final String MINIMAL_PROFILE = "MinimalProfile";
	public static final String SAMPLE_PROFILE = "SampleProfile";

	/** The CGMES CIM17 schema in CIMToolTestFiles.zip. */
	public static final String CGMES_SCHEMA = "CIM17-2_CGMES31v01_PROF-20v02_NC23v66_MS10v01_DES10v01";

	public static final String CIM100 = "http://iec.ch/TC57/CIM100#";
	public static final String CIM100_EUROPEAN = "http://iec.ch/TC57/CIM100-European#";
	public static final String SAMPLE_PROFILE_NAMESPACE = "http://example.com/CIMUtilTest/SampleProfile#";

	private static final String TEST_FILES_ZIP = "CIMToolTest/CIMToolTestFiles/CIMToolTestFiles.zip";

	private static final SampleProject MERGED = new SampleProject("sample-project", true);
	private static final SampleProject UNMERGED = new SampleProject("sample-project-unmerged", false);

	private TestProjects() {
	}

	/**
	 * Copies the minimal project into {@code project} and returns a transformer
	 * with its profile and schema loaded. The caller selects a builder or
	 * stylesheet and an output directory.
	 */
	public static CLIProfileTransformer minimal(Path project) throws IOException {
		String profile = "Profiles/" + MINIMAL_PROFILE + ".owl";
		String schema = "Schema/MinimalSchema.owl";
		copyResource("/minimal-project/" + profile, project.resolve(profile));
		copyResource("/minimal-project/" + schema, project.resolve(schema));
		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve(profile).toFile());
		try (InputStream in = Files.newInputStream(project.resolve(schema))) {
			OntModel model = ModelFactory.createMem();
			IO.read(model, in, CIM100, Format.RDF_XML.toFormat());
			transformer.addSchema(model);
		}
		return transformer;
	}

	/**
	 * Returns a transformer with the sample profile and the parsed CGMES schema
	 * loaded. The caller selects a builder or stylesheet and an output directory.
	 */
	public static CLIProfileTransformer sample() throws Exception {
		return MERGED.transformer();
	}

	/**
	 * As {@link #sample()}, for the project imported with shadow and mix-in
	 * extensions left unmerged.
	 */
	public static CLIProfileTransformer sampleUnmerged() throws Exception {
		return UNMERGED.transformer();
	}

	/** The sample project folder, assembled on first use. */
	public static Path sampleProject() throws IOException {
		return MERGED.folder();
	}

	/** The unmerged sample project folder, assembled on first use. */
	public static Path sampleUnmergedProject() throws IOException {
		return UNMERGED.folder();
	}

	/** The CGMES schema of the sample project, parsed once as the CLI parses it. */
	public static OntModel sampleSchema() throws Exception {
		return MERGED.schema();
	}

	/** The CGMES schema of the unmerged sample project, parsed once. */
	public static OntModel sampleUnmergedSchema() throws Exception {
		return UNMERGED.schema();
	}

	/**
	 * The {@code .cimtool-settings} that the New Project wizard writes for an EA
	 * project schema with its defaults (merge shadow/mix-in extensions and
	 * self-healing both checked, CIM 17 namespace), and that the New Profile
	 * wizard adds for the sample profile. With {@code merge} false, the merge
	 * option is left unchecked.
	 */
	static String wizardSettings(boolean merge) {
		return "@prefix project: <http://cimtoole.langdale.com.au/2009/project/> .\n"
				+ "@prefix tool: <http://cimtoole.langdale.com.au/2009/setting#> .\n\n"
				+ "project: tool:merge_shadow_extensions \"" + merge + "\" ;\n"
				+ "        tool:self_heal_on_import \"true\" .\n\n"
				+ "<http://cimtoole.langdale.com.au/2009/project/Schema/" + CGMES_SCHEMA + ".qea>\n"
				+ "        tool:schema_namespace \"" + CIM100 + "\" .\n\n"
				+ "<http://cimtoole.langdale.com.au/2009/project/Profiles/" + SAMPLE_PROFILE + ".owl>\n"
				+ "        tool:profile_envelope \"Profile\" ;\n"
				+ "        tool:profile_namespace \"" + SAMPLE_PROFILE_NAMESPACE + "\" .\n";
	}

	/**
	 * A sample project: the profile in {@code test-resources/<resources>}, the
	 * CGMES schema and its {@code .namespaces} file, and a
	 * {@code .cimtool-settings} as written by the New Project and New Profile
	 * wizards.
	 */
	private static final class SampleProject {
		private final String resources;
		private final boolean merge;
		private Path folder;
		private OntModel schema;

		SampleProject(String resources, boolean merge) {
			this.resources = resources;
			this.merge = merge;
		}

		CLIProfileTransformer transformer() throws Exception {
			Path project = folder();
			CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
			transformer.setProfile(project.resolve("Profiles/" + SAMPLE_PROFILE + ".owl").toFile());
			transformer.addSchema(schema());
			return transformer;
		}

		synchronized Path folder() throws IOException {
			if (folder != null)
				return folder;
			Path zip = repositoryRoot().resolve(TEST_FILES_ZIP);
			Path project = Files.createTempDirectory("cimutiltest-" + resources);
			Path schemaFolder = Files.createDirectories(project.resolve("Schema"));
			try (ZipFile archive = new ZipFile(zip.toFile())) {
				extract(archive, CGMES_SCHEMA + ".qea", schemaFolder);
				extract(archive, CGMES_SCHEMA + ".namespaces", schemaFolder);
			}
			copyResource("/" + resources + "/Profiles/" + SAMPLE_PROFILE + ".owl",
					project.resolve("Profiles/" + SAMPLE_PROFILE + ".owl"));
			Files.writeString(project.resolve(".cimtool-settings"), wizardSettings(merge), StandardCharsets.UTF_8);
			deleteOnExit(project);
			folder = project;
			return project;
		}

		synchronized OntModel schema() throws Exception {
			if (schema != null)
				return schema;
			File project = folder().toFile();
			CLISettings settings = new CLISettings(project);
			schema = new CLISchemaParser(settings, project).parse(settings.getSchemaFiles().get(0));
			return schema;
		}
	}

	/**
	 * Finds the repository root by walking up from the working folder to the
	 * folder containing {@code CIMToolTest/CIMToolTestFiles/CIMToolTestFiles.zip}.
	 * Tests needing it are skipped when run outside a repository checkout.
	 */
	public static Path repositoryRoot() {
		Path folder = Paths.get("").toAbsolutePath();
		while (folder != null && !Files.isRegularFile(folder.resolve(TEST_FILES_ZIP)))
			folder = folder.getParent();
		assumeTrue(folder != null, "not run from within a CIMTool repository checkout containing " + TEST_FILES_ZIP);
		return folder;
	}

	/** Copies a test resource to {@code target}, creating its folder. */
	public static void copyResource(String resource, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		try (InputStream in = TestProjects.class.getResourceAsStream(resource)) {
			if (in == null)
				throw new IOException("Test resource not found: " + resource);
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void extract(ZipFile archive, String name, Path folder) throws IOException {
		ZipEntry entry = archive.stream()
				.filter(e -> !e.isDirectory() && (e.getName().equals(name) || e.getName().endsWith("/" + name))).findFirst()
				.orElseThrow(() -> new IOException(name + " not found in " + archive.getName()));
		try (InputStream in = archive.getInputStream(entry)) {
			Files.copy(in, folder.resolve(name), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void deleteOnExit(Path folder) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try (var paths = Files.walk(folder)) {
				paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			} catch (IOException e) {
				// Best effort: the folder is in the system temporary directory.
			}
		}));
	}
}
