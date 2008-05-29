/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
/**
 * An additional perspective for CMTool that supports CIM/XML validation.
 */
public class ValidationPerspective implements IPerspectiveFactory {

	private static final String ID_SCHEMA_VIEW = "au.com.langdale.cimtoole.views.ProjectModelView";
	private static final String ID_VALID_VIEW = "au.com.langdale.cimtoole.views.ValidationView";

	public void createInitialLayout(IPageLayout layout) {
		
		layout.setFixed(true);
		layout.addView(ID_VALID_VIEW, IPageLayout.LEFT, .2f, IPageLayout.ID_EDITOR_AREA);
		layout.addView(ID_SCHEMA_VIEW, IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);

		layout.addView(IPageLayout.ID_PROBLEM_VIEW, IPageLayout.BOTTOM, 0.75f, ID_VALID_VIEW);
		layout.addView(IPageLayout.ID_PROGRESS_VIEW, IPageLayout.BOTTOM, 0.85f, IPageLayout.ID_EDITOR_AREA);

		layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, .66f, IPageLayout.ID_EDITOR_AREA);

		layout.addPerspectiveShortcut("au.com.langdale.cimtoole.CIMToolPerspective");
	
	}

}
