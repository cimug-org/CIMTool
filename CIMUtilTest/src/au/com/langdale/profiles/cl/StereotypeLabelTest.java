package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * The documentation builders label an attribute or association row with the
 * stereotypes a user would want to see, such as «European» or «NC», and with
 * none of the stereotypes CIMTool uses for the structure of the profile, such
 * as «attribute», «byreference», «enumeration» or «ofAggregate».
 *
 * <p>
 * The builders covered are every registered {@code ASCIIDOC} builder and
 * {@code profile-doc-rtf}. In the sample profile:
 * </p>
 * <ul>
 * <li>{@code IdentifiedObject.energyIdentCodeEic} carries «attribute» and
 * «European», and is labelled {@code (European)}.</li>
 * <li>{@code Equipment.AggregatedEquipment} carries «byreference» and «NC», and
 * is labelled {@code (NC)}.</li>
 * <li>{@code mRID} and {@code aggregate} («attribute»),
 * {@code Equipment.EquipmentContainer} («byreference», «ofAggregate»),
 * {@code Terminal.phases} («enumeration», «attribute») and
 * {@code Terminal.ConductingEquipment} («byreference») carry only structural
 * stereotypes, and are not labelled.</li>
 * </ul>
 */
@NoNetwork
class StereotypeLabelTest {

	/** The label expected on each row: the user stereotype, or null for none. */
	private static final Map<String, String> EXPECTED = Map.of( //
			"energyIdentCodeEic", "European", //
			"AggregatedEquipment", "NC", //
			"mRID", "", //
			"aggregate", "", //
			"EquipmentContainer", "", //
			"phases", "", //
			"ConductingEquipment", "");

	/**
	 * The first cell of an AsciiDoc table row naming a property: an optional
	 * role such as {@code [.extension (.NC) ]##}, the name, an optional
	 * multiplicity or {@code (@ref)}, and an optional label in parentheses.
	 */
	private static final Pattern ASCIIDOC_ROW = Pattern.compile(
			"^\\|(?<role>\\[[^\\]]*\\]##)?(?<name>\\w+)(?: \\[[0-9]+\\.\\.(?:[0-9]+|\\*)\\]| \\(@ref\\))?(?:##)?(?: \\((?<label>[^)]*)\\))?\\s*$");

	/** The first cell of an RTF table row naming a property, with an optional label. */
	private static final Pattern RTF_ROW = Pattern
			.compile("\\\\fs16 (?<name>\\w+)(?: \\((?<label>[^)]*)\\))?\\s*\\\\cell\\}");

	static Stream<String> documentationBuilders() {
		BuilderRegistry registry = BuilderRegistry.getInstance();
		return Stream.concat(
				registry.getBuilderNames().stream()
						.filter(name -> registry.getBuilder(name).getType() == BuilderType.ASCIIDOC).sorted(),
				Stream.of("profile-doc-rtf"));
	}

	@Test
	void documentationBuildersAreRegistered() {
		assertTrue(documentationBuilders().count() > 1, "ASCIIDOC builders and profile-doc-rtf are registered");
		assertTrue(BuilderRegistry.getInstance().getBuilderNames().contains("profile-doc-rtf"),
				"profile-doc-rtf is registered");
	}

	@ParameterizedTest
	@MethodSource("documentationBuilders")
	void labelsRowsWithUserStereotypesOnly(String builder, @TempDir Path output) throws Exception {
		CLIProfileTransformer transformer = TestProjects.sample();
		transformer.setBuilder(builder);
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();
		String text = Files.readString(transformer.getOutputFile().toPath(), StandardCharsets.UTF_8);
		boolean rtf = builder.equals("profile-doc-rtf");

		List<String> seen = new ArrayList<>();
		for (String line : text.split("\\r?\\n")) {
			Matcher row = (rtf ? RTF_ROW : ASCIIDOC_ROW).matcher(line);
			if (!(rtf ? row.find() : row.matches()) || !EXPECTED.containsKey(row.group("name")))
				continue;
			String name = row.group("name");
			String expected = EXPECTED.get(name);
			seen.add(name);
			if (expected.isEmpty()) {
				assertNull(row.group("label"), builder + " labels " + name + ": " + line);
				if (!rtf)
					assertNull(row.group("role"), builder + " gives " + name + " a role: " + line);
			} else {
				assertEquals(expected, row.group("label"), builder + " label on " + name + ": " + line);
			}
		}
		for (String name : EXPECTED.keySet())
			assertTrue(seen.contains(name), builder + " writes a row for " + name);
		assertFalse(seen.isEmpty());
	}
}
