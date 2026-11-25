/*
 * Copyright (c) 2026 the CIMTool contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package au.com.langdale.cimtoole.test.headless;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.test.WorkspaceTest;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

/**
 * The project selector shared by the CIMTool wizards lists, preselects and
 * accepts open CIMTool projects only: projects with the CIMTool nature, or older
 * projects with a {@code .cimtool-settings} file. Other projects, such as a
 * plain Java project, are not valid targets.
 */
public class ProjectBindingTests extends WorkspaceTest {

	private static final String MESSAGE = "A CIMTool project must be selected.";

	private IProject legacy;
	private IProject plain;
	private ProjectBinding binding;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workspace.run(Task.createProject(project, null), monitor);
		legacy = createPlainProject("LegacyCIMToolProject");
		createFile(Info.getSettings(legacy));
		plain = createPlainProject("PlainProject");
		binding = new ProjectBinding();
	}

	@Override
	protected void tearDown() throws Exception {
		// A closed project's folders stay on disk when it is deleted, which would
		// make the next test's setUp fail to recreate it, so reopen it first.
		if (project.exists() && !project.isOpen())
			project.open(monitor);
		for (IProject p : new IProject[] { legacy, plain })
			if (p != null && p.exists())
				p.delete(true, monitor);
		super.tearDown();
	}

	private IProject createPlainProject(String name) throws CoreException {
		IProject p = workspace.getRoot().getProject(name);
		if (p.exists())
			p.delete(true, monitor);
		p.create(monitor);
		p.open(monitor);
		return p;
	}

	private void createFile(IFile file) throws CoreException {
		file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
	}

	public final void testAllowsProjectWithCIMToolNature() {
		assertTrue(binding.allow(project));
	}

	public final void testAllowsLegacyProjectWithSettingsFile() {
		assertTrue(binding.allow(legacy));
	}

	public final void testRejectsPlainProject() {
		assertFalse(binding.allow(plain));
	}

	public final void testRejectsClosedCIMToolProject() throws CoreException {
		project.close(monitor);
		assertFalse(binding.allow(project));
	}

	public final void testRejectsResourcesOtherThanProjects() {
		assertFalse("folder", binding.allow(Info.getSchemaFolder(project)));
		assertFalse("null", binding.allow(null));
	}

	public final void testSelectionInCIMToolProjectSelectsIt() throws CoreException {
		IFile file = Info.getSchemaFolder(project).getFile("a.txt");
		createFile(file);
		binding.setSelected(new StructuredSelection(file));
		assertEquals(project, binding.getProject());
	}

	public final void testSelectionInPlainProjectSelectsNothing() throws CoreException {
		IFile file = plain.getFile("a.txt");
		createFile(file);
		binding.setSelected(new StructuredSelection(file));
		assertNull(binding.getProject());
	}

	public final void testValidate() {
		binding.setValue(plain);
		assertEquals("plain project", MESSAGE, binding.validate());
		binding.setValue(null);
		assertEquals("no project", MESSAGE, binding.validate());
		binding.setValue(project);
		assertNull("CIMTool project", binding.validate());
	}
}
