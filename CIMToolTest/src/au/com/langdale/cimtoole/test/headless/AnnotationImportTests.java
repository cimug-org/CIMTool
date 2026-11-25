/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

import com.hp.hpl.jena.graph.Node;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.ProjectTest;
import au.com.langdale.kena.OntModel;

/**
 * An annotation file is a companion file of an XMI schema only.
 *
 * <ul>
 * <li>Importing an XMI schema copies the annotation file found beside it, with
 * the same base name, into the project's Schema folder, and the annotation is
 * applied when the schema is read.</li>
 * <li>Importing an Enterprise Architect project file does not copy an
 * annotation file, and an annotation file placed beside an EA project file in
 * the Schema folder has no effect on the schema read from it.</li>
 * <li>An EA project file's own companion files ({@code .namespaces},
 * {@code .namespace-prefixes} and {@code .stereotype-extensions}) are still
 * copied with it.</li>
 * </ul>
 */
public class AnnotationImportTests extends ProjectTest {

	private static final String ANNOTATED_NS = "http://example.com/annotated#";
	private static final String OTHER_ANNOTATED_NS = "http://example.com/other-annotated#";

	private static final String XMI_BASE = "cim11v09combined";
	private static final String QEA_BASE = "CIM17-2_CGMES31v01_PROF-20v02_NC23v66_MS10v01_DES10v01";

	/** A folder of source files to import, outside the workspace. */
	private Path source;
	private IFolder schemaFolder;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		source = Paths.get(getSamplesFolder(), "annotation-import");
		Files.createDirectories(source);
		schemaFolder = Info.getSchemaFolder(project);
	}

	/**
	 * An annotation file that assigns the given namespace to one package, written
	 * with the relative package prefix of the example annotation file.
	 */
	private static String annotation(String packageName, String namespace) {
		return "@prefix uml: <http://langdale.com.au/2005/UML#> .\n" //
				+ "@prefix package: <#Package_> .\n\n" //
				+ "package:" + packageName + " uml:baseuri \"" + namespace + "\" .\n";
	}

	private Path copySample(String sampleName, String targetName) throws IOException {
		return Files.copy(Paths.get(getSamplesFolder(), sampleName), source.resolve(targetName),
				StandardCopyOption.REPLACE_EXISTING);
	}

	private Path writeSource(String name, String content) throws IOException {
		return Files.write(source.resolve(name), content.getBytes(StandardCharsets.UTF_8));
	}

	private void importSchema(IFile file, Path pathname, String namespace) throws CoreException {
		Task.importSchema(file, pathname.toString(), namespace, MERGE_SHADOW_EXTENSIONS, SELF_HEAL_ON_IMPORT)
				.run(monitor);
	}

	private static String contents(IFile file) throws CoreException, IOException {
		try (InputStream in = file.getContents()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static boolean defines(OntModel model, String uri) {
		return model.getGraph().contains(Node.createURI(uri), Node.ANY, Node.ANY);
	}

	private void createFile(IFile file, String content) throws CoreException {
		file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, monitor);
	}

	public final void testXMIImportCopiesAnnotation() throws Exception {
		String content = annotation("Wires", ANNOTATED_NS);
		Path xmi = copySample(SAMPLE_SCHEMA_XMI, XMI_BASE + ".xmi");
		writeSource(XMI_BASE + ".annotation", content);

		IFile file = schemaFolder.getFile(XMI_BASE + ".xmi");
		importSchema(file, xmi, SCHEMA_NS);

		IFile annotation = schemaFolder.getFile(XMI_BASE + ".annotation");
		assertTrue("annotation file copied", annotation.exists());
		assertEquals("annotation file content", content, contents(annotation));
	}

	public final void testAnnotationAppliedToXMISchema() throws Exception {
		Path xmi = copySample(SAMPLE_SCHEMA_XMI, XMI_BASE + ".xmi");
		writeSource(XMI_BASE + ".annotation", annotation("Wires", ANNOTATED_NS));

		IFile file = schemaFolder.getFile(XMI_BASE + ".xmi");
		importSchema(file, xmi, SCHEMA_NS);

		OntModel model = Task.parse(file);
		assertTrue("class of the annotated package", defines(model, ANNOTATED_NS + "ACLineSegment"));
		assertFalse("class of the annotated package in the schema namespace",
				defines(model, SCHEMA_NS + "ACLineSegment"));
		assertTrue("class of another package", defines(model, SCHEMA_NS + "IdentifiedObject"));
	}

	public final void testNameWithSpacesKeepsNamesMatched() throws Exception {
		Path xmi = copySample(SAMPLE_SCHEMA_XMI, "cim 11 combined.xmi");
		writeSource("cim 11 combined.annotation", annotation("Wires", ANNOTATED_NS));

		importSchema(schemaFolder.getFile("cim 11 combined.xmi"), xmi, SCHEMA_NS);

		assertTrue("schema file renamed", schemaFolder.getFile("cim_11_combined.xmi").exists());
		assertTrue("annotation file renamed to match", schemaFolder.getFile("cim_11_combined.annotation").exists());
		assertFalse("annotation file under its original name",
				schemaFolder.getFile("cim 11 combined.annotation").exists());
	}

	public final void testReimportReplacesAnnotation() throws Exception {
		Path xmi = copySample(SAMPLE_SCHEMA_XMI, XMI_BASE + ".xmi");
		IFile file = schemaFolder.getFile(XMI_BASE + ".xmi");
		writeSource(XMI_BASE + ".annotation", annotation("Wires", ANNOTATED_NS));
		importSchema(file, xmi, SCHEMA_NS);

		String changed = annotation("Wires", OTHER_ANNOTATED_NS);
		writeSource(XMI_BASE + ".annotation", changed);
		importSchema(file, xmi, SCHEMA_NS);

		assertEquals("annotation file replaced", changed, contents(schemaFolder.getFile(XMI_BASE + ".annotation")));
	}

	public final void testNoAnnotationNoneCreated() throws Exception {
		Path xmi = copySample(SAMPLE_SCHEMA_XMI, XMI_BASE + ".xmi");
		Files.deleteIfExists(source.resolve(XMI_BASE + ".annotation"));

		importSchema(schemaFolder.getFile(XMI_BASE + ".xmi"), xmi, SCHEMA_NS);

		assertTrue("schema file copied", schemaFolder.getFile(XMI_BASE + ".xmi").exists());
		assertFalse("no annotation file", schemaFolder.getFile(XMI_BASE + ".annotation").exists());
	}

	public final void testEAImportDoesNotCopyAnnotation() throws Exception {
		Path qea = copySample(SAMPLE_SCHEMA_QEA, QEA_BASE + ".qea");
		writeSource(QEA_BASE + ".annotation", annotation("Core", ANNOTATED_NS));

		importSchema(schemaFolder.getFile(QEA_BASE + ".qea"), qea, SCHEMA_CIM100_NS);

		assertTrue("EA project file copied", schemaFolder.getFile(QEA_BASE + ".qea").exists());
		assertFalse("annotation file not copied", schemaFolder.getFile(QEA_BASE + ".annotation").exists());
	}

	public final void testEAImportStillCopiesCompanionFiles() throws Exception {
		String base = "CGMES model";
		Path qea = copySample(SAMPLE_SCHEMA_QEA, base + ".qea");
		copySample(QEA_BASE + ".namespaces", base + ".namespaces");
		copySample(QEA_BASE + ".namespace-prefixes", base + ".namespace-prefixes");
		writeSource(base + ".stereotype-extensions", "European\n");

		importSchema(schemaFolder.getFile(base + ".qea"), qea, SCHEMA_CIM100_NS);

		for (String extension : new String[] { "qea", "namespaces", "namespace-prefixes", "stereotype-extensions" })
			assertTrue("." + extension + " file copied and renamed",
					schemaFolder.getFile("CGMES_model." + extension).exists());
	}

	public final void testAnnotationBesideEAProjectIgnored() throws Exception {
		// The EA project file is imported without its .namespaces file, which would
		// otherwise take precedence over an annotation, so the annotation would take
		// effect if it were read.
		Path qea = copySample(SAMPLE_SCHEMA_QEA, QEA_BASE + ".qea");
		IFile file = schemaFolder.getFile(QEA_BASE + ".qea");
		importSchema(file, qea, SCHEMA_CIM100_NS);
		createFile(schemaFolder.getFile(QEA_BASE + ".annotation"), annotation("Core", ANNOTATED_NS));

		OntModel model = Task.parse(file);
		assertTrue("class of the package named in the annotation",
				defines(model, SCHEMA_CIM100_NS + "IdentifiedObject"));
		assertFalse("annotation applied", defines(model, ANNOTATED_NS + "IdentifiedObject"));
	}

	public final void testFindMasterForAnnotation() throws Exception {
		createFile(schemaFolder.getFile("a.xmi"), "");
		createFile(schemaFolder.getFile("a.annotation"), "");
		createFile(schemaFolder.getFile("b.qea"), "");
		createFile(schemaFolder.getFile("b.annotation"), "");
		createFile(schemaFolder.getFile("c.annotation"), "");

		assertEquals("XMI master", schemaFolder.getFile("a.xmi"), Info.findMasterFor(schemaFolder.getFile("a.annotation")));
		assertNull("beside an EA project file", Info.findMasterFor(schemaFolder.getFile("b.annotation")));
		assertNull("beside no schema", Info.findMasterFor(schemaFolder.getFile("c.annotation")));
	}

	@Override
	protected void tearDownTestData() {
		File folder = source == null ? null : source.toFile();
		if (folder != null && folder.isDirectory())
			for (File f : folder.listFiles())
				f.delete();
		super.tearDownTestData();
	}
}
