package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.profiles.cl.builders.BuilderRegistry;
import au.com.langdale.profiles.cl.builders.BuilderType;

/**
 * Every {@code ASCIIDOC} builder keeps the leading whitespace of each line of a
 * source listing in a class's AsciiDoc documentation (issue #288).
 *
 * <p>
 * The sample profile documents {@code IdentifiedObject} with the indented
 * Python listing reported in issue #288. Before the fix, the builders wrote it
 * with the indentation of every line removed.
 * </p>
 */
@NoNetwork
class AsciiDocWhitespaceTest {

	/** The listing in the sample profile's documentation of IdentifiedObject. */
	private static final List<String> LISTING = List.of( //
			"[source,python]", //
			"----", //
			"If PowerTransformer.EquipmentContainer-> EquipmentContainer.@type:", //
			"    xfmerType = DCConverterUnit # see DCConverterUnit", //
			"else:", //
			"    count = PowerTransformer.count(PowerTransformerEnd)", //
			"    if count == 2:", //
			"        xfmerType = TwoWinding", //
			"    elif count == 3:", //
			"        xfmerType = ThreeWinding", //
			"    else:", //
			"        # Log error", //
			"----");

	static Stream<String> asciiDocBuilders() {
		BuilderRegistry registry = BuilderRegistry.getInstance();
		return registry.getBuilderNames().stream().filter(name -> registry.getBuilder(name).getType() == BuilderType.ASCIIDOC)
				.sorted();
	}

	@Test
	void asciiDocBuildersAreRegistered() {
		assertFalse(asciiDocBuilders().findAny().isEmpty(), "builders.json registers ASCIIDOC builders");
	}

	@ParameterizedTest
	@MethodSource("asciiDocBuilders")
	void keepsTheIndentationOfASourceListing(String builder, @TempDir Path output) throws Exception {
		CLIProfileTransformer transformer = TestProjects.sample();
		transformer.setBuilder(builder);
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();
		List<String> lines = List.of(Files.readString(transformer.getOutputFile().toPath(), StandardCharsets.UTF_8)
				.split("\\r?\\n", -1));

		int start = Collections.indexOfSubList(lines.stream().map(String::strip).toList(),
				LISTING.stream().map(String::strip).toList());
		assertTrue(start >= 0, builder + " writes the listing from IdentifiedObject's documentation");
		assertEquals(LISTING, lines.subList(start, start + LISTING.size()), builder + " keeps the listing's indentation");
	}
}
