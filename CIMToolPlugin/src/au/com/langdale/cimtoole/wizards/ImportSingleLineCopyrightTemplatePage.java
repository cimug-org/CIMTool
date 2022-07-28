/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.project.Info;

public class ImportSingleLineCopyrightTemplatePage extends ImportCopyrightTemplatePage {

	public ImportSingleLineCopyrightTemplatePage() {
		super();
	}

	public IFile getFile() {
		return Info.getSingleLineCopyrightFile(projects.getProject());
	}

	@Override
	public String getCopyrightType() {
		return "single-line";
	}

	@Override
	public String getCurrentCopyrightTemplate(IProject project) {
		String copyrightText = "";
		try {
			copyrightText = Info.getSingleLineCopyrightTemplate(project);
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
			} else if (copyrightTemplate.contains("\n")) {
				message =  "The selected file contains carriage return(s). This is not allowed in a " + getCopyrightType() + " copyright template.";
			}
		}

		return message;
	}

}