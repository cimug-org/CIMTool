/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.reporting.ReportGenerationSettings;
import au.com.langdale.util.Jobs;

public class ImportSchema extends Wizard implements IImportWizard {

	private SchemaWizardPage main = new SchemaWizardPage();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Schema");
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import an additional schema or replace an existing schema.");
		main.setSelected(selection);
	}

	@Override
	public void addPages() {
		addPage(main);
	}

	@Override
	public boolean performFinish() {
		String pathname = main.getPathname();
		if (pathname != null && pathname.length() != 0) {
			IWorkspaceRunnable job = Task.importSchema( //
					main.getFile(), //
					main.getPathname(), //
					main.getNamespace(), //
					main.isMergeShadowExtensionsEnabled(), //
					main.isSelfHealingOnImportEnabled());
			ReportGenerationSettings reportGenerationSettings = main.getReportGenerationSettings();
			if (reportGenerationSettings.shouldGenerateReport()) {
				IProject project = main.getFile().getProject();
				IFolder schemaFolder = Info.getSchemaFolder(project);
				try {
					schemaFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				} catch (CoreException e) {
				}
				IFolder importReportFolder = Info.getSchemaImportReportFolder(project);
				if (!Info.doesFolderExistOnDiskEFS(importReportFolder)) {
					job = Task.chain(job, Task.createImportReportDependencies(project));
				}
				job = Task.chain(job, Task.generateImportSchemaReport(main.getFile(), reportGenerationSettings));
			}
			return Jobs.runInteractive(job, ResourcesPlugin.getWorkspace().getRoot(), getContainer(), getShell());
		}
		return true;
	}

}
