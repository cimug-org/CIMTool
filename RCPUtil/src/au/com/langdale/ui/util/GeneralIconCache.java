package au.com.langdale.ui.util;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

public class GeneralIconCache {
    public final int DEFAULT_IMAGE_SIZE = 16;
    private ImageRegistry registry;
    private Display display;
    private String prefix = "/au/com/langdale/ui/icons/";
    private Bundle bundle;

    public GeneralIconCache(Bundle bundleToUse, String prefixPath) {
	bundle = bundleToUse;
	prefix = prefixPath;
    }

    private InputStream getData(IPath path) {
	try {
	    return FileLocator.openStream(bundle, path, false);
	} catch (IOException e) {
	    return null;
	}
    }

    private InputStream getData(String path, int size) {
	String scode = size == DEFAULT_IMAGE_SIZE ? "" : "-" + Integer.toString(size);
	if (bundle != null)
	    return getData(new Path(prefix + path + scode + ".png")); // normal
								      // source
								      // here
	else
	    return IconCache.class.getResourceAsStream(prefix + path + scode + ".png");
    }

    private Image readImage(String type, int size) {
	InputStream stream = getData(type, size);
	if (stream != null) {
	    try {
		return new Image(display, stream);
		// System.out.println("Image from path: " + path + " size: " +
		// image.getBounds());
	    } finally {
		try {
		    stream.close();
		} catch (IOException e) {
		    throw new RuntimeException(e);
		}
	    }
	} else {
	    return null;
	}

    }

    /**
     * Get an icon of the given type.
     * 
     * @param type
     *            the type of icon as a string.
     * @param error
     * @return
     */
    public Image get(String type, boolean error, int size) {
	String key = keyString(type, error, size);
	Image image;

	if (registry == null) {
	    display = PlatformUI.getWorkbench().getDisplay();
	    registry = new ImageRegistry(display);
	    image = null;
	} else
	    image = registry.get(key);

	if (image == null) {
	    if (error) {
		Image base = get(type, false, size);
		ImageDescriptor overlay = ImageDescriptor.createFromImage(get(
			"error_tsk", false, size));
		image = new DecorationOverlayIcon(base, overlay,
			IDecoration.BOTTOM_LEFT).createImage();
	    } else {
		image = readImage(type, size);
		if (image == null)
		    image = new Image(display, size, size);
	    }

	    registry.put(key, image);
	}

	return image;
    }

    private String keyString(String type, boolean error, int size) {
	return type + (error ? "-error" : "") + "-" + Integer.toString(size);
    }

    /**
     * Get an ImageDescriptor that will refer to the icon of the given type.
     */
    public ImageDescriptor getDescriptor(String type, boolean error,
	    int size) {
	get(type, error, size); // side effect places image descriptor in
				// registry
	return registry.getDescriptor(keyString(type, error, size));
    }

    /**
     * Get an icon for a given class of TreeNode.
     * 
     * The simple classname is used. If the classname ends in "Node" this is
     * stripped.
     * 
     * @param clss
     *            the class of TreeNode.
     * @param error
     * @return
     */
    public Image get(Class clss, boolean error, int size) {
	return get(getName(clss), error, size);
    }

    public String getName(Class clss) {
	return clss.getName().replaceFirst("^.+[.$]", "").replaceFirst("Node$",
		"").toLowerCase();
    }

    /**
     * Get an icon for the given object.
     * 
     * By default The objects class determines the icon. If the object is
     * assignable to NodeTraits then the getIconClass() method determines the
     * icon.
     * 
     * @param value
     * @return
     */
    public Image get(Object value) {
	return get(value, DEFAULT_IMAGE_SIZE);
    }

    public Image get(Object value, int size) {
        if (value instanceof String) {
	    return get((String) value, false, size);
	} else if (value instanceof Class) {
	    return get((Class) value, false, size);
	} else if (value != null) {
	    return get(value.getClass(), false, size);
	} else {
	    return get("unknown", false, size);
	}
    }
}
