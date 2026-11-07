package au.com.langdale.cimtoole.test.headless;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

/**
 * Tests for {@link Task#ensureProfilesAsciiDoctorConfigFile}, which maintains
 * the Asciidoctor editor configuration in a project's Profiles folder so that
 * {profilesdir} resolves for the AsciiDoc builders' plantuml:: references
 * (issue #291).
 */
public class ProfilesAsciiDoctorConfigTests extends WorkspaceTest {

	private static final String DEFINITION = ":profilesdir: {asciidoctorconfigdir}";

	private IFile config;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workspace.run(Task.createProject(project, null), monitor);
		config = Info.getProfilesFolder(project).getFile(Task.PROFILES_ASCIIDOCTOR_CONFIG_FILE);
	}

	public final void testCreatesConfigWhenMissing() throws Exception {
		assertFalse("config absent after project creation", config.exists());
		ensureConfig();
		assertTrue("config created", config.exists());
		assertEquals("profilesdir defined once", 1, countDefinitions(read(config)));
	}

	public final void testLeavesExistingDefinitionUntouched() throws Exception {
		String original = ":profilesdir: ../Generated\n";
		create(original);
		ensureConfig();
		assertEquals("existing definition untouched", original, read(config));
	}

	public final void testLeavesUnsetDefinitionUntouched() throws Exception {
		String original = ":profilesdir!:\n";
		create(original);
		ensureConfig();
		assertEquals("explicit unset untouched", original, read(config));
	}

	public final void testAppendsDefinitionWhenMissing() throws Exception {
		String original = ":toc: left\n";
		create(original);
		ensureConfig();
		String content = read(config);
		assertTrue("original content kept", content.startsWith(original));
		assertEquals("profilesdir defined once", 1, countDefinitions(content));
		assertFalse("LF line endings kept", content.contains("\r"));
	}

	public final void testAppendsWithCRLFLineEndings() throws Exception {
		String original = ":toc: left\r\n";
		create(original);
		ensureConfig();
		String content = read(config);
		assertTrue("original content kept", content.startsWith(original));
		assertEquals("profilesdir defined once", 1, countDefinitions(content));
		assertFalse("CRLF line endings kept", content.replace("\r\n", "").contains("\n"));
	}

	public final void testAppendsAfterUnterminatedLastLine() throws Exception {
		create(":toc: left");
		ensureConfig();
		String content = read(config);
		assertTrue("last line terminated before the addition", content.startsWith(":toc: left\n"));
		assertEquals("profilesdir defined once", 1, countDefinitions(content));
	}

	public final void testAppendKeepsLocalHistory() throws Exception {
		create(":toc: left\n");
		ensureConfig();
		assertTrue("previous content in local history", config.getHistory(monitor).length > 0);
	}

	public final void testRepeatedRunsAddNothing() throws Exception {
		create(":toc: left\n");
		ensureConfig();
		String once = read(config);
		ensureConfig();
		assertEquals("second run changes nothing", once, read(config));
	}

	public final void testNoProfilesFolder() throws Exception {
		Info.getProfilesFolder(project).delete(true, monitor);
		ensureConfig();
		assertFalse("Profiles folder not recreated", Info.getProfilesFolder(project).exists());
	}

	private void ensureConfig() throws CoreException {
		workspace.run(Task.ensureProfilesAsciiDoctorConfigFile(project), monitor);
	}

	private void create(String content) throws CoreException {
		config.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, monitor);
	}

	private static String read(IFile file) throws CoreException, IOException {
		try (InputStream in = file.getContents(true)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static int countDefinitions(String content) {
		Matcher matcher = Pattern.compile("(?m)^" + Pattern.quote(DEFINITION) + "\r?$").matcher(content);
		int count = 0;
		while (matcher.find())
			count++;
		return count;
	}
}
