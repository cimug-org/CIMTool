/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.ui.util.IconCache;



/**
 * The activator class for the CIMTool plugin.
 * It manages the lifetime of the model cache.
 */
public class CIMToolPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "au.com.langdale.cimtoole";
	
	// The shared instance
	private static CIMToolPlugin plugin;
	
	// the model cache
	private static Cache cache;
	

	/**
	 * The constructor
	 */
	public CIMToolPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		IconCache.setSource(context.getBundle(), "/icons/");
		plugin = this;
		cache = new Cache();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CIMToolPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static Cache getCache() {
		return cache;
	}
}
