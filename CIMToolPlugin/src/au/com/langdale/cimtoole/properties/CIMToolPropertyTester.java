/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.properties;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import au.com.langdale.cimtoole.project.Info;

/**
 * Property tester used by the <code>enabledWhen</code> expressions of the
 * CIMTool property page contributions in <code>plugin.xml</code>.
 * <p>
 * The resource classification rules live in {@link Info}, so this tester
 * delegates to it rather than repeating folder and extension rules in XML.
 * <ul>
 * <li><code>hasPlantUMLPreferences</code>: true for a CIMTool project, a
 * profile and a generated <code>.puml</code> file. These are the resources
 * that carry a PlantUML Builder Preferences child page.</li>
 * </ul>
 */
public class CIMToolPropertyTester extends PropertyTester {

	public static final String HAS_PLANTUML_PREFERENCES = "hasPlantUMLPreferences";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof IResource))
			return false;
		IResource resource = (IResource) receiver;
		if (HAS_PLANTUML_PREFERENCES.equals(property))
			return hasPlantUMLPreferences(resource);
		return false;
	}

	public static boolean hasPlantUMLPreferences(IResource resource) {
		if (resource instanceof IProject)
			return Info.isCIMToolProject((IProject) resource);
		return Info.isProfile(resource) || Info.isPlantUML(resource);
	}
}
