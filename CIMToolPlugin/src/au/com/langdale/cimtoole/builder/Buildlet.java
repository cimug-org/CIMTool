/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.builder;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 *	Represents a task to be performed as part of a build.
 */
public abstract class Buildlet {
	public static String NS = "http://langdale.com.au/2007/Buildlet#";

	/**
	 * Indicates which files would be built by this Buildlet if the
	 * given input file was changed.
	 * @param file: a changed file that might trigger building
	 * @return: a collection of files that would be built.
	 * @throws CoreException
	 */
	protected abstract Collection getOutputs(IResource file) throws CoreException;
	
	/**
	 * Execute a build task to create a given output.
	 * Implementations may create other outputs at the same time,
	 * in which case no action should be taken when build() is called for them.
	 * @param result: the output file to be built.
	 * @param monitor: for progress reporting
	 * @throws CoreException
	 */
	protected abstract void build(IFile result, IProgressMonitor monitor) throws CoreException;
	/**
	 * Remove the given build output.  The default implementation simply deletes the 
	 * resource.  
	 * @param result: the output of a build task.
	 * @param monitor: for reporting progress.
	 * @throws CoreException
	 */
	protected void clean(IFile result, IProgressMonitor monitor) throws CoreException {
		if( result.exists())
			result.delete(false, monitor);
	}
	/**
	 * Execute the build task or clean build outputs depending on a flag.
	 * @param result: the build output
	 * @param cleanup: true to clean, false to build.
	 * @param monitor: to report progress.
	 * @throws CoreException
	 */
	public void run(IFile result, boolean cleanup, IProgressMonitor monitor) throws CoreException {
		if( cleanup )
			clean( result, monitor );
		else
			build( result, monitor );				
	}
}