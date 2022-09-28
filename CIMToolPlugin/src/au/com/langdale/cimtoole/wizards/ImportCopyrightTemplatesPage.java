/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.DisplayArea;
import static au.com.langdale.ui.builder.Templates.FileField;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.RadioButton;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class ImportCopyrightTemplatesPage extends FurnishedWizardPage {

	private String multiLineCopyrightPathname;
	private String singleLineCopyrightPathname;
	private String[] multiLineCopyrightSources;
	private String[] singleLineCopyrightSources;

	protected ProjectBinding projects = new ProjectBinding();
	protected final boolean expectNewProject;
	protected IProject newProject;
	
	public enum Option {
		NO_TEMPLATE, DEFAULT_TEMPLATE, IMPORT_TEMPLATE
	}
	
	public String getMultiLineCopyrightTemplateTextForSelectedOption() {
		String copyright = "";

		if (getContent().getButton(Option.DEFAULT_TEMPLATE.name()).getSelection()) {
			copyright = getDefaultMultiLineCopyrightTemplate();
		} else if (getContent().getButton(Option.IMPORT_TEMPLATE.name()).getSelection()) {
			String pathname = getMultiLineCopyrightPathname();
			copyright = getFileContents(pathname);
		} else if (getContent().getButton(Option.NO_TEMPLATE.name()).getSelection()) {
			try {
				copyright = Info.getDefaultEmptyCopyrightTemplate();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return copyright;
	}
	
	public String getSingleLineCopyrightTemplateTextForSelectedOption() {
		String copyright = "";

		if (getContent().getButton(Option.DEFAULT_TEMPLATE.name()).getSelection()) {
			copyright = getDefaultSingleLineCopyrightTemplate();
		} else if (getContent().getButton(Option.IMPORT_TEMPLATE.name()).getSelection()) {
			String pathname = getSingleLineCopyrightPathname();
			copyright = getFileContents(pathname);
		} else if (getContent().getButton(Option.NO_TEMPLATE.name()).getSelection()) {
			try {
				copyright = Info.getDefaultEmptyCopyrightTemplate();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return copyright;
	}
	
	public String getCurrentMultiLineCopyrightTemplate(IProject project) {
		String copyrightText = "";
		try {
			copyrightText = Info.getMultiLineCopyrightTemplate(project);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return copyrightText;
	}
	
	public String getCurrentSingleLineCopyrightTemplate(IProject project) {
		String copyrightText = "";
		try {
			copyrightText = Info.getSingleLineCopyrightTemplate(project);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return copyrightText;
	}
	
	public ImportCopyrightTemplatesPage() {
		this(false);
	}
	
	public ImportCopyrightTemplatesPage(boolean expectNewProject) {
		super("main");
		this.expectNewProject = expectNewProject;
	}
	
	public ImportCopyrightTemplatesPage(String pageName, boolean expectNewProject) {
		super(pageName);
		this.expectNewProject = expectNewProject;
	}
	
	public void setNewProject(IProject newProject) {
		this.newProject = newProject;
	}

	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}

	public String getMultiLineCopyrightPathname() {
		return multiLineCopyrightPathname;
	}
	
	public String getSingleLineCopyrightPathname() {
		return singleLineCopyrightPathname;
	}

	public String[] getMultiLineCopyrightSources() {
		return multiLineCopyrightSources;
	}
	
	public String[] getSingleLineCopyrightSources() {
		return singleLineCopyrightSources;
	}

	public void setMultiLineCopyrightSources(String[] extensions) {
		multiLineCopyrightSources = extensions;
	}
	
	public void setSingleLineCopyrightSources(String[] extensions) {
		singleLineCopyrightSources = extensions;
	}
	
	public IFile getMultiLineCopyrightFile() {
		IFile file = Info.getMultiLineCopyrightFile((expectNewProject ? newProject : projects.getProject()));
		return file;
	}
	
	public IFile getSingleLineCopyrightFile() {
		IFile file = Info.getSingleLineCopyrightFile((expectNewProject ? newProject : projects.getProject()));
		return file;
	}

	public String getDefaultMultiLineCopyrightTemplate() {
		String defaultCopyrightTemplate = "";
		try {
			defaultCopyrightTemplate = Info.getDefaultMultiLineCopyrightTemplate();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return defaultCopyrightTemplate;
	}

	public String getDefaultSingleLineCopyrightTemplate() {
		String defaultCopyrightTemplate = "";
		try {
			defaultCopyrightTemplate = Info.getDefaultSingleLineCopyrightTemplate();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return defaultCopyrightTemplate;
	}
	
	protected String validateMultiLineCopyrightFileContents(String pathname) {
		String message = null;
		
		String copyrightTemplate = getFileContents(pathname);
		if (!"".equals(copyrightTemplate.trim())) {
			if (!copyrightTemplate.contains("${year}") && !copyrightTemplate.contains("${YEAR}")) {
				message =  "The selected multiline copyright template file must contain a ${year} variable.";
			} else if (!copyrightTemplate.contains("\n")) {
				message =  "The selected file does not contain multiple lines. This is expected in a multiline copyright template.";
			}	
		}

		return message;
	}
	
	protected String validateCopyrightFilesForConsistency(String multiLineCopyrightPathname, String singleLineCopyrightPathname) {
		String message = null;
		
		String multiLineCopyrightTemplate = getFileContents(multiLineCopyrightPathname);
		String singleLineCopyrightTemplate = getFileContents(singleLineCopyrightPathname);
		
		if ((!"".equals(multiLineCopyrightTemplate.trim()) && "".equals(singleLineCopyrightTemplate.trim())) || 
				("".equals(multiLineCopyrightTemplate.trim()) && !"".equals(singleLineCopyrightTemplate.trim()))) {
			message =  "Both selected template files must either have valid copyrights or be empty (i.e. no copyright is to be applied).";
		}

		return message;
	}
	
	protected String validateSingleLineCopyrightFileContents(String pathname) {
		String message = null;
		
		String copyrightTemplate = getFileContents(pathname);
		if (!"".equals(copyrightTemplate.trim())) {
			if (!copyrightTemplate.contains("${year}") && !copyrightTemplate.contains("${YEAR}")) {
				message =  "The selected single-line copyright template file must contain a ${year} variable.";
			} else if (copyrightTemplate.contains("\n")) {
				message =  "The selected file contains carriage return(s). This is not allowed in a single-line copyright template.";
			}
		}

		return message;
	}

	protected String getFileContents(String pathname) {
		String contents = null;
		
		InputStream source;
		try {
			source = new BufferedInputStream(new FileInputStream(pathname));
			contents = new String(IOUtils.toByteArray(new InputStreamReader(source), CharEncoding.UTF_8));
		} catch (IOException e) {
			//
		}

		return contents;
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				Template template = null;
				if (expectNewProject) {
					template = Grid(
							Group(Label("Multiline file to import:")), //
							Group(FileField("multi-line-source", "", multiLineCopyrightSources)),
							Group(Label("Single-line file to import:")), //
							Group(FileField("single-line-source", "", singleLineCopyrightSources)),
							Group(Label("")), //
							Group(RadioButton(Option.NO_TEMPLATE.name(), "Do not include copyrights in generated profiles and artifacts for this project")), //
							Group(RadioButton(Option.DEFAULT_TEMPLATE.name(), "Use the default UCAIug Apache 2.0 copyright templates")), //
							Group(RadioButton(Option.IMPORT_TEMPLATE.name(), "Select custom copyright template files to import for this project")), //
							Group(Label("")), //				
							Group(Label("multi-line-copyright-label", "Contents of Selected Copyright Template (multiline)"), Label("single-line-copyright-label", "Contents of Selected Copyright Template (single-line)")),
							Group(DisplayArea("multi-line-copyright", true), DisplayArea("single-line-copyright", true)));					
				} else {
					template = Grid(
							Group(Label("Project")),
							Group(CheckboxTableViewer("projects")),
							Group(Label("")), //
							Group(Label("* To have no copyrights applied to generated profiles and artifacts simply import copyright template files that are empty.")), //	
							Group(Label("")), //
							Group(Label("Multiline file to import:")), //
							Group(FileField("multi-line-source", "", multiLineCopyrightSources)),
							Group(Label("Single-line file to import:")), //
							Group(FileField("single-line-source", "", singleLineCopyrightSources)),
							Group(Label("")), //
							Group(Label("multi-line-copyright-label", "Currently Assigned Copyright Template (multiline)"), Label("single-line-copyright-label", "Currently Assigned Copyright Template (single-line)")),
							Group(DisplayArea("multi-line-copyright", true), DisplayArea("single-line-copyright", true)));
				}
				return template;
			}

			@Override
			protected void addBindings() {
				if (!expectNewProject) {
					projects.bind("projects", this);
				}
			}
			
			@Override
			public String validate() {
				if (expectNewProject) {
					if (getButton(Option.IMPORT_TEMPLATE.name()).getSelection()) {
						multiLineCopyrightPathname = getText("multi-line-source").getText().trim();
						if (multiLineCopyrightPathname.length() == 0)
							return "A file containing a multiline copyright header template must be chosen for import.";
										
						File multiLineCopyrightsource = new File(multiLineCopyrightPathname);
						if (!multiLineCopyrightsource.canRead())
							return "The chosen mulitline file cannot be read.";
						
						singleLineCopyrightPathname = getText("single-line-source").getText().trim();
						if (singleLineCopyrightPathname.length() == 0)
							return "A file containing a single-line copyright header template must be chosen for import.";
									
						File singleLineCopyrightsource = new File(singleLineCopyrightPathname);
						if (!singleLineCopyrightsource.canRead())
							return "The chosen single-line file cannot be read.";
						
						IProject project = newProject;
						if (project != null) {
							String message = validateMultiLineCopyrightFileContents(multiLineCopyrightPathname);
							if (message != null)
								return message;
							
							message = validateSingleLineCopyrightFileContents(singleLineCopyrightPathname);
							if (message != null)
								return message;
							
							message = validateCopyrightFilesForConsistency(multiLineCopyrightPathname, singleLineCopyrightPathname);
							if (message != null)
								return message;
						}
					}
				} else {
					multiLineCopyrightPathname = getText("multi-line-source").getText().trim();
					if (multiLineCopyrightPathname.length() == 0)
						return "A file containing a multiline copyright header template must be chosen for import.";
									
					File multiLineCopyrightsource = new File(multiLineCopyrightPathname);
					if (!multiLineCopyrightsource.canRead())
						return "The chosen mulitline file cannot be read.";
					
					singleLineCopyrightPathname = getText("single-line-source").getText().trim();
					if (singleLineCopyrightPathname.length() == 0)
						return "A file containing a single-line copyright header template must be chosen for import.";
								
					File singleLineCopyrightsource = new File(singleLineCopyrightPathname);
					if (!singleLineCopyrightsource.canRead())
						return "The chosen single-line file cannot be read.";
					
					IProject project = projects.getProject();
					if (project != null) {
						String message = validateMultiLineCopyrightFileContents(multiLineCopyrightPathname);
						if (message != null)
							return message;
						
						message = validateSingleLineCopyrightFileContents(singleLineCopyrightPathname);
						if (message != null)
							return message;
						
						message = validateCopyrightFilesForConsistency(multiLineCopyrightPathname, singleLineCopyrightPathname);
						if (message != null)
							return message;
					}
				}

				return null;
			}
			
			@Override
			public void refresh() {
				if (expectNewProject) {
					if (getButton(Option.DEFAULT_TEMPLATE.name()).getSelection()) {
						if (getControl("multi-line-source").isEnabled()) {
							getControl("multi-line-source").setEnabled(false);
						}
						setTextValue("multi-line-source", null);
						setTextValue("multi-line-copyright", getDefaultMultiLineCopyrightTemplate());
						//
						if (getControl("single-line-source").isEnabled()) {
							getControl("single-line-source").setEnabled(false);
						}
						setTextValue("single-line-source", null);
						setTextValue("single-line-copyright", getDefaultSingleLineCopyrightTemplate());
					} else if (getButton(Option.IMPORT_TEMPLATE.name()).getSelection()) {
						if (!getControl("multi-line-source").isEnabled()) {
							getControl("multi-line-source").setEnabled(true);
						}
						String pathname = getText("multi-line-source").getText().trim();
						if ((pathname != null) && (pathname.length() != 0) && new File(pathname).canRead()) {
							setTextValue("multi-line-copyright", getFileContents(pathname));
						} else {
							setTextValue("multi-line-copyright", getCurrentMultiLineCopyrightTemplate(projects.getProject()));
						}
						//
						if (!getControl("single-line-source").isEnabled()) {
							getControl("single-line-source").setEnabled(true);
						}
						pathname = getText("single-line-source").getText().trim();
						if ((pathname != null) && (pathname.length() != 0) && new File(pathname).canRead()) {
							setTextValue("single-line-copyright", getFileContents(pathname));
						} else {
							setTextValue("single-line-copyright", getCurrentSingleLineCopyrightTemplate(projects.getProject()));
						}
					} else if (getButton(Option.NO_TEMPLATE.name()).getSelection()) {
						if (getControl("multi-line-source").isEnabled()) {
							getControl("multi-line-source").setEnabled(false);
						}
						setTextValue("multi-line-source", null);
						setTextValue("multi-line-copyright", null);
						//
						if (getControl("single-line-source").isEnabled()) {
							getControl("single-line-source").setEnabled(false);
						}
						setTextValue("single-line-source", null);
						setTextValue("single-line-copyright", null);
					} else {
						getButton(Option.NO_TEMPLATE.name()).setSelection(true);
						if (getControl("multi-line-source").isEnabled()) {
							getControl("multi-line-source").setEnabled(false);
						}
						setTextValue("multi-line-source", null);
						setTextValue("multi-line-copyright", null);
						//
						getButton(Option.NO_TEMPLATE.name()).setSelection(true);
						if (getControl("single-line-source").isEnabled()) {
							getControl("single-line-source").setEnabled(false);
						}
						setTextValue("single-line-source", null);
						setTextValue("signle-line-copyright", null);
					}
				} else {
					String pathname = getText("multi-line-source").getText().trim();
					if ((pathname != null) && (pathname.length() != 0) && new File(pathname).canRead()) {
						setTextValue("multi-line-copyright-label", "Contents of Selected Multiline Copyright Template");
						setTextValue("multi-line-copyright", getFileContents(pathname));
					} else {
						setTextValue("multi-line-copyright-label", "Currently Assigned Copyright Template (multiline)");
						setTextValue("multi-line-copyright", getCurrentMultiLineCopyrightTemplate(projects.getProject()));
					}
					//
					pathname = getText("single-line-source").getText().trim();
					if ((pathname != null) && (pathname.length() != 0) && new File(pathname).canRead()) {
						setTextValue("single-line-copyright-label", "Contents of Selected Single-line Copyright Template");
						setTextValue("single-line-copyright", getFileContents(pathname));
					} else {
						setTextValue("single-line-copyright-label", "Currently Assigned Copyright Template (single-line)");
						setTextValue("single-line-copyright", getCurrentSingleLineCopyrightTemplate(projects.getProject()));
					}
				}
			}
		};
	}
	
}