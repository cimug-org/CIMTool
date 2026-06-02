package au.com.langdale.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

public class ProjectUtils {

	public static IProject getCurrentSelectedProject() {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow workbenchWindow = (workbench != null ? workbench.getActiveWorkbenchWindow() : null);
			IWorkbenchPage workbenchPage = (workbenchWindow != null ? workbenchWindow.getActivePage() : null);
			IEditorPart editor = (workbenchPage != null ? workbenchPage.getActiveEditor() : null);
			if (editor != null) {
				IEditorInput input = editor.getEditorInput();
				if (input instanceof FileEditorInput) {
					IFile file = ((FileEditorInput) input).getFile();
					IProject currentProject = file.getProject();
					return currentProject;
				}
			}
		} catch (Exception e) {
			// Do nothing...allow to return null.
		}
		return null;
	}

}