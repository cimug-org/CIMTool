package au.com.langdale.cimtoole.editors;

import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Makes the plain Text Editor the default editor for PlantUML ({@code *.puml})
 * files (issue #405).
 *
 * <p>
 * CIMTool's PlantUML builders generate these files, so they are read rather
 * than edited, and the PlantUML Svg view draws them with the same PlantUML
 * library as the Profile Real-Time Preview. Without this class they open in the
 * Asciidoctor Editor's PlantUML editor, the only editor registered for
 * {@code puml}, which draws the diagram a second time through the plugin's ASP
 * server: a separate Java process that renders with an older PlantUML release
 * and whose port check raises a Windows Firewall prompt on a new installation.
 * </p>
 *
 * <p>
 * Eclipse does not persist a default set through
 * {@link IEditorRegistry#setDefaultEditor(String, String)}, so this runs at
 * every startup, and only while the Asciidoctor Editor's PlantUML editor is the
 * default. A user's choice for a particular file through Open With is kept by
 * Eclipse and is not affected. The registry's internal
 * {@code saveAssociations()} is deliberately not used, so that CIMTool depends
 * only on public Eclipse API.
 * </p>
 */
public class PlantUmlEditorDefaultStartup implements IStartup {

	private static final String PUML_FILES = "*.puml";
	private static final String SAMPLE_PUML_FILE = "default.puml";
	private static final String ASCIIDOCTOR_PUML_EDITOR = "asciidoctoreditor.editors.AsciiDoctorPlantUMLEditor";
	private static final String TEXT_EDITOR = "org.eclipse.ui.DefaultTextEditor";

	@Override
	public void earlyStartup() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(() -> {
			if (workbench.isClosing())
				return;
			IEditorRegistry registry = workbench.getEditorRegistry();
			IEditorDescriptor current = registry.getDefaultEditor(SAMPLE_PUML_FILE);
			if (current != null && ASCIIDOCTOR_PUML_EDITOR.equals(current.getId())
					&& registry.findEditor(TEXT_EDITOR) != null)
				registry.setDefaultEditor(PUML_FILES, TEXT_EDITOR);
		});
	}
}
