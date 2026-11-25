/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hp.hpl.jena.graph.Node;

import au.com.langdale.cimutil.test.NoNetwork;
import au.com.langdale.cimutil.test.TestProjects;
import au.com.langdale.kena.OntModel;

/**
 * The CLI reads an annotation file for an XMI schema only.
 *
 * <ul>
 * <li>An annotation file beside an XMI schema, with the same base name, is
 * applied when the schema is parsed. The test annotation names its package with
 * the relative package prefix of the example annotation file, so it also
 * confirms that the prefix resolves against the schema namespace.</li>
 * <li>An annotation file beside an Enterprise Architect project file is
 * ignored. The test annotation names a package with no namespace of its own,
 * and the schema is parsed without its {@code .namespaces} file, which would
 * otherwise take precedence, so the annotation would take effect if the file
 * were read.</li>
 * </ul>
 */
@NoNetwork
class CLISchemaParserAnnotationTest {

	private static final String ANNOTATED_NS = "http://example.com/annotated#";

	private static final String XMI_ZIP = "CIMToolTest/CIMToolTestFiles/CIMToolTestFiles.zip";
	private static final String CGMES_ZIP = "CIMToolTest/CIMToolTestFiles/CIMToolTestFiles-CGMES.zip";

	private static final String XMI_SCHEMA = "cim11v09combined";
	private static final String XMI_NS = "http://iec.ch/TC57/2007/CIM-schema-cim12#";

	private static String annotation(String packageName) {
		return "@prefix uml: <http://langdale.com.au/2005/UML#> .\n" //
				+ "@prefix package: <#Package_> .\n\n" //
				+ "package:" + packageName + " uml:baseuri \"" + ANNOTATED_NS + "\" .\n";
	}

	private static String settings(String schemaFile, String namespace) {
		return "@prefix project: <http://cimtoole.langdale.com.au/2009/project/> .\n"
				+ "@prefix tool: <http://cimtoole.langdale.com.au/2009/setting#> .\n\n"
				+ "project: tool:merge_shadow_extensions \"true\" ;\n"
				+ "        tool:self_heal_on_import \"true\" .\n\n"
				+ "<http://cimtoole.langdale.com.au/2009/project/Schema/" + schemaFile + ">\n"
				+ "        tool:schema_namespace \"" + namespace + "\" .\n";
	}

	private static Path extract(String zip, String name, Path folder) throws IOException {
		Path archivePath = TestProjects.repositoryRoot().resolve(zip);
		assumeTrue(Files.isRegularFile(archivePath), "test files not found: " + zip);
		try (ZipFile archive = new ZipFile(archivePath.toFile())) {
			ZipEntry entry = archive.getEntry(name);
			assumeTrue(entry != null, name + " not found in " + zip);
			try (InputStream in = archive.getInputStream(entry)) {
				Path target = folder.resolve(name);
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
				return target;
			}
		}
	}

	private static OntModel parse(Path project, Path schema) throws Exception {
		CLISettings settings = new CLISettings(project.toFile());
		return new CLISchemaParser(settings, project.toFile()).parse(schema.toFile());
	}

	private static boolean defines(OntModel model, String uri) {
		return model.getGraph().contains(Node.createURI(uri), Node.ANY, Node.ANY);
	}

	@Test
	void annotationAppliedToXMISchema(@TempDir Path project) throws Exception {
		Path schemaFolder = Files.createDirectories(project.resolve("Schema"));
		Path schema = extract(XMI_ZIP, XMI_SCHEMA + ".xmi", schemaFolder);
		Files.writeString(schemaFolder.resolve(XMI_SCHEMA + ".annotation"), annotation("Wires"), StandardCharsets.UTF_8);
		Files.writeString(project.resolve(CLISettings.SETTINGS_FILENAME), settings(XMI_SCHEMA + ".xmi", XMI_NS),
				StandardCharsets.UTF_8);

		OntModel model = parse(project, schema);

		assertTrue(defines(model, ANNOTATED_NS + "ACLineSegment"), "class of the annotated package");
		assertFalse(defines(model, XMI_NS + "ACLineSegment"), "class of the annotated package in the schema namespace");
		assertTrue(defines(model, XMI_NS + "IdentifiedObject"), "class of another package");
	}

	@Test
	void annotationBesideEAProjectIgnored(@TempDir Path project) throws Exception {
		Path schemaFolder = Files.createDirectories(project.resolve("Schema"));
		String base = TestProjects.CGMES_SCHEMA;
		Path schema = extract(CGMES_ZIP, base + ".qea", schemaFolder);
		Files.writeString(schemaFolder.resolve(base + ".annotation"), annotation("Core"), StandardCharsets.UTF_8);
		Files.writeString(project.resolve(CLISettings.SETTINGS_FILENAME), settings(base + ".qea", TestProjects.CIM100),
				StandardCharsets.UTF_8);

		OntModel model = parse(project, schema);

		assertTrue(defines(model, TestProjects.CIM100 + "IdentifiedObject"), "class of the package named in the annotation");
		assertFalse(defines(model, ANNOTATED_NS + "IdentifiedObject"), "annotation applied");
	}
}
