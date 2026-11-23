/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.profiles.cl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * cimtool-cli reads the {@code .cimtool-global-preferences} file that the
 * workbench writes into each CIMTool project.
 *
 * <p>
 * The workbench records the workspace preferences under the subject
 * {@code <http://cimtoole.langdale.com.au/2009/global-prefs#>}. The command line
 * once read them under a different subject, so it found nothing and silently
 * fell back to the shipped defaults. Each case writes the file in exactly the
 * form the workbench produces, with the URIs spelled out rather than taken from
 * the constants, so that a change to either side that breaks the format fails
 * here.
 * </p>
 */
class CLIGlobalPreferencesTest {

	private static final String WORKBENCH_EXPORT = "" //
			+ "@prefix prefs:   <http://cimtoole.langdale.com.au/2009/builder-prefs#> .\n"
			+ "@prefix project: <http://cimtoole.langdale.com.au/2009/project/> .\n" //
			+ "\n" //
			+ "<http://cimtoole.langdale.com.au/2009/global-prefs#>\n"
			+ "      prefs:plantuml_theme \"cerulean\" ;\n"
			+ "      prefs:plantuml_nodesep_horizontal_spacing \"42\" .\n";

	@TempDir
	Path project;

	@Test
	void readsWorkbenchExport() throws IOException {
		writeGlobalPreferences(WORKBENCH_EXPORT);

		CLIGlobalPreferences preferences = new CLIGlobalPreferences(project.toFile());

		assertTrue(preferences.isLoaded(), "file loaded");
		assertEquals("cerulean", preferences.getPreference("plantuml_theme"));
		assertEquals("42", preferences.getPreference("plantuml_nodesep_horizontal_spacing"));
	}

	@Test
	void preferenceNotInFileIsNull() throws IOException {
		writeGlobalPreferences(WORKBENCH_EXPORT);

		CLIGlobalPreferences preferences = new CLIGlobalPreferences(project.toFile());

		assertNull(preferences.getPreference("plantuml_errors_color"));
	}

	@Test
	void missingFileIsNotLoaded() throws IOException {
		CLIGlobalPreferences preferences = new CLIGlobalPreferences(project.toFile());

		assertFalse(preferences.isLoaded(), "no file to load");
		assertNull(preferences.getPreference("plantuml_theme"));
	}

	@Test
	void builderPreferencesUseWorkbenchExport() throws IOException {
		writeGlobalPreferences(WORKBENCH_EXPORT);

		CLIBuilderPreferences preferences = new CLIBuilderPreferences(project.toFile());

		assertEquals("cerulean", preferences.getPreference("Profiles/Test.rdfs-t2b.puml", "plantuml_theme"),
				"exported workspace value, not the shipped default");
		assertEquals(CLIGlobalPreferenceDefaults.getDefault("plantuml_errors_color"),
				preferences.getPreference("Profiles/Test.rdfs-t2b.puml", "plantuml_errors_color"),
				"shipped default for a preference the export does not hold");
	}

	private void writeGlobalPreferences(String content) throws IOException {
		Files.writeString(project.resolve(".cimtool-global-preferences"), content);
	}
}
