package au.com.langdale.cimtoole.test.headless;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.BooleanSupplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.SchemaMergeMarkers;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.cimtoole.test.WorkspaceTest;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.SchemaMerger;

/**
 * Tests for the base CIM schema of a project with more than one schema file
 * (issue #391): the merged schema is built around the base, a problem choosing
 * the base is shown as markers on the schema files, and the project's profiles
 * are not built until it is resolved.
 * <p>
 * The project has two small XMI schemas: {@code Base.xmi}, whose namespace is the
 * Schema Namespace preference, and {@code European.xmi}, an extension.
 */
public class BaseSchemaTests extends WorkspaceTest {

	private static final String RESOURCES = "CIMToolTestFiles/BaseSchema/";
	private static final String BASE_NS = "http://iec.ch/TC57/CIM100#";
	private static final String EUROPEAN_NS = "http://iec.ch/TC57/CIM100-European#";
	private static final long TIMEOUT_MILLIS = 30000;

	private static final XSDBuildlet xsdBuildlet = new XSDBuildlet("xsd", "xsd");

	private IPreferenceStore store;
	private IFolder schemaFolder;
	private IFile base;
	private IFile european;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setValue(Info.SCHEMA_NAMESPACE.getLocalName(), BASE_NS);
		workspace.run(Task.createProject(project, null), monitor);
		schemaFolder = Info.getSchemaFolder(project);
		base = importSchema("Base.xmi", BASE_NS);
		european = importSchema("European.xmi", EUROPEAN_NS);
	}

	@Override
	protected void tearDown() throws Exception {
		store.setToDefault(Info.SCHEMA_NAMESPACE.getLocalName());
		super.tearDown();
	}

	public final void testPreferenceChoosesTheBase() throws Exception {
		// Base.xmi has the Schema Namespace preference and is not flagged
		SchemaMerger.Result result = merge();
		assertEquals("base CIM schema", base.getName(), result.getBase().getName());
		assertFalse(result.hasErrors());
		assertEquals(BASE_NS, Info.getSchemaNamespace(project));
		assertEquals("no markers", 0, markers(base).length + markers(european).length);
	}

	public final void testFlagChoosesTheBase() throws Exception {
		flag(european, true);
		waitFor("the flagged file to become the base", () -> european.getName().equals(baseName()));
		assertEquals(EUROPEAN_NS, Info.getSchemaNamespace(project));
	}

	public final void testTwoFlagsAreMarkedAsErrors() throws Exception {
		flag(base, true);
		flag(european, true);
		waitFor("markers on both files", () -> isMarked(base) && isMarked(european));
		IMarker marker = markers(base)[0];
		assertEquals(IMarker.SEVERITY_ERROR, marker.getAttribute(IMarker.SEVERITY, -1));
		assertEquals(SchemaMerger.ProblemType.MULTIPLE_BASE_SCHEMAS.name(),
				marker.getAttribute(SchemaMergeMarkers.PROBLEM_TYPE, ""));
		assertTrue("marker states the action",
				marker.getAttribute(IMarker.MESSAGE, "").contains("Base CIM schema"));
		assertNull("no base", merge().getBase());
	}

	public final void testMarkersClearWhenResolved() throws Exception {
		flag(base, true);
		flag(european, true);
		waitFor("markers on both files", () -> isMarked(base) && isMarked(european));
		flag(european, false);
		waitFor("markers removed", () -> !isMarked(base) && !isMarked(european));
		assertEquals(base.getName(), baseName());
	}

	public final void testProfileIsNotBuiltWithoutABase() throws Exception {
		IFile profile = Info.getProfilesFolder(project).getFile("MultiSchemaProfile.owl");
		workspace.run(Task.importProfile(profile, copy("MultiSchemaProfile.owl").getAbsolutePath()), monitor);
		OntModel model = Task.getProfileModel(profile);
		xsdBuildlet.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
		IFile xsd = Info.getRelated(profile, "xsd");

		flag(base, true);
		flag(european, true);
		waitFor("the merge to fail", () -> hasErrors());
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		assertFalse("profile not built without a base CIM schema", xsd.exists());

		flag(european, false);
		waitFor("the merge to succeed", () -> !hasErrors());
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		assertTrue("profile built once the base CIM schema is resolved", xsd.exists());
	}

	public final void testSetBaseSchemaMovesTheFlag() throws Exception {
		flag(base, true);
		workspace.run(Task.setBaseSchema(european), monitor);
		assertTrue("new base flagged", isFlagged(european));
		assertFalse("previous base unflagged", isFlagged(base));
		waitFor("the new base to be chosen", () -> european.getName().equals(baseName()));
	}

	public final void testSetBaseSchemaClearsStrayFlags() throws Exception {
		flag(base, true);
		flag(european, true);
		workspace.run(Task.setBaseSchema(base), monitor);
		assertTrue(isFlagged(base));
		assertFalse("the second flag is removed", isFlagged(european));
		waitFor("the markers to clear", () -> !isMarked(base) && !isMarked(european));
	}

	public final void testImportAndSetBaseInOneOperation() throws Exception {
		IFile extension = schemaFolder.getFile("Extension.xmi");
		workspace.run(Task.chain(
				Task.importSchema(extension, copy("European.xmi").getAbsolutePath(), "http://example.com/ext#",
						ProjectTest.MERGE_SHADOW_EXTENSIONS, ProjectTest.SELF_HEAL_ON_IMPORT),
				Task.setBaseSchema(extension)), monitor);
		assertTrue("imported file flagged", isFlagged(extension));
		waitFor("the imported file to be the base", () -> extension.getName().equals(baseName()));
	}

	public final void testRemoveSchemaRemovesCompanionFiles() throws Exception {
		IFile namespaces = schemaFolder.getFile("Base.namespaces");
		IFile annotation = schemaFolder.getFile("Base.annotation");
		namespaces.create(new java.io.ByteArrayInputStream(new byte[0]), true, monitor);
		annotation.create(new java.io.ByteArrayInputStream(new byte[0]), true, monitor);
		IFile otherNamespaces = schemaFolder.getFile("European.namespaces");
		otherNamespaces.create(new java.io.ByteArrayInputStream(new byte[0]), true, monitor);

		workspace.run(Task.removeSchema(base), monitor);
		assertFalse("schema removed", base.exists());
		assertFalse("companion removed", namespaces.exists());
		assertFalse("companion removed", annotation.exists());
		assertTrue("another file's companion kept", otherNamespaces.exists());
		assertNull("settings removed with the file",
				CIMToolPlugin.getSettings().getSetting(base, Info.SCHEMA_NAMESPACE));
		waitFor("the remaining file to be the base", () -> european.getName().equals(baseName()));
	}

	private boolean isFlagged(IFile file) {
		return Info.isFlaggedAsBaseSchema(file);
	}

	private IFile importSchema(String name, String namespace) throws Exception {
		IFile file = schemaFolder.getFile(name);
		workspace.run(Task.importSchema(file, copy(name).getAbsolutePath(), namespace, ProjectTest.MERGE_SHADOW_EXTENSIONS,
				ProjectTest.SELF_HEAL_ON_IMPORT), monitor);
		return file;
	}

	private File copy(String name) throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir"), "BaseSchemaTests-" + name);
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCES + name)) {
			assertNotNull("test resource " + name, in);
			Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		file.deleteOnExit();
		return file;
	}

	private void flag(IFile file, boolean value) {
		CIMToolPlugin.getSettings().putSetting(file, Info.CANONICAL_SCHEMA, Boolean.toString(value));
	}

	private SchemaMerger.Result merge() throws CoreException {
		return CIMToolPlugin.getCache().getSchemaMergeWait(schemaFolder);
	}

	private String baseName() {
		try {
			SchemaMerger.SchemaFile file = merge().getBase();
			return file == null ? null : file.getName();
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean hasErrors() {
		try {
			return merge().hasErrors();
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private IMarker[] markers(IFile file) throws CoreException {
		return file.findMarkers(SchemaMergeMarkers.MARKER_TYPE, false, IResource.DEPTH_ZERO);
	}

	private boolean isMarked(IFile file) {
		try {
			return markers(file).length > 0;
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Waits for the background merge and marker jobs that a settings change
	 * starts, until the condition holds.
	 */
	private void waitFor(String what, BooleanSupplier condition) throws Exception {
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				fail("timed out waiting for " + what);
			Job.getJobManager().join(SchemaMergeMarkers.JOB_FAMILY, monitor);
			Thread.sleep(100);
		}
	}
}
