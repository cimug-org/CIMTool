/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.workspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * An output stream that, on close(), sets contents of the given eclipse resource.
 * @author adv
 *
 */
public class ResourceOutputStream extends ByteArrayOutputStream {
	private IFile file;
	private IProgressMonitor monitor;
	private boolean prune, derived;
	
	public ResourceOutputStream(IFile file, IProgressMonitor monitor, boolean pruneEmptyFile, boolean derived) {
		this.file = file;
		this.monitor = monitor;
		this.prune = pruneEmptyFile;
		this.derived = derived;
	}

	private IWorkspaceRunnable save = new IWorkspaceRunnable() {
		public void run(IProgressMonitor monitor) throws CoreException {
			ByteArrayInputStream is = new ByteArrayInputStream(toByteArray());
			if( file.exists()) {
				if( size() > 0 || ! prune) {
					file.setContents(is, false, true, monitor);
					file.setDerived(derived);
				}
				else
					file.delete(false, true, monitor);
			}
			else {
				if( size() > 0 || ! prune) {
					file.create(is, false, monitor);
					file.setDerived(derived);
				}
			}
		}
	};
	
	@Override
	public void close() throws IOException {
		try {
			ISchedulingRule rule = file.exists()? file: file.getParent();
			ResourcesPlugin.getWorkspace().run(save, rule, 0, monitor);
		} catch (final CoreException e) {
			IOException i = new IOException();
			i.initCause(e);
			throw i;
		}
	}
}
