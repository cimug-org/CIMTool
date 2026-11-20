/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.IPerspectiveFactory;
/**
 * The main eclipse perspective for CIMTool.
 */
public class CIMToolPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.addView("au.com.langdale.cimtoole.views.ProjectModelView", IPageLayout.LEFT, .25f, layout.getEditorArea());
		layout.addView("org.eclipse.ui.navigator.ProjectExplorer", IPageLayout.TOP, .5f, "au.com.langdale.cimtoole.views.ProjectModelView");

		// Help column at the far right, shown only while the CIMTool Context Help panel or
		// Eclipse's Help view (F1, Help Contents) is open; the two open there as sibling tabs.
		// It is split from the editor area before the Detail folder and the Outline, so it runs
		// the full height of the window and, like Eclipse's own Help view, takes its width from
		// the editor; the layout is unchanged when it is closed.
		IPlaceholderFolderLayout helpFolder = layout.createPlaceholderFolder("au.com.langdale.cimtoole.Help", IPageLayout.RIGHT, 0.7f, layout.getEditorArea());
		helpFolder.addPlaceholder("au.com.langdale.cimtoole.views.CIMToolHelpView");
		helpFolder.addPlaceholder("org.eclipse.help.ui.HelpView");

		IFolderLayout folder = layout.createFolder("au.com.langdale.cimtoole.Detail", IPageLayout.BOTTOM, .66f, layout.getEditorArea());
		folder.addView("au.com.langdale.cimtoole.views.CurrentProfilePlantUmlSvgView");
		folder.addView("net.sourceforge.plantuml.eclipse.views.PlantUmlSvgView");	
		folder.addView("au.com.langdale.cimtoole.views.Documentation");
		folder.addView("org.eclipse.ui.views.PropertySheet");
		
		IFolderLayout topRightContentLayout = layout.createFolder("topRightFolder", IPageLayout.RIGHT, 0.75f, layout.getEditorArea());
	    topRightContentLayout.addView("org.eclipse.ui.views.ContentOutline");
		
		layout.addShowViewShortcut("org.eclipse.ui.views.ProblemView");
		layout.addShowViewShortcut("org.eclipse.ui.views.PropertySheet");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.Documentation");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.CurrentProfilePlantUmlSvgView");
		layout.addShowViewShortcut("net.sourceforge.plantuml.eclipse.views.PlantUmlSvgView");	
		layout.addShowViewShortcut("org.eclipse.ui.views.ContentOutline");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.ProjectModelView");
		layout.addShowViewShortcut("org.eclipse.ui.navigator.ProjectExplorer");
		layout.addShowViewShortcut("au.com.langdale.cimtoole.views.CIMToolHelpView");
		
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewProject");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewProfile");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewXSDRules");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewHTMLRules");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewRuleset");
		layout.addNewWizardShortcut("au.com.langdale.cimtoole.wizards.NewMappings");
		
		layout.addActionSet("au.com.langdale.cimtoole.CIMToolActions");
		layout.addPerspectiveShortcut("au.com.langdale.cimtoole.CIMToolBrowsingPerspective");
		layout.addPerspectiveShortcut("au.com.langdale.cimtoole.ValidationPerspective");
	}

}
