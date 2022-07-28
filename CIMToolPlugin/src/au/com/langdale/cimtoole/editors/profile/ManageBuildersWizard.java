/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.builder.ProfileBuildlets;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.registries.ProfileBuildletConfigUtils;
import au.com.langdale.cimtoole.registries.ProfileBuildletRegistryManager;
import au.com.langdale.util.Jobs;

public class ManageBuildersWizard extends Wizard {
	private ProfileEditor master;
	private ManageBuildersWizardPage page;

	public ManageBuildersWizard(ProfileEditor master) {
		this.master = master;
		setNeedsProgressMonitor(true);
	}

	public void run() {
		Shell shell = master.getSite().getWorkbenchWindow().getShell();
		WizardDialog dialog = new WizardDialog(shell, this);
		dialog.create();
		dialog.open();
	}

	@Override
	public void addPages() {
		setWindowTitle("Mangage XSLT Transform Builders");
		page = new ManageBuildersWizardPage();
		addPage(page);
	}

	public static IWorkspaceRunnable updateTransformBuilder(final TransformBuildlet buildlet) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				ProfileBuildletConfigUtils.updateTransformBuilderConfigEntry(buildlet);
				/**
				 * Given that we've imported a new buildlet, we need to reset the "cached"
				 * available profile buildlets.
				 */
				ProfileBuildlets.reload();
			}
		};
	}

	public static IWorkspaceRunnable deleteTransformBuilder(final String buildletKey) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				ProfileBuildletConfigUtils.deleteTransformBuilderConfigEntry(buildletKey);
				/**
				 * Given that we've imported a new buildlet, we need to reset the "cached"
				 * available profile buildlets.
				 */
				ProfileBuildlets.reload();
			}
		};
	}

	@Override
	public boolean performFinish() {

		switch (page.getSelectedAction()) {
		case DELETE:
			Jobs.runInteractive(deleteTransformBuilder(page.getTranformBuildlet().getStyle()), null, getContainer(),
					getShell());
			ProfileBuildletRegistryManager.fireBuildersChanged();
			break;
		case UPDATE:
			Jobs.runInteractive(updateTransformBuilder(page.getTranformBuildlet()), null, getContainer(), getShell());
			ProfileBuildletRegistryManager.fireBuildersChanged();
			break;
		}

		return true;
	}

}
