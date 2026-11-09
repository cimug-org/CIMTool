package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import au.com.langdale.profiles.cl.builders.BuilderType;

/**
 * The option rules that {@link CLIOptions#validate()} enforces for
 * {@code cimtool-cli} (issue #287), one case per rule.
 *
 * <p>
 * Each case runs against a project folder holding {@code .cimtool-settings},
 * {@code .builder-preferences}, a profile and three stylesheets: one named after
 * a registered XSLT builder ({@code sql.xsl}), one named after a JAVA builder
 * ({@code ttl.xsl}) and one matching no builder ({@code custom.xsl}). In the
 * arguments, {@code @} stands for the project folder.
 * </p>
 */
class CLIOptionsTest {

	private static final String VALID_TYPES = "Valid types are: TEXT, ASCIIDOC, XSD, TRANSFORM";

	@TempDir
	Path project;

	@BeforeEach
	void createProject() throws IOException {
		for (String file : new String[] { ".cimtool-settings", ".builder-preferences", "Profiles/Sample.owl",
				"sql.xsl", "ttl.xsl", "custom.xsl", "multi.txt", "single.txt" }) {
			Files.createDirectories(project.resolve(file).getParent());
			Files.writeString(project.resolve(file), "");
		}
	}

	static Stream<Arguments> invalidOptions() {
		return Stream.of( //
				arguments("--project-dir missing", "--builder sql", //
						"Missing required option: --project-dir"),
				arguments("--builder with --xslt", "-pd @ --builder sql --xslt @/sql.xsl", //
						"Cannot specify both --builder and --xslt"),
				arguments("--type without --xslt", "-pd @ --builder sql --type TEXT", //
						"--type is only valid when --xslt is specified"),
				arguments("--output-ext without --xslt", "-pd @ --builder sql --output-ext sql", //
						"--output-ext is only valid when --xslt is specified"),
				arguments("--type JAVA", "-pd @ --xslt @/custom.xsl --type JAVA --output-ext txt", //
						"--type JAVA is not valid: JAVA builders are not XSLT-based. " + VALID_TYPES),
				arguments("invalid --type", "-pd @ --xslt @/custom.xsl --type HTML --output-ext txt", //
						"Invalid --type value: HTML. " + VALID_TYPES),
				arguments("unregistered stylesheet without --type or --output-ext", "-pd @ --xslt @/custom.xsl", //
						"The stylesheet 'custom.xsl' does not match any registered builder. "
								+ "Both --type and --output-ext are required. " + VALID_TYPES + "."),
				arguments("unregistered stylesheet without --type", "-pd @ --xslt @/custom.xsl --output-ext txt", //
						"The stylesheet 'custom.xsl' does not match any registered builder. --type is required. "
								+ VALID_TYPES + "."),
				arguments("unregistered stylesheet without --output-ext", "-pd @ --xslt @/custom.xsl --type TEXT", //
						"The stylesheet 'custom.xsl' does not match any registered builder. --output-ext is required."),
				arguments("stylesheet named after a JAVA builder", "-pd @ --xslt @/ttl.xsl", //
						"The stylesheet 'ttl.xsl' matches the name of the JAVA builder 'ttl'. JAVA builders are not "
								+ "XSLT-based, so a type and extension cannot be inherited from it. Both --type and "
								+ "--output-ext are required. " + VALID_TYPES + ". (To run the JAVA builder itself, use "
								+ "--builder ttl instead.)"),
				arguments("explicit copyright files with --copyright-defaults",
						"-pd @ --builder sql -cm @/multi.txt -cs @/single.txt --copyright-defaults", //
						"At most one copyright selection may be specified: the --copyright-multi-line and "
								+ "--copyright-single-line pair, --copyright-defaults, or --copyright-none"),
				arguments("explicit copyright files with --copyright-none",
						"-pd @ --builder sql -cm @/multi.txt -cs @/single.txt --copyright-none", //
						"At most one copyright selection may be specified: the --copyright-multi-line and "
								+ "--copyright-single-line pair, --copyright-defaults, or --copyright-none"),
				arguments("--copyright-defaults with --copyright-none",
						"-pd @ --builder sql --copyright-defaults --copyright-none", //
						"At most one copyright selection may be specified: the --copyright-multi-line and "
								+ "--copyright-single-line pair, --copyright-defaults, or --copyright-none"),
				arguments("only --copyright-single-line", "-pd @ --builder sql -cs @/single.txt", //
						"Copyright templates must be specified as a pair: --copyright-multi-line is missing"),
				arguments("only --copyright-multi-line", "-pd @ --builder sql -cm @/multi.txt", //
						"Copyright templates must be specified as a pair: --copyright-single-line is missing"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidOptions")
	void rejectsInvalidOptions(String rule, String args, String message) {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> validate(args));
		assertEquals(message, e.getMessage());
	}

	@ParameterizedTest(name = "project provides only {0}")
	@MethodSource("projectCopyrightFiles")
	void rejectsAHalfPairOfProjectCopyrightTemplates(String file) throws IOException {
		Files.writeString(project.resolve(file), "");
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> validate("-pd @ --builder sql"));
		assertEquals("The project provides only " + file + ". Copyright templates are used as a pair, so both files "
				+ "are required. Add the missing file, or specify --copyright-defaults or --copyright-none.",
				e.getMessage());
	}

	static Stream<String> projectCopyrightFiles() {
		return Stream.of(".copyright-multi-line", ".copyright-single-line");
	}

	@Test
	void registeredStylesheetInheritsTypeAndExtension() {
		CLIOptions options = validate("-pd @ --xslt @/sql.xsl");
		assertEquals(BuilderType.TEXT, options.getXsltType());
		assertEquals("sql", options.getOutputExtension());
	}

	@Test
	void explicitTypeAndExtensionOverrideARegisteredStylesheet() {
		CLIOptions options = validate("-pd @ --xslt @/sql.xsl --type XSD --output-ext ddl");
		assertEquals(BuilderType.XSD, options.getXsltType());
		assertEquals("ddl", options.getOutputExtension());
	}

	@Test
	void unregisteredStylesheetWithTypeAndExtension() {
		CLIOptions options = validate("-pd @ --xslt @/custom.xsl --type TEXT --output-ext txt");
		assertEquals(BuilderType.TEXT, options.getXsltType());
		assertEquals("txt", options.getOutputExtension());
	}

	@Test
	void outputDefaultsToTheProfilesFolderAndProjectModeWithoutAProfile() {
		CLIOptions options = validate("-pd @ --builder sql");
		assertEquals(project.resolve("Profiles").toFile(), options.getOutputDir());
		assertTrue(options.isProjectMode());
	}

	@Test
	void noCopyrightSelectionAndNoProjectTemplates() {
		CLIOptions options = validate("-pd @ --builder sql");
		assertFalse(options.isCopyrightFromProject());
		assertNull(options.getCopyrightMultiLineFile());
		assertNull(options.getCopyrightSingleLineFile());
	}

	@Test
	void projectCopyrightTemplatesAreUsedWhenNoSelectionIsMade() throws IOException {
		Files.writeString(project.resolve(".copyright-multi-line"), "");
		Files.writeString(project.resolve(".copyright-single-line"), "");
		CLIOptions options = validate("-pd @ --builder sql");
		assertTrue(options.isCopyrightFromProject());
		assertEquals(project.resolve(".copyright-multi-line").toFile(), options.getCopyrightMultiLineFile());
		assertEquals(project.resolve(".copyright-single-line").toFile(), options.getCopyrightSingleLineFile());
	}

	@Test
	void anExplicitSelectionTakesPrecedenceOverProjectTemplates() throws IOException {
		Files.writeString(project.resolve(".copyright-multi-line"), "");
		Files.writeString(project.resolve(".copyright-single-line"), "");
		assertFalse(validate("-pd @ --builder sql --copyright-none").isCopyrightFromProject());
		assertFalse(validate("-pd @ --builder sql --copyright-defaults").isCopyrightFromProject());
		CLIOptions explicit = validate("-pd @ --builder sql -cm @/multi.txt -cs @/single.txt");
		assertFalse(explicit.isCopyrightFromProject());
		assertEquals(project.resolve("multi.txt").toFile(), explicit.getCopyrightMultiLineFile());
	}

	@Test
	void copyrightSelectionsAreRecorded() {
		assertTrue(validate("-pd @ --builder sql --copyright-none").isCopyrightNoneRequested());
		assertTrue(validate("-pd @ --builder sql --copyright-defaults").isCopyrightDefaultsRequested());
	}

	private CLIOptions validate(String args) {
		// Split before substituting, since the project path may contain spaces.
		String[] parts = Stream.of(args.split(" ")).map(arg -> arg.replace("@", project.toString())).toArray(String[]::new);
		CLIOptions options = CLIOptions.parse(parts);
		options.validate();
		return options;
	}
}
