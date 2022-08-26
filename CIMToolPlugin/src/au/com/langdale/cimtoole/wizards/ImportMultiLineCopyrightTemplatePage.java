/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.project.Info;

public class ImportMultiLineCopyrightTemplatePage extends ImportCopyrightTemplatePage {
	
	public ImportMultiLineCopyrightTemplatePage() {
		super(false);
	}
	
	public ImportMultiLineCopyrightTemplatePage(boolean expectNewProject) {
		super(expectNewProject);
	}
	
	public ImportMultiLineCopyrightTemplatePage(String pageName, boolean expectNewProject) {
		super(pageName, expectNewProject);
	}

	public IFile getFile() {
		IFile file = Info.getMultiLineCopyrightFile((expectNewProject ? newProject : projects.getProject()));
		return file;
	}

	public String getCopyrightType() {
		return "multiline";
	}

	@Override
	public String getDefaultCopyrightTemplate() {
		String defaultCopyrightTemplate = "";
		try {
			defaultCopyrightTemplate = Info.getDefaultMultiLineCopyrightTemplate();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return defaultCopyrightTemplate;
	}
	
	@Override
	public String getCurrentCopyrightTemplate(IProject project) {
		String copyrightText = "";
		try {
			copyrightText = Info.getMultiLineCopyrightTemplate(project);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return copyrightText;
	}

	@Override
	protected String validateFileContents(String pathname) {
		String message = null;
		
		String copyrightTemplate = getFileContents(pathname);
		if (!"".equals(copyrightTemplate.trim())) {
			if (!copyrightTemplate.contains("${year}") && !copyrightTemplate.contains("${YEAR}")) {
				message =  "The selected " + getCopyrightType() + " copyright template file must contain a ${year} variable.";
			} else if (!copyrightTemplate.contains("\n")) {
				message =  "The selected file should contain carriage returns. This is expected in a " + getCopyrightType() + " copyright template.";
			}	
		}

		return message;
	}

}