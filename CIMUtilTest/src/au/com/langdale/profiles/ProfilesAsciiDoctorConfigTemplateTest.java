package au.com.langdale.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Verifies the template that CIMTool copies to a project's
 * Profiles/.asciidoctorconfig.adoc (issue #291).
 */
class ProfilesAsciiDoctorConfigTemplateTest {

	private static final String TEMPLATE = "/profiles/asciidoctorconfig.adoc";

	@Test
	void definesProfilesDirOnceAsTheConfigFolder() throws IOException {
		try (InputStream in = getClass().getResourceAsStream(TEMPLATE)) {
			assertNotNull(in, "template on the CIMUtil classpath: " + TEMPLATE);
			String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			long definitions = content.lines().filter(line -> line.startsWith(":profilesdir")).count();
			assertEquals(1, definitions, "one profilesdir attribute line");
			assertEquals(1, content.lines().filter(":profilesdir: {asciidoctorconfigdir}"::equals).count(),
					"profilesdir is the folder containing the configuration file");
		}
	}
}
