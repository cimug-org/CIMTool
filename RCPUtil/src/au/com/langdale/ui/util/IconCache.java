/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.util;

import org.osgi.framework.Bundle;

/**
 * Select an image to represent an object, instantiate it as an
 * <code>Image</code> and cache it.
 */
public class IconCache {

    private static GeneralIconCache icons;
    
    public static GeneralIconCache getIcons() {
	if( icons == null )
	    icons = new GeneralIconCache( null, "/au/com/langdale/ui/icons/" );
	return icons;
    }
    
    public static void setSource(Bundle bundleToUse, String prefixPath) {
	icons = new GeneralIconCache(bundleToUse, prefixPath);
    }
    
    public static void setIcons(GeneralIconCache cache) {
	icons = cache;
    }
}
