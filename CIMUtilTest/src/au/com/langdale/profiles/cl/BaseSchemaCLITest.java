package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;

/**
 * Runs {@code cimtool-cli} on a project with two schema files, a base CIM
 * schema and a European extension, to check how the base CIM schema is chosen
 * (issue #391). With a base, the profile XML's {@code ontologyURI} is the base
 * CIM ontology. Without one, nothing is generated, and the CLI says why and how
 * to fix it, and exits with {@link CIMToolCLI#EXIT_TRANSFORM_ERROR}.
 *
 * <pre>
 * &lt;temporary folder&gt;
 * ├── .builder-preferences
 * ├── .cimtool-settings                 ← written by each test
 * ├── Profiles
 * │   └── MultiSchemaProfile.owl
 * └── Schema
 *     ├── Base.xmi
 *     └── European.xmi
 * </pre>
 */
@NoNetwork
class BaseSchemaCLITest {

	private static final String RESOURCES = "/multi-schema-project/";
	private static final String CIM = "http://iec.ch/TC57/CIM100#";
	private static final String EUROPEAN = "http://iec.ch/TC57/CIM100-European#";

	@TempDir
	Path project;

	@TempDir
	Path output;

	private String errors;

	@BeforeEach
	void copyProject() throws IOException {
		for (String file : new String[] { "Schema/Base.xmi", "Schema/European.xmi", "Profiles/MultiSchemaProfile.owl" })
			TestProjects.copyResource(RESOURCES + file, project.resolve(file));
		Files.writeString(project.resolve(".builder-preferences"),
				"@prefix project: <http://cimtoole.langdale.com.au/2009/project/> .\n", StandardCharsets.UTF_8);
	}

	/** Writes .cimtool-settings with each schema file's namespace and whether it is flagged as the base. */
	private void settings(String baseNamespace, boolean baseFlagged, String europeanNamespace, boolean europeanFlagged)
			throws IOException {
		StringBuilder text = new StringBuilder();
		text.append("@prefix tool: <http://cimtoole.langdale.com.au/2009/setting#> .\n\n");
		text.append(schema("Base.xmi", baseNamespace, baseFlagged));
		text.append(schema("European.xmi", europeanNamespace, europeanFlagged));
		Files.writeString(project.resolve(".cimtool-settings"), text.toString(), StandardCharsets.UTF_8);
	}

	private static String schema(String file, String namespace, boolean flagged) {
		return "<http://cimtoole.langdale.com.au/2009/project/Schema/" + file + ">\n"
				+ "      tool:schema_namespace  \"" + namespace + "\""
				+ (flagged ? " ;\n      tool:canonical_schema  \"true\"" : "") + " .\n\n";
	}

	/** Runs the xml builder in project mode, keeping what the CLI writes to standard error. */
	private int run() {
		PrintStream original = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
		try {
			return CIMToolCLI.run(new String[] { "--project-dir", project.toString(), "--output", output.toString(),
					"--builder", "xml" });
		} finally {
			System.setErr(original);
			errors = captured.toString(StandardCharsets.UTF_8);
		}
	}

	private String profileXml() throws IOException {
		try (Stream<Path> files = Files.list(output)) {
			Path[] written = files.toArray(Path[]::new);
			assertEquals(1, written.length, "one output file");
			return Files.readString(written[0], StandardCharsets.UTF_8);
		}
	}

	private long outputCount() throws IOException {
		try (Stream<Path> files = Files.list(output)) {
			return files.count();
		}
	}

	@Test
	void theFlaggedSchemaIsTheBase() throws IOException {
		settings(CIM, true, EUROPEAN, false);
		assertEquals(CIMToolCLI.EXIT_SUCCESS, run(), errors);
		assertTrue(profileXml().contains("ontologyURI=\"http://iec.ch/TC57/CIM100\""),
				"the profile XML's ontologyURI is the base CIM ontology");
	}

	@Test
	void withoutAFlagTheSchemaNamespacePreferenceChoosesTheBase() throws IOException {
		settings(CIM, false, EUROPEAN, false);
		assertEquals(CIMToolCLI.EXIT_SUCCESS, run(), errors);
		assertTrue(profileXml().contains("ontologyURI=\"http://iec.ch/TC57/CIM100\""),
				"the schema with the default Schema Namespace is the base");
	}

	@Test
	void withoutABaseNothingIsGenerated() throws IOException {
		settings("http://example.com/base#", false, EUROPEAN, false);
		assertEquals(CIMToolCLI.EXIT_TRANSFORM_ERROR, run());
		assertEquals(0, outputCount(), "no output is written");
		assertTrue(errors.contains("Error: No base CIM schema is identified"), errors);
		assertTrue(errors.contains("To fix:") && errors.contains("tool:canonical_schema"), errors);
		assertTrue(errors.contains("No output was generated."), errors);
	}

	@Test
	void twoFlaggedSchemasStopTheRun() throws IOException {
		settings(CIM, true, EUROPEAN, true);
		assertEquals(CIMToolCLI.EXIT_TRANSFORM_ERROR, run());
		assertEquals(0, outputCount(), "no output is written");
		assertTrue(errors.contains("Base.xmi and European.xmi are all marked as the base CIM schema"), errors);
	}
}
