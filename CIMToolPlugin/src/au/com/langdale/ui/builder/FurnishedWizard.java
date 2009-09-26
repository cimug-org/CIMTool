/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.wizard.Wizard;

import au.com.langdale.util.Jobs;

/**
 * Wizard provided with utilities for performing the finish operation.
 */
public abstract class FurnishedWizard extends Wizard {

	/**
	 * Run this operation in the context of the wizard UI 
	 * which provides visual indication and in the context of 
	 * the Workspace, which provides resource notification and 
	 * synchronisation.   
	 */
	public boolean run(final IWorkspaceRunnable op, final ISchedulingRule rule) {
		return Jobs.runInteractive(op, rule, getContainer(), getShell());
	}
	
	/**
	 * Run this operation in the background using the Job system and
	 * let the wizard complete asynchronously.
	 */
	public boolean runJob(final IWorkspaceRunnable op, final ISchedulingRule rule, String commentary) {
		Jobs.runJob(op, rule, commentary);
		return true;
	}	

}
