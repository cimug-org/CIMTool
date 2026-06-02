/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.registries.ProfileBuildletRegistryManager;
import au.com.langdale.util.Jobs;

public class ImportTransformBuilder extends Wizard implements IImportWizard {

	private ImportTransformBuilderPage main = new ImportTransformBuilderPage();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import XSLT Transform Builder");
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import an XSLT transform builder (.xsl file).");
	}

	@Override
	public void addPages() {
		addPage(main);
	}

	@Override
	public boolean performFinish() {
		boolean successful = Jobs.runInteractive(
				Task.importTransformBuilder(main.getTranformBuildlet(), main.getXslFile()), null, getContainer(),
				getShell());
		ProfileBuildletRegistryManager.fireBuildersChanged();
		return successful;
	}

}
