/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.WizardProjectsImportPage;

import au.com.langdale.cimtoole.CIMNature;
import au.com.langdale.cimtoole.builder.UpgradeRegeneration;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

/**
 * A CIMTool-specific "Import Existing Projects into Workspace" wizard that
 * suppresses the ".settings overwrite" confirmation dialog by automatically
 * answering "Yes To All".
 *
 * <p>
 * The dialog is triggered by Eclipse when importing a project whose source
 * directory contains a {@code .settings} folder and "Copy projects into
 * workspace" is selected. For CIMTool projects this is always safe to overwrite
 * as the folder contains preference overrides that should travel with the
 * project.
 *
 * <pre>
 * This wizard appears in <em>File -> Import -> CIMTool -> Import CIMTool Projects
 * </pre>
 */
@SuppressWarnings("restriction")
public class ImportCIMToolProjects extends Wizard implements IImportWizard {

	private WizardProjectsImportPage mainPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import CIMTool Projects");
		setNeedsProgressMonitor(true);

		// Instantiate the standard import page but override queryOverwrite
		// so it always returns ALL — the programmatic equivalent of clicking
		// "Yes To All" on the .settings overwrite confirmation dialog.
		mainPage = new WizardProjectsImportPage() {
			@Override
			public String queryOverwrite(String pathString) {
				return IOverwriteQuery.ALL;
			}
		};
		mainPage.setTitle("Import CIMTool Projects");
		mainPage.setDescription("Select a directory to search for existing CIMTool projects.");
	}

	@Override
	public void addPages() {
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Set<IProject> existing = new HashSet<>(Arrays.asList(root.getProjects()));
		boolean created = mainPage.createProjects();
		if (created) {
			List<IProject> imported = new ArrayList<>();
			for (IProject project : root.getProjects()) {
				if (!existing.contains(project)) {
					ensureProfilesAsciiDoctorConfig(project);
					imported.add(project);
				}
			}
			// Regenerates the imported projects' artifacts with this release's builders,
			// whether or not Build Automatically is on (issue #310).
			UpgradeRegeneration.schedule(imported);
		}
		return created;
	}

	/**
	 * Upgrades an imported CIMTool project by ensuring its Profiles folder carries
	 * the Asciidoctor editor configuration that defines profilesdir (issue #291).
	 * A failure is not fatal, since the AsciiDoc buildlet ensures the same file
	 * the next time AsciiDoc artifacts are built.
	 */
	private void ensureProfilesAsciiDoctorConfig(IProject project) {
		try {
			if (project.isOpen() && project.hasNature(CIMNature.NATURE_ID))
				Jobs.runJob(Task.ensureProfilesAsciiDoctorConfigFile(project), project,
						"Configuring AsciiDoc profilesdir for " + project.getName());
		} catch (CoreException e) {
			// Intentionally ignored: see method comment.
		}
	}
}
