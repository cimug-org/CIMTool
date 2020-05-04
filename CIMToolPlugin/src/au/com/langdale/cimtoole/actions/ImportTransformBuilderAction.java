/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.actions;

import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.wizards.ImportTransformBuilder;
import au.com.langdale.ui.util.WizardLauncher;

public class ImportTransformBuilderAction extends WizardLauncher {

	@Override
	protected IWorkbenchWizard createWizard() {
		return new ImportTransformBuilder();
	}

}
