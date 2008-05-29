/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import au.com.langdale.cimtoole.project.Info;

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
		IRunnableWithProgress wrapped = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ResourcesPlugin.getWorkspace().run(op, rule, 0, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		
		try {
			getContainer().run(true, false, wrapped);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		
		return true;
	}
	
	/**
	 * Run this operation in the background using the Job system and
	 * let the wizard complete asynchronously.
	 */
	public boolean runJob(final IWorkspaceRunnable op, final ISchedulingRule rule, String commentary) {
		Job job = new Job(commentary) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.run(monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
			
			@Override
			public boolean belongsTo(Object family) {
				return Info.WIZARD_JOBS.equals(family);
			}
			
		};
		job.setRule(rule);
		job.schedule();
		return true;
	}	

}
