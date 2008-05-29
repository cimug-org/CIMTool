/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.util;

import java.io.InputStream;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import au.com.langdale.ui.NodeTraits;
/**
 * Select an image to represent an object, instantiate it as an <code>Image</code> 
 * and cache it.
 */
public class IconCache {
	private static ImageRegistry registry;
	private static Display display;
	private static String prefix = "/au/com/langdale/ui/icons/";
	
	public static void setResourcePrefix(String prefixPath) {
		prefix = prefixPath;
	}

	/**
	 * Get an icon of the given type.
	 * @param type the type of icon as a string.
	 * @return
	 */
	public static Image get(String type) {
		if( registry == null ) {
			display = PlatformUI.getWorkbench().getDisplay();
			registry = new ImageRegistry(display);
		}
		
		Image image = registry.get(type);
		if( image != null )
			return image;
		
		InputStream stream = IconCache.class.getResourceAsStream(prefix + type + ".png");
		if( stream != null ) {
			image = new Image(display, stream);
			//System.out.println("Image from path: " + path + " size: " + image.getBounds());
		}
		else {
			image = new Image(display, 32,32);
			//System.out.println("Creating placemark image: " + path + " image: " + image);
		}
		registry.put(type, image);
		return image;
	}
	
	/**
	 * Get an icon for a given class of TreeNode.
	 * 
	 * The simple classname is used.  If 
	 * the classname ends in "Node" this is stripped.
	 * 
	 * @param clss the class of TreeNode.
	 * @return
	 */
	public static Image get(Class clss) {
		String name = clss.getName();
		return get( name.replaceFirst("^.+[.$]", "").replaceFirst("Node$", "").toLowerCase());
	}
	
	
	/**
	 * Get an icon for the given object. 
	 * 
	 * By default The objects class determines
	 * the icon.  If the object is assignable to
	 * NodeTraits then the getIconClass() method
	 * determines the icon.
	 * 
	 * @param value
	 * @return
	 */
	public static Image get(Object value) {
		if( value instanceof NodeTraits ) {
			NodeTraits node = (NodeTraits)value;
			return get(node.getIconClass());
		}
		else if( value != null ){
			return get(value.getClass());
		}
		else {
			return get("unknown");
		}
	}
}
