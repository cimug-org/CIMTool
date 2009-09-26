/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
/**
 * The main eclipse perspective for CIMTool.
 */
public class CIMToolBrowsingPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.addView("au.com.langdale.cimtoole.views.ProjectModelView", IPageLayout.RIGHT, .33f, layout.getEditorArea());
		layout.addView("org.eclipse.ui.navigator.ProjectExplorer", IPageLayout.LEFT, .0f, layout.getEditorArea());
		layout.addView("org.eclipse.ui.views.PropertySheet", IPageLayout.BOTTOM, .66f, "org.eclipse.ui.navigator.ProjectExplorer");
		layout.addView("au.com.langdale.cimtoole.views.Documentation", IPageLayout.BOTTOM, .66f, "au.com.langdale.cimtoole.views.ProjectModelView");

		layout.addShowViewShortcut("org.eclipse.ui.views.ProblemView");
		layout.addShowViewShortcut("org.eclipse.ui.views.PropertySheet");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.Documentation");
		layout.addShowViewShortcut("org.eclipse.ui.views.ContentOutline");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.ProjectModelView");
		layout.addShowViewShortcut("org.eclipse.ui.navigator.ProjectExplorer");
		
		layout.addPerspectiveShortcut("au.com.langdale.cimtoole.CIMToolPerspective");
		layout.setEditorAreaVisible(false);
	}

}
