package au.com.langdale.ui;

/**
 * An interface that tree nodes may implement to 
 * gain more control over rendering.
 */
public interface NodeTraits {
	/**
	 * 
	 * Should the node be rendered with an error indicator?
	 */
	public boolean getErrorIndicator();
	
	/**
	 * A marker class that will determine the icon used.
	 */
	public Class getIconClass();
}