package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;

/**
 * The copyright notices that the SQL and HTML builders write (issue #272),
 * using the copyright templates bundled with CIMTool.
 *
 * <ul>
 * <li>{@code sql.xsl} and {@code sql-rdfs-ansi92.xsl} write the multi-line
 * template as SQL comments at the top of the file, inside the header banner,
 * with each line prefixed by {@code -- }, its indentation removed, and a blank
 * line written as {@code --}.</li>
 * <li>{@code html.xsl} writes the single-line template as a footer at the end of
 * the page.</li>
 * <li>With the empty templates, none of the three writes a copyright block.</li>
 * </ul>
 *
 * <p>
 * The year variable in the templates is replaced with the current year (issue
 * #287).
 * </p>
 */
@NoNetwork
class CopyrightBuildersTest {

	private static final String BANNER = "-- ==========================================================================================";
	private static final String SEPARATOR = "-- -------------------------------------------------------------------------------------";
	private static final Pattern FOOTER = Pattern.compile("<div class=\"footer\"[^>]*>(.*?)</div>", Pattern.DOTALL);

	@TempDir
	Path project;

	@TempDir
	Path output;

	@ParameterizedTest
	@ValueSource(strings = { "sql", "sql-rdfs-ansi92" })
	void sqlBuildersPrependTheMultiLineCopyright(String builder) throws Exception {
		List<String> lines = build(builder, true);

		List<String> expected = new ArrayList<>();
		expected.add(BANNER);
		expected.add("--");
		for (String line : bundledTemplate("default-copyright-template-multi-line.txt").strip().split("\\r?\\n", -1))
			expected.add(line.isBlank() ? "--" : "-- " + line.strip());
		expected.add("--");
		expected.add(SEPARATOR);
		expected.add("--");

		assertTrue(lines.size() > expected.size(), builder + " output is longer than its header");
		assertEquals(expected, lines.subList(0, expected.size()), builder + " header");
		assertTrue(lines.get(2).equals("-- Copyright " + Year.now() + " UCAIug"), lines.get(2));
	}

	@Test
	void htmlBuilderAppendsTheSingleLineCopyright() throws Exception {
		String html = String.join("\n", build("html", true));

		Matcher footer = FOOTER.matcher(html);
		assertTrue(footer.find(), "html output has a copyright footer");
		assertEquals("Copyright " + Year.now() + " UCAIug SPDX-License-Identifier: Apache-2.0", footer.group(1).strip());
		assertTrue(html.indexOf("</body>") > footer.end(), "footer is inside the body");
		assertFalse(footer.find(), "html output has one copyright footer");
	}

	@ParameterizedTest
	@ValueSource(strings = { "sql", "sql-rdfs-ansi92" })
	void sqlBuildersWriteNoCopyrightWhenTheTemplateIsEmpty(String builder) throws Exception {
		List<String> lines = build(builder, false);

		assertEquals(BANNER, lines.get(0));
		assertFalse(lines.get(1).equals("--"), builder + " writes no copyright block: " + lines.get(1));
		assertFalse(lines.contains(SEPARATOR), builder + " writes no copyright separator");
	}

	@Test
	void htmlBuilderWritesNoFooterWhenTheTemplateIsEmpty() throws Exception {
		String html = String.join("\n", build("html", false));

		assertFalse(FOOTER.matcher(html).find(), "html output has no copyright footer");
	}

	/**
	 * Runs a builder on the minimal project with the bundled default or empty
	 * copyright templates and returns its output lines. The year variable must be
	 * replaced in every case.
	 */
	private List<String> build(String builder, boolean withCopyright) throws Exception {
		CLIProfileTransformer transformer = TestProjects.minimal(project);
		if (withCopyright)
			transformer.loadDefaultCopyrightTemplates();
		else
			transformer.loadEmptyCopyrightTemplates();
		transformer.setBuilder(builder);
		transformer.setOutputDirectory(output.toFile());
		transformer.transform();
		String text = Files.readString(transformer.getOutputFile().toPath(), StandardCharsets.UTF_8);
		assertFalse(text.contains("${year}") || text.contains("${YEAR}"), builder + " replaces the year variable");
		return List.of(text.split("\\r?\\n", -1));
	}

	private static String bundledTemplate(String name) throws Exception {
		try (var in = CopyrightBuildersTest.class.getResourceAsStream("/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("${year}", Year.now().toString());
		}
	}
}
