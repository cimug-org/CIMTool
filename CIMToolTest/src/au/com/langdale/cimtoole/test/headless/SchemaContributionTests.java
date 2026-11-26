/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.CopyBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.Format;
import au.com.langdale.kena.OntModel;

/**
 * Every file in a project's Schema folder that the merged schema includes,
 * mapping definitions among them, counts as a schema change for the builder.
 * Adding, changing or removing such a file rebuilds the profile artifacts and
 * refreshes the merged schema export, as a change to a primary schema file
 * does. The merged schema export itself never causes another rebuild, even
 * when it is written into the Schema folder.
 */
public class SchemaContributionTests extends ProjectTest {

	private static final String MAPPING = "TestMappings.mapping-ttl";
	private static final String MAPPING_NS = "http://example.com/mapping#";
	private static final String CHANGED_MAPPING_NS = "http://example.com/changed-mapping#";
	private static final String MAPPING_ENVELOPE = "Mappings";
	private static final String MERGED_SCHEMA = "schema.merged-owl";

	private static final CopyBuildlet TTL = new CopyBuildlet(Format.TURTLE.toFormat(), "ttl");

	private IFolder schemaFolder;
	private IFile mapping;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		schemaFolder = Info.getSchemaFolder(project);
		mapping = schemaFolder.getFile(MAPPING);
	}

	public final void testMappingFilesContribute() {
		assertTrue("Turtle mapping", Info.contributesToSchema(schemaFolder.getFile("a.mapping-ttl")));
		assertTrue("RDF/XML mapping", Info.contributesToSchema(schemaFolder.getFile("a.mapping-owl")));
		assertTrue("mapping in a subfolder",
				Info.contributesToSchema(schemaFolder.getFolder("sub").getFile("a.mapping-ttl")));
	}

	public final void testPrimarySchemaFilesContribute() {
		assertTrue("XMI schema", Info.contributesToSchema(schemaFolder.getFile("a.xmi")));
		assertTrue("OWL schema", Info.contributesToSchema(schemaFolder.getFile("a.owl")));
		assertTrue("EA project", Info.contributesToSchema(schemaFolder.getFile("a.qea")));
	}

	public final void testFilesOutsideSchemaDoNotContribute() {
		assertFalse("profile", Info.contributesToSchema(profile));
		assertFalse("merged schema export at the project root",
				Info.contributesToSchema(project.getFile(MERGED_SCHEMA)));
		assertFalse("merged schema export in the Schema folder",
				Info.contributesToSchema(schemaFolder.getFile(MERGED_SCHEMA)));
		assertFalse("Schema folder", Info.contributesToSchema(schemaFolder));
		assertFalse("file that is not parsed", Info.contributesToSchema(schemaFolder.getFile("readme.txt")));
	}

	public final void testAddingMappingRebuildsArtifacts() throws CoreException {
		buildProject(MERGED_SCHEMA);
		Stamps before = new Stamps();
		createMapping(MAPPING_NS);
		build();
		before.assertRebuilt();
	}

	public final void testChangingMappingRebuildsArtifacts() throws CoreException {
		buildProject(MERGED_SCHEMA);
		createMapping(MAPPING_NS);
		build();
		Stamps before = new Stamps();
		createMapping(CHANGED_MAPPING_NS);
		build();
		before.assertRebuilt();
	}

	public final void testRemovingMappingRebuildsArtifacts() throws CoreException {
		buildProject(MERGED_SCHEMA);
		createMapping(MAPPING_NS);
		build();
		Stamps before = new Stamps();
		mapping.delete(true, monitor);
		build();
		before.assertRebuilt();
	}

	public final void testMergedOutputInSchemaFolderDoesNotRebuildAgain() throws CoreException {
		buildProject("Schema/" + MERGED_SCHEMA);
		assertTrue("merged schema written", schemaFolder.getFile(MERGED_SCHEMA).exists());
		build();
		long artifact = artifact().getModificationStamp();
		build();
		assertEquals("profile artifact left alone", artifact, artifact().getModificationStamp());
	}

	/**
	 * Imports the schema and a profile that has the Turtle builder enabled, sets
	 * the merged schema export, and runs a full build.
	 */
	private void buildProject(String mergedSchemaPath) throws CoreException {
		setupSchema();
		setupProfile();
		OntModel model = Task.getProfileModel(profile);
		TTL.setFlagged(model, true);
		workspace.run(Task.saveProfile(profile, model), monitor);
		Info.putProperty(project, Info.MERGED_SCHEMA_PATH, mergedSchemaPath);
		workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		assertTrue("profile artifact built", artifact().exists());
		assertTrue("merged schema built", project.getFile(mergedSchemaPath).exists());
	}

	private void createMapping(String namespace) throws CoreException {
		workspace.run(Task.createMappings(mapping, namespace, MAPPING_ENVELOPE), monitor);
	}

	private void build() throws CoreException {
		workspace.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
	}

	private IFile artifact() {
		return getRelated(TTL.getFileExt());
	}

	/**
	 * The modification stamps of the profile artifact and the merged schema
	 * export, taken before a change, to show that both were written again.
	 */
	private class Stamps {
		private final IFile merged = project.getFile(MERGED_SCHEMA);
		private final long artifactStamp = artifact().getModificationStamp();
		private final long mergedStamp = merged.getModificationStamp();

		void assertRebuilt() {
			assertTrue("profile artifact rebuilt", artifact().getModificationStamp() != artifactStamp);
			assertTrue("merged schema refreshed", merged.getModificationStamp() != mergedStamp);
		}
	}
}
