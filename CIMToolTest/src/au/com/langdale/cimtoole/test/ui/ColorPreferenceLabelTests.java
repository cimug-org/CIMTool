/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.preferences.PlantUMLBuildersPreferencesPage;
import au.com.langdale.cimtoole.project.Info;
import junit.framework.TestCase;

/**
 * The label beside each color button on the PlantUML Builder Preferences page
 * shows the color's hexadecimal value, and keeps showing it when the page itself
 * changes the color, as Restore Defaults does. The spacing captions
 * are separate labels and are never replaced by a value.
 */
public class ColorPreferenceLabelTests extends TestCase {

	private static final String CUSTOM = "#123456";

	/**
	 * Exposes Restore Defaults, which the preference dialog normally triggers.
	 */
	public static class TestPage extends PlantUMLBuildersPreferencesPage {
		public void restoreDefaults() {
			performDefaults();
		}
	}

	private IPreferenceStore store;
	private Shell shell;
	private TestPage page;

	@Override
	protected void setUp() throws Exception {
		store = CIMToolPlugin.getDefault().getPreferenceStore();
		store.setValue(Info.REFS_COLOR.getLocalName(), CUSTOM);
		shell = new Shell(Display.getDefault());
		page = new TestPage();
		page.createControl(shell);
	}

	@Override
	protected void tearDown() throws Exception {
		page.dispose();
		shell.dispose();
		store.setToDefault(Info.REFS_COLOR.getLocalName());
	}

	public final void testLabelShowsStoredColor() {
		assertTrue("stored color shown: " + labels(), labels().contains(CUSTOM));
	}

	public final void testRestoreDefaultsUpdatesLabel() {
		String defaultColor = store.getDefaultString(Info.REFS_COLOR.getLocalName()).toUpperCase();

		page.restoreDefaults();

		List<String> labels = labels();
		assertTrue("default color shown: " + labels, labels.contains(defaultColor));
		assertFalse("previous color no longer shown: " + labels, labels.contains(CUSTOM));
	}

	public final void testSpacingCaptionsKeptAfterRestoreDefaults() {
		page.restoreDefaults();

		List<String> labels = labels();
		assertTrue("horizontal caption kept: " + labels, labels.contains("Horizontal spacing:"));
		assertTrue("vertical caption kept: " + labels, labels.contains("Vertical spacing:"));
	}

	/**
	 * The text of every label on the page, upper-cased where it is a color.
	 */
	private List<String> labels() {
		List<String> texts = new ArrayList<String>();
		collect(shell, texts);
		return texts;
	}

	private void collect(Control control, List<String> texts) {
		if (control instanceof Label) {
			String text = ((Label) control).getText();
			texts.add(text.startsWith("#") ? text.toUpperCase() : text);
		}
		if (control instanceof Composite)
			for (Control child : ((Composite) control).getChildren())
				collect(child, texts);
	}
}
