package au.com.langdale.ui.builder;

/**
 * An abstraction of the IHyperlinkListener and SelectionListener
 * that reports clicks and the name of the button or hyperlink that
 * was clicked.
 *
 */
public interface ButtonObserver {
	public void entered(String name);
	public void exited(String name);
	public void clicked(String name);
}
