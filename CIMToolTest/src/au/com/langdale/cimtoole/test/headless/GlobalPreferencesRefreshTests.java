package au.com.langdale.cimtoole.test.headless;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.GlobalPreferencesExporter;
import au.com.langdale.cimtoole.project.GlobalPreferencesSynchronizer;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;

/**
 * Tests for the export that {@link GlobalPreferencesSynchronizer} runs for every
 * open CIMTool project when CIMTool starts (issue #339). A project's
 * {@code .cimtool-global-preferences} file mirrors the workspace preferences,
 * so the export must refresh an existing file as well as create a missing one,
 * and must leave a file that already matches the workspace untouched.
 */
public class GlobalPreferencesRefreshTests extends WorkspaceTest {

	private static final String SPACING = Info.HORIZONTAL_SPACING.getLocalName();

	private IPreferenceStore store;
	private IFile prefsFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workspace.run(Task.createProject(project, null), monitor);
		prefsFile = Info.getGlobalPreferences(project);
		store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setValue(SPACING, "42");
	}

	@Override
	protected void tearDown() throws Exception {
		store.setToDefault(SPACING);
		super.tearDown();
	}

	public final void testRefreshesExistingFile() throws Exception {
		write("@prefix prefs: <http://cimtoole.langdale.com.au/2009/builder-prefs#> .\n"
				+ "<http://cimtoole.langdale.com.au/2009/global-prefs#> prefs:" + SPACING + " \"999\" .\n");
		GlobalPreferencesExporter.exportGlobalPreferencesToAllOpenProjects();
		String content = read();
		assertTrue("workspace value written", content.contains("\"42\""));
		assertFalse("value from another workspace replaced", content.contains("\"999\""));
	}

	public final void testLeavesUpToDateFileUntouched() throws Exception {
		GlobalPreferencesExporter.exportGlobalPreferencesToAllOpenProjects();
		long stamp = prefsFile.getModificationStamp();
		GlobalPreferencesExporter.exportGlobalPreferencesToAllOpenProjects();
		assertEquals("up-to-date file not rewritten", stamp, prefsFile.getModificationStamp());
	}

	public final void testCreatesMissingFile() throws Exception {
		if (prefsFile.exists())
			prefsFile.delete(true, monitor);
		GlobalPreferencesExporter.exportGlobalPreferencesToAllOpenProjects();
		assertTrue("file created", prefsFile.exists());
		assertTrue("workspace value written", read().contains("\"42\""));
	}

	private void write(String content) throws CoreException {
		InputStream source = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		if (prefsFile.exists())
			prefsFile.setContents(source, true, false, monitor);
		else
			prefsFile.create(source, true, monitor);
	}

	private String read() throws CoreException, IOException {
		try (InputStream in = prefsFile.getContents(true)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
