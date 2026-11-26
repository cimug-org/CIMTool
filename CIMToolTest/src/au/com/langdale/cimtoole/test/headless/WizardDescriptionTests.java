/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Every import, new and export wizard that CIMTool contributes carries a
 * description, which Eclipse shows in the header of the wizard selection
 * dialog when the wizard is selected.
 */
public class WizardDescriptionTests extends TestCase {

	private static final String CIMTOOL = "au.com.langdale.cimtoole";
	private static final String CIMPHONY = "com.cimphony.cimtoole";

	public void testImportWizardsHaveDescriptions() {
		assertDescriptions("org.eclipse.ui.importWizards", 12);
	}

	public void testNewWizardsHaveDescriptions() {
		assertDescriptions("org.eclipse.ui.newWizards", 7);
	}

	public void testExportWizardsHaveDescriptions() {
		assertDescriptions("org.eclipse.ui.exportWizards", 1);
	}

	/**
	 * Checks the wizards that CIMTool's plug-ins contribute to the given
	 * extension point. The count for the main plug-in guards against the test
	 * passing because no wizards were found at all.
	 */
	private void assertDescriptions(String point, int expectedFromCIMTool) {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(point);
		int fromCIMTool = 0;
		List<String> missing = new ArrayList<String>();
		for (IConfigurationElement element : elements) {
			if (!"wizard".equals(element.getName()))
				continue;
			String contributor = element.getContributor().getName();
			if (!CIMTOOL.equals(contributor) && !CIMPHONY.equals(contributor))
				continue;
			if (CIMTOOL.equals(contributor))
				fromCIMTool++;
			if (!hasDescription(element))
				missing.add(element.getAttribute("name") + " (" + contributor + ")");
		}
		assertEquals("CIMTool wizards contributed to " + point, expectedFromCIMTool, fromCIMTool);
		assertTrue("Wizards without a description in " + point + ": " + missing, missing.isEmpty());
	}

	private static boolean hasDescription(IConfigurationElement wizard) {
		for (IConfigurationElement child : wizard.getChildren("description")) {
			String text = child.getValue();
			if (text != null && text.trim().length() > 0)
				return true;
		}
		return false;
	}
}
