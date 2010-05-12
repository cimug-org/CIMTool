package au.com.langdale.util;

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
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

public class Jobs {
	
	/**
	 * Run this operation in the background using the Job system.
	 */
	public static void runJob(final IWorkspaceRunnable oper, final ISchedulingRule rule, String comment) {
		Job job = new Job(comment) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					oper.run(monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(rule);
		job.schedule();
	}
	
	/**
	 * Run this operation in the foreground.
	 */
	public static void runWait(final IWorkspaceRunnable oper, final ISchedulingRule rule) {
		try {
			ResourcesPlugin.getWorkspace().run(oper, rule, 0, null);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Run this operation interactively (with progress bar provided by container).
	 */
	public static boolean runInteractive(final IWorkspaceRunnable oper, final ISchedulingRule rule, IRunnableContext container, Shell shell) {
		boolean result;
		
		IRunnableWithProgress wrapped = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ResourcesPlugin.getWorkspace().run(oper, rule, 0, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		
		try {
			container.run(true, false, wrapped);
			result  = true;
		} catch (InterruptedException e) {
			result = false;
		} catch (InvocationTargetException e) {
			if( shell != null) {
				Throwable realException = e.getTargetException();
				MessageDialog.openError(shell, "Error", realException.getMessage());
			}
			result = false;
		}
		return result;
	}
}
