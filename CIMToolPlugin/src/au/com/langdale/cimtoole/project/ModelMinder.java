/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.PlatformUI;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Cache.CacheListener;
import au.com.langdale.kena.OntModel;
/**
 * An adjunct to the model cache to manage cache notifications 
 * for a user interface component.
 */
public class ModelMinder implements CacheListener {
	private List<IResource> keys = new ArrayList<IResource>();
	private CacheListener delegate;
	
	/**
	 * Initialise with the user interface component, which
	 * implements the cache listener interface as usual. 
	 * @param delegate
	 */
	public ModelMinder( CacheListener delegate) {
		this.delegate = delegate;
		CIMToolPlugin.getCache().addCacheListener(this);
	}
	/**
	 * Get a cached model and subscribe for updates.
	 * @param file: the model file.
	 * @return: the model or null if not yet cached.
	 */
	public OntModel getOntology( IFile file ) {
		add(file);
		return CIMToolPlugin.getCache().getOntology(file);
	}
	/**
	 * Get a cached union model and subscribe for updates.
	 * @param folder: the folder defining the union
	 * @return: the model or null if not yest cached.
	 */
	public OntModel getProjectOntology( IFolder folder ) {
		add(folder);
		return CIMToolPlugin.getCache().getMergedOntology(folder);
	}
	/**
	 * Subscribe for cache notifications affecting a specific resource
	 * @param key: the resource
	 */
	public void add(IResource key) {
		if( ! keys.contains(key))
			keys.add(key);
	}
	/**
	 * Remove all subscriptions. This instance should no longer be used.
	 */
	public void dispose() {
		CIMToolPlugin.getCache().removeCacheListener(this);
	}
	/**
	 * Notifications from the <code>Cache</code> are filtered and 
	 * regenerated on the UI thread.
	 */
	public void modelCached(final IResource key) {
		if( keys.contains(key))
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					delegate.modelCached(key);
				}
			});
	}
	/**
	 * Notifications from the <code>Cache</code> are filtered and 
	 * regenerated on the UI thread.
	 */
	public void modelDropped(final IResource key) {
		if( keys.contains(key))
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					delegate.modelDropped(key);
				}
			});
	}
}