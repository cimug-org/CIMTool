package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.IO;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.cl.builders.BuilderRegistry;

/**
 * Runs every builder against profiles that previously made builders loop or
 * hang (issue #387): {@code UnresolvedProfile.owl}, whose classes are based on
 * schema classes the schema does not declare, and {@code CyclicProfile.owl},
 * whose classes name each other as superclasses. Every builder must finish
 * within the time limit and write non-empty output.
 */
@NoNetwork
class UnresolvedAndCyclicProfileBuildersTest {

	private static final String SCHEMA = "Schema/MinimalSchema.owl";
	private static final String[] PROFILES = { "UnresolvedProfile", "CyclicProfile" };
	private static final Duration LIMIT = Duration.ofSeconds(120);

	/**
	 * Stylesheets that are not registered as builders but follow the
	 * {@code SuperType} chain, so a cyclic hierarchy made them loop.
	 */
	private static final String UNREGISTERED = "json-ld-context-json-beta.xsl";

	static Stream<Arguments> builders() {
		return BuilderRegistry.getInstance().getBuilderNames().stream().sorted()
				.flatMap(builder -> Stream.of(PROFILES).map(profile -> Arguments.of(builder, profile)));
	}

	private static CLIProfileTransformer transformer(String profile, Path project) throws Exception {
		String profilePath = "Profiles/" + profile + ".owl";
		TestProjects.copyResource("/unresolved-project/" + profilePath, project.resolve(profilePath));
		TestProjects.copyResource("/minimal-project/" + SCHEMA, project.resolve(SCHEMA));
		CLIProfileTransformer transformer = new CLIProfileTransformer(new CLIBuilderPreferences(project.toFile()));
		transformer.setProfile(project.resolve(profilePath).toFile());
		try (InputStream in = Files.newInputStream(project.resolve(SCHEMA))) {
			OntModel model = ModelFactory.createMem();
			IO.read(model, in, TestProjects.CIM100, Format.RDF_XML.toFormat());
			transformer.addSchema(model);
		}
		return transformer;
	}

	private static File run(String builder, String profile, Path project, Path output) throws Exception {
		CLIProfileTransformer transformer = transformer(profile, project);
		transformer.setBuilder(builder);
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();
		return transformer.getOutputFile();
	}

	@ParameterizedTest(name = "{0} on {1}")
	@MethodSource("builders")
	void builderFinishes(String builder, String profile, @TempDir Path project, @TempDir Path output) {
		File result = assertTimeoutPreemptively(LIMIT, () -> run(builder, profile, project, output),
				builder + " finishes on " + profile);
		assertTrue(result.isFile() && result.length() > 0, builder + " writes output for " + profile);
	}

	@ParameterizedTest
	@ValueSource(strings = { "UnresolvedProfile", "CyclicProfile" })
	void unregisteredStylesheetFinishes(String profile, @TempDir Path project, @TempDir Path output) {
		Path result = output.resolve("result.out");
		assertTimeoutPreemptively(LIMIT, () -> {
			File xml = run("xml", profile, project, output);
			Path stylesheet = TestProjects.repositoryRoot().resolve("CIMUtil/builders/" + UNREGISTERED);
			Transformer transformer = new net.sf.saxon.TransformerFactoryImpl()
					.newTransformer(new StreamSource(stylesheet.toFile()));
			transformer.transform(new StreamSource(xml), new StreamResult(result.toFile()));
		}, UNREGISTERED + " finishes on " + profile);
		assertTrue(result.toFile().length() > 0, UNREGISTERED + " writes output for " + profile);
	}
}
