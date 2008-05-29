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
public class CIMToolPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.addView("au.com.langdale.cimtoole.views.ProjectModelView", IPageLayout.LEFT, .25f, layout.getEditorArea());
		layout.addView("org.eclipse.ui.views.ResourceNavigator", IPageLayout.TOP, .5f, "au.com.langdale.cimtoole.views.ProjectModelView");
		layout.addView("org.eclipse.ui.views.ContentOutline", IPageLayout.RIGHT, .66f, layout.getEditorArea());

		layout.addShowViewShortcut("org.eclipse.ui.views.ProblemView");
		layout.addShowViewShortcut("org.eclipse.ui.views.ContentOutline");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.ProjectModelView");
		layout.addShowViewShortcut("org.eclipse.ui.views.ResourceNavigator");
		
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewProject");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewProfile");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewXSDRules");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewHTMLRules");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewRuleset");
		
		layout.addActionSet("au.com.langdale.cimtoole.CIMToolActions");
		layout.addPerspectiveShortcut("au.com.langdale.cimtoole.ValidationPerspective");
	}

}
