package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.profiles.cl.builders.BuilderConfig;
import au.com.langdale.profiles.cl.builders.BuilderRegistry;
import au.com.langdale.profiles.cl.builders.BuilderType;

/**
 * Runs every builder registered in {@code builders.json} and
 * {@code java-builders.json} against the sample projects, offline.
 *
 * <p>
 * Each builder must complete without an exception and produce non-empty output,
 * and XSD builders must produce well-formed XML. Because the test class runs
 * with {@link NoNetwork}, a builder whose stylesheet tries to fetch anything
 * from the network fails here (issues #292, #297 and #311). This also runs
 * every builder changed in 2.3.1 against a real schema, including the
 * documentation builders' stereotype handling and the PlantUML builders.
 * Every builder is run against both sample projects, merged and unmerged.
 * </p>
 */
@NoNetwork
class AllBuildersSmokeTest {

	static Stream<String> builders() {
		return BuilderRegistry.getInstance().getBuilderNames().stream().sorted();
	}

	/**
	 * The registry holds exactly the builders named in {@code builders.json} and
	 * {@code java-builders.json}. The registry only logs a warning when it cannot
	 * read one of the files, so without this check a builder could drop out of
	 * {@link #producesOutput} unnoticed. Adding or removing a builder needs no
	 * change here.
	 */
	@Test
	void registryHoldsEveryBuilderInTheConfigurationFiles() throws IOException {
		List<String> named = new ArrayList<>();
		for (String file : new String[] { "/builders.json", "/java-builders.json" }) {
			try (InputStream in = BuilderRegistry.class.getResourceAsStream(file)) {
				assertNotNull(in, file + " is on the classpath");
				JsonObject builders = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
						.getAsJsonObject();
				named.addAll(builders.keySet());
			}
		}
		assertEquals(named.size(), new HashSet<>(named).size(),
				"no builder is named in both configuration files: " + named);
		assertEquals(new TreeSet<>(named), new TreeSet<>(BuilderRegistry.getInstance().getBuilderNames()));
	}

	/** Every registered XSLT builder's stylesheet is present beside builders.json. */
	@ParameterizedTest
	@MethodSource("builders")
	void stylesheetExists(String builder) {
		BuilderConfig config = BuilderRegistry.getInstance().getBuilder(builder);
		if (config.isJavaBuilder())
			return;
		assertNotNull(BuilderRegistry.class.getResource("/" + config.getXslFileName()),
				builder + " is registered but " + config.getXslFileName() + " is missing");
	}

	@ParameterizedTest
	@MethodSource("builders")
	void producesOutput(String builder, @TempDir Path output) throws Exception {
		run(TestProjects.sample(), builder, output);
	}

	/**
	 * Every builder also completes on the project imported with shadow and
	 * mix-in extensions left unmerged, a mistake users make that previously
	 * caused exceptions while the PlantUML real-time preview was built, and
	 * failures in the LinkML and Avro builders (issue #318).
	 */
	@ParameterizedTest
	@MethodSource("builders")
	void producesOutputWhenMergeWasLeftUnchecked(String builder, @TempDir Path output) throws Exception {
		run(TestProjects.sampleUnmerged(), builder, output);
	}

	/**
	 * With extensions left unmerged, LinkML names the normative parent in
	 * {@code is_a} and the shadow class as a mixin (issue #318).
	 */
	@Test
	void linkmlListsAShadowParentAsAMixin(@TempDir Path output) throws Exception {
		String yaml = Files.readString(run(TestProjects.sampleUnmerged(), "linkml", output).toPath());
		int start = yaml.indexOf("\n  Equipment:\n");
		assertTrue(start >= 0, "Equipment class in LinkML output");
		int end = yaml.indexOf("\n    attributes:", start);
		String equipment = yaml.substring(start, end);
		assertTrue(equipment.contains("\n    is_a: PowerSystemResource\n"), equipment);
		assertTrue(equipment.contains("\n    mixins:\n      - ExtEquipment\n"), equipment);
	}

	private static File run(CLIProfileTransformer transformer, String builder, Path output) throws Exception {
		transformer.setBuilder(builder);
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();

		File result = transformer.getOutputFile();
		assertTrue(result.isFile(), builder + " wrote " + result.getName());
		assertTrue(Files.size(result.toPath()) > 0, builder + " output is not empty");

		if (BuilderRegistry.getInstance().getBuilder(builder).getType() == BuilderType.XSD) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.newDocumentBuilder().parse(result);
		}
		return result;
	}
}
