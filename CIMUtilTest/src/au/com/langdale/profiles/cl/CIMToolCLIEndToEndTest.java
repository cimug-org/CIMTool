package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hp.hpl.jena.graph.Node;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.kena.OntModel;

/**
 * Runs {@code cimtool-cli} as a user would, through {@link CIMToolCLI#run}, on
 * a copy of the sample project to which project copyright templates have been
 * added (issue #287).
 *
 * <pre>
 * &lt;temporary folder&gt;
 * ├── .builder-preferences
 * ├── .cimtool-settings
 * ├── .copyright-multi-line
 * ├── .copyright-single-line
 * ├── Profiles
 * │   └── SampleProfile.owl
 * └── Schema
 *     ├── CIM17-2_CGMES31v01_PROF-20v02_NC23v66_MS10v01_DES10v01.qea
 *     ├── CIM17-2_CGMES31v01_PROF-20v02_NC23v66_MS10v01_DES10v01.namespaces
 *     └── CIM17-2_CGMES31v01_PROF-20v02_NC23v66_MS10v01_DES10v01.namespace-prefixes
 * </pre>
 *
 * <p>
 * Each CLI run parses the CGMES schema, which takes a few seconds.
 * </p>
 */
@NoNetwork
class CIMToolCLIEndToEndTest {

	private static final String MULTI_LINE = "Copyright ${year} Example Utility\n\nFor internal use only.\n";
	private static final String SINGLE_LINE = "Copyright ${year} Example Utility. For internal use only.\n";

	@TempDir
	static Path project;

	@TempDir
	Path output;

	@BeforeAll
	static void copySampleProject() throws IOException {
		Path sample = TestProjects.sampleProject();
		try (Stream<Path> paths = Files.walk(sample)) {
			for (Path source : (Iterable<Path>) paths::iterator) {
				Path target = project.resolve(sample.relativize(source).toString());
				if (Files.isDirectory(source))
					Files.createDirectories(target);
				else
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		Files.writeString(project.resolve(".builder-preferences"),
				"@prefix project: <http://cimtoole.langdale.com.au/2009/project/> .\n", StandardCharsets.UTF_8);
		Files.writeString(project.resolve(".copyright-multi-line"), MULTI_LINE, StandardCharsets.UTF_8);
		Files.writeString(project.resolve(".copyright-single-line"), SINGLE_LINE, StandardCharsets.UTF_8);
	}

	@Test
	void htmlUsesTheProjectSingleLineCopyright() throws IOException {
		String html = run("--builder", "html");
		assertTrue(html.contains(">Copyright " + Year.now() + " Example Utility. For internal use only.</div>"),
				"html footer carries the project's single-line copyright");
	}

	@Test
	void sqlUsesTheProjectMultiLineCopyright() throws IOException {
		String sql = run("--builder", "sql-rdfs-ansi92");
		assertTrue(sql.contains("\n--\n-- Copyright " + Year.now() + " Example Utility\n--\n-- For internal use only.\n--\n"),
				"sql header carries the project's multi-line copyright");
	}

	/**
	 * Before issue #287, {@code --xslt} never set a builder type, so a
	 * {@code TEXT} stylesheet skipped the indentation stage and the output held
	 * the raw {@code <item>} markup.
	 */
	@Test
	void xsltWithTypeTextProducesPlainText() throws IOException {
		Path stylesheet = TestProjects.repositoryRoot().resolve("CIMUtil/builders/sql-rdfs-ansi92.xsl");
		String sql = run("--xslt", stylesheet.toString(), "--type", "TEXT");
		assertTrue(sql.contains("CREATE TABLE"), "sql output holds table definitions");
		assertFalse(sql.contains("<item"), "sql output holds no <item> markup");
		assertFalse(sql.contains("<list"), "sql output holds no <list> markup");
	}

	/**
	 * The {@code .namespaces} file beside the schema maps the «European»
	 * stereotype to {@code http://iec.ch/TC57/CIM100-European#}, and the CLI's
	 * schema parser places {@code IdentifiedObject.energyIdentCodeEic} in that
	 * namespace. The CGMES model's own namespace tags give the same result, so the
	 * test also remaps «European» in a copy of the file and checks that the
	 * attribute follows the file.
	 */
	@Test
	void theNamespacesFileBesideTheSchemaIsApplied(@TempDir Path remapped) throws Exception {
		String attribute = "IdentifiedObject.energyIdentCodeEic";
		assertTrue(defines(TestProjects.sampleSchema(), TestProjects.CIM100_EUROPEAN + attribute),
				attribute + " is in the European namespace");

		String testNamespace = "http://example.com/CIMUtilTest/European#";
		Files.createDirectories(remapped.resolve("Schema"));
		Files.copy(project.resolve(".cimtool-settings"), remapped.resolve(".cimtool-settings"));
		String schema = "Schema/" + TestProjects.CGMES_SCHEMA;
		Files.copy(project.resolve(schema + ".qea"), remapped.resolve(schema + ".qea"));
		String namespaces = Files.readString(project.resolve(schema + ".namespaces"), StandardCharsets.UTF_8);
		assertTrue(namespaces.contains("European=" + TestProjects.CIM100_EUROPEAN), "the file maps «European»");
		Files.writeString(remapped.resolve(schema + ".namespaces"),
				namespaces.replace("European=" + TestProjects.CIM100_EUROPEAN, "European=" + testNamespace),
				StandardCharsets.UTF_8);

		CLISettings settings = new CLISettings(remapped.toFile());
		OntModel model = new CLISchemaParser(settings, remapped.toFile()).parse(settings.getSchemaFiles().get(0));
		assertTrue(defines(model, testNamespace + attribute), attribute + " follows the remapped namespace");
		assertFalse(defines(model, TestProjects.CIM100_EUROPEAN + attribute), attribute + " leaves the original namespace");
	}

	private static boolean defines(OntModel model, String uri) {
		return model.getGraph().contains(Node.createURI(uri), Node.ANY, Node.ANY);
	}

	/** Runs the CLI in project mode with the given options and returns the single output file's text. */
	private String run(String... options) throws IOException {
		String[] args = Stream.concat(Stream.of("--project-dir", project.toString(), "--output", output.toString()),
				Stream.of(options)).toArray(String[]::new);
		assertEquals(CIMToolCLI.EXIT_SUCCESS, CIMToolCLI.run(args), "cimtool-cli exit code");
		try (Stream<Path> files = Files.list(output)) {
			Path[] written = files.toArray(Path[]::new);
			assertEquals(1, written.length, "one output file");
			return Files.readString(written[0], StandardCharsets.UTF_8).replace("\r\n", "\n");
		}
	}
}
