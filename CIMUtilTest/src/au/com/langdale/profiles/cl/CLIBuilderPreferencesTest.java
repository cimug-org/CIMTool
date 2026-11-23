/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The order in which cimtool-cli resolves a builder preference (issue #411),
 * matching the workbench: a file's own value, then its project's value, both
 * from {@code .builder-preferences}, then the workspace value exported to
 * {@code .cimtool-global-preferences}, then the shipped default.
 *
 * <p>
 * The project in each case holds a file value for the theme, a project value for
 * the theme and the horizontal spacing, and workspace values for both spacings.
 * The files are written in the form the workbench produces, with the URIs
 * spelled out.
 * </p>
 */
class CLIBuilderPreferencesTest {

	private static final String DIAGRAM = "Profiles/Test.rdfs-t2b.puml";

	private static final String BUILDER_PREFERENCES = "" //
			+ "@prefix prefs:   <http://cimtoole.langdale.com.au/2009/builder-prefs#> .\n"
			+ "@prefix project: <http://cimtoole.langdale.com.au/2009/project/> .\n" //
			+ "\n" //
			+ "<http://cimtoole.langdale.com.au/2009/project/>\n" //
			+ "      prefs:plantuml_theme \"cerulean\" ;\n"
			+ "      prefs:plantuml_nodesep_horizontal_spacing \"13\" .\n" //
			+ "\n" //
			+ "<http://cimtoole.langdale.com.au/2009/project/Profiles/Test.rdfs-t2b.puml>\n"
			+ "      prefs:plantuml_theme \"sketchy\" .\n";

	private static final String GLOBAL_PREFERENCES = "" //
			+ "@prefix prefs:   <http://cimtoole.langdale.com.au/2009/builder-prefs#> .\n" //
			+ "\n" //
			+ "<http://cimtoole.langdale.com.au/2009/global-prefs#>\n"
			+ "      prefs:plantuml_nodesep_horizontal_spacing \"42\" ;\n"
			+ "      prefs:plantuml_ranksep_vertical_spacing \"24\" .\n";

	@TempDir
	Path project;

	private CLIBuilderPreferences preferences;

	@BeforeEach
	void createProject() throws IOException {
		Files.writeString(project.resolve(".builder-preferences"), BUILDER_PREFERENCES);
		Files.writeString(project.resolve(".cimtool-global-preferences"), GLOBAL_PREFERENCES);
		Files.createDirectories(project.resolve("Profiles"));
		Files.writeString(project.resolve(DIAGRAM), "");
		preferences = new CLIBuilderPreferences(project.toFile());
	}

	@Test
	void fileOwnValueWins() {
		assertEquals("sketchy", preferences.getPreference(DIAGRAM, "plantuml_theme"));
	}

	@Test
	void fileInheritsProjectValue() {
		assertEquals("13", preferences.getPreference(DIAGRAM, "plantuml_nodesep_horizontal_spacing"));
	}

	@Test
	void fileInheritsWorkspaceValueWhenProjectHasNone() {
		assertEquals("24", preferences.getPreference(DIAGRAM, "plantuml_ranksep_vertical_spacing"));
	}

	@Test
	void shippedDefaultComesLast() {
		assertEquals(CLIGlobalPreferenceDefaults.getDefault("plantuml_errors_color"),
				preferences.getPreference(DIAGRAM, "plantuml_errors_color"));
	}

	@Test
	void projectResolvesOwnValueThenWorkspace() {
		assertEquals("cerulean", preferences.getPreference("", "plantuml_theme"), "project's own value");
		assertEquals("24", preferences.getPreference("", "plantuml_ranksep_vertical_spacing"), "workspace value");
	}

	@Test
	void builderParametersUseProjectValue() {
		File diagram = project.resolve(DIAGRAM).toFile();

		String parameters = preferences.getBuilderParameters(diagram);

		assertTrue(parameters.contains("horizontalSpacing=13|"), "project value passed to the builders: " + parameters);
		assertTrue(parameters.contains("verticalSpacing=24|"), "workspace value passed to the builders: " + parameters);
		assertTrue(parameters.contains("plantUMLTheme=sketchy|"), "file value passed to the builders: " + parameters);
	}
}
