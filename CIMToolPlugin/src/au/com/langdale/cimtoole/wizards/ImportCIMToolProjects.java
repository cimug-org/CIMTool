/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.wizards.datatransfer.WizardProjectsImportPage;

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
		return mainPage.createProjects();
	}
}
