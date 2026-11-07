package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.cl.builders.BuilderRegistry;
import au.com.langdale.profiles.cl.builders.BuilderType;

/**
 * Verifies that every AsciiDoc builder registered in builders.json references
 * its PlantUML diagram through {profilesdir} and defines the {docdir}/Profiles
 * fallback (issue #291).
 *
 * <p>
 * Each builder is run headlessly through {@link CLIProfileTransformer} against
 * the minimal project in test-resources. The builders are discovered from
 * builders.json, so a newly registered AsciiDoc builder is tested
 * automatically.
 * </p>
 */
class AsciiDocBuildersProfilesDirTest {

	private static final String PROFILE = "MinimalProfile";

	private static final String FALLBACK = "ifndef::profilesdir[:profilesdir: {docdir}/Profiles]";

	private static final Pattern PLANTUML = Pattern
			.compile("plantuml::\\{profilesdir\\}/" + PROFILE + "\\.[a-z0-9-]+\\.puml\\[format=svg, align=center\\]");

	static Stream<String> asciiDocBuilders() {
		BuilderRegistry registry = BuilderRegistry.getInstance();
		return registry.getBuilderNames().stream()
				.filter(name -> registry.getBuilder(name).getType() == BuilderType.ASCIIDOC).sorted();
	}

	@Test
	void asciiDocBuildersAreRegistered() {
		assertEquals(8, asciiDocBuilders().count(),
				"number of AsciiDoc builders in builders.json; update this test when one is added or removed");
	}

	@ParameterizedTest
	@MethodSource("asciiDocBuilders")
	void referencesDiagramThroughProfilesDir(String builder, @TempDir Path temp) throws Exception {
		List<String> lines = generate(builder, temp);

		List<String> plantuml = lines.stream().filter(line -> line.contains("plantuml::"))
				.collect(Collectors.toList());
		assertEquals(1, plantuml.size(), "one plantuml:: macro");
		assertTrue(PLANTUML.matcher(plantuml.get(0)).matches(),
				"plantuml:: macro at the start of its line, through {profilesdir}: " + plantuml.get(0));

		int fallback = lines.indexOf(FALLBACK);
		assertTrue(fallback >= 0, "profilesdir fallback at the start of its line");
		assertTrue(fallback < lines.indexOf(plantuml.get(0)), "fallback defined before the plantuml:: macro");

		assertFalse(lines.stream().anyMatch(line -> line.contains("./Profiles/")),
				"no references relative to the base directory remain");
	}

	private static List<String> generate(String builder, Path temp) throws Exception {
		Path project = temp.resolve("project");
		copyFixture("Profiles/" + PROFILE + ".owl", project);
		copyFixture("Schema/MinimalSchema.owl", project);

		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve("Profiles/" + PROFILE + ".owl").toFile());
		transformer.addSchema(readSchema(project.resolve("Schema/MinimalSchema.owl")));
		transformer.setBuilder(builder);
		transformer.setOutputDirectory(temp.resolve("output").toFile());
		transformer.transform();

		return Files.readAllLines(transformer.getOutputFile().toPath());
	}

	private static OntModel readSchema(Path schema) throws IOException {
		OntModel model = ModelFactory.createMem();
		try (InputStream in = Files.newInputStream(schema)) {
			IO.read(model, in, "http://iec.ch/TC57/CIM100#", Format.RDF_XML.toFormat());
		}
		return model;
	}

	private static void copyFixture(String path, Path project) throws IOException {
		Path target = project.resolve(path);
		Files.createDirectories(target.getParent());
		try (InputStream in = AsciiDocBuildersProfilesDirTest.class.getResourceAsStream("/minimal-project/" + path)) {
			if (in == null)
				throw new IOException("Test resource not found: /minimal-project/" + path);
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
