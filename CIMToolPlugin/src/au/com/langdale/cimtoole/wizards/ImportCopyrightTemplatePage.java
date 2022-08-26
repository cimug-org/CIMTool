/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.DisplayArea;
import static au.com.langdale.ui.builder.Templates.DisplayFileField;
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
import au.com.langdale.ui.binding.RadioTextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public abstract class ImportCopyrightTemplatePage extends FurnishedWizardPage {

	private String pathname;
	private String[] sources;
	private String type;

	protected ProjectBinding projects = new ProjectBinding();
	protected final boolean expectNewProject;
	protected IProject newProject;
	
	public enum Option {
		NO_TEMPLATE, DEFAULT_TEMPLATE, IMPORT_TEMPLATE
	}
	
	private RadioTextBinding namespace = new RadioTextBinding(Validators.NAMESPACE, Option.NO_TEMPLATE.name());
	
	private String[] presets = new String[] {
			Option.NO_TEMPLATE.name(), Option.NO_TEMPLATE.name(),
			Option.DEFAULT_TEMPLATE.name(), "http://iec.ch/TC57/2016/CIM-schema-cim16#",
			Option.IMPORT_TEMPLATE.name(), "http://iec.ch/TC57/2022/CIM-schema-cim17#",
			"preset", Option.NO_TEMPLATE.name()
	};
	
	public String getCopyrightTemplateTextForSelectedOption() {
		String copyright = "";

		if (getContent().getButton(Option.DEFAULT_TEMPLATE.name()).getSelection() || getContent().getButton(Option.IMPORT_TEMPLATE.name()).getSelection()) {
			copyright = getDefaultCopyrightTemplate();
		} else {
			try {
				copyright = Info.getDefaultEmptyCopyrightTemplate();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return copyright;
	}
	
	public ImportCopyrightTemplatePage() {
		this(false);
	}
	
	public ImportCopyrightTemplatePage(boolean expectNewProject) {
		super("main");
		this.expectNewProject = expectNewProject;
	}
	
	public ImportCopyrightTemplatePage(String pageName, boolean expectNewProject) {
		super(pageName);
		this.expectNewProject = expectNewProject;
	}
	
	public void setNewProject(IProject newProject) {
		this.newProject = newProject;
	}

	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}

	public String getPathname() {
		return pathname;
	}

	public String[] getSources() {
		return sources;
	}

	public void setSources(String[] extensions) {
		sources = extensions;
	}

	public String getType() {
		return type;
	}

	public void setType(String extension) {
		type = extension;
	}

	public abstract IFile getFile();

	public abstract String getCopyrightType();

	public abstract String getCurrentCopyrightTemplate(IProject project);
	
	public abstract String getDefaultCopyrightTemplate();
	
	protected abstract String validateFileContents(String pathname);

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
							Group(FileField("source", "File to import:", sources)),
							Group(Label("")), //
							Group(RadioButton(Option.NO_TEMPLATE.name(), "Do not include " + getCopyrightType() + " copyrights in generated profiles and artifacts for this project")), //
							Group(RadioButton(Option.DEFAULT_TEMPLATE.name(), "Use the default UCAIug Apache 2.0 " + getCopyrightType() + " copyright template")), //
							Group(RadioButton(Option.IMPORT_TEMPLATE.name(), "Select a custom " + getCopyrightType() + " copyright template file to import for this project")), //
							Group(Label("")), //				
							Group(Label("copyright-label", "Contents of Selected Copyright Template ("
									+ getCopyrightType() + ")")),
							Group(DisplayArea("copyright", true)));					
				} else {
					template = Grid(
							Group(FileField("source", "File to import:", sources)),
							Group(Label("Project")),
							Group(CheckboxTableViewer("projects")),
							Group(Label("copyright-label", "Currently Assigned Copyright Template ("
									+ getCopyrightType() + ")")),
							Group(DisplayArea("copyright", true)));
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
						pathname = getText("source").getText().trim();
						if (pathname.length() == 0)
							return "A file containing a " + getCopyrightType()
									+ " copyright header template must be chosen for import.";
						
						File source = new File(pathname);
						if (!source.canRead())
							return "The chosen file cannot be read.";
						
						IProject project = newProject;
						if (project != null) {
							String message = validateFileContents(pathname);
							if (message != null)
								return message;
						}
					}
				} else {
					pathname = getText("source").getText().trim();
					if (pathname.length() == 0)
						return "A file containing a " + getCopyrightType()
								+ " copyright header template must be chosen for import.";
					File source = new File(pathname);
					if (!source.canRead())
						return "The chosen file cannot be read";
					
					IProject project = projects.getProject();
					if (project != null) {
						String message = validateFileContents(pathname);
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
						if (getControl("source").isEnabled()) {
							getControl("source").setEnabled(false);
						}
						setTextValue("source", null);
						setTextValue("copyright", getDefaultCopyrightTemplate());
					} else if (getButton(Option.IMPORT_TEMPLATE.name()).getSelection()) {
						if (!getControl("source").isEnabled()) {
							getControl("source").setEnabled(true);
						}
						String pathname = getText("source").getText().trim();
						if ((pathname != null) && (pathname.length() != 0) && new File(pathname).canRead()) {
							setTextValue("copyright", getFileContents(pathname));
						} else {
							setTextValue("copyright", getCurrentCopyrightTemplate(projects.getProject()));
						}
					} else if (getButton(Option.NO_TEMPLATE.name()).getSelection()) {
						if (getControl("source").isEnabled()) {
							getControl("source").setEnabled(false);
						}
						setTextValue("source", null);
						setTextValue("copyright", null);
					} else {
						getButton(Option.NO_TEMPLATE.name()).setSelection(true);
						if (getControl("source").isEnabled()) {
							getControl("source").setEnabled(false);
						}
						setTextValue("source", null);
						setTextValue("copyright", null);
					}
				} else {
					String pathname = getText("source").getText().trim();
					if ((pathname != null) && (pathname.length() != 0) && new File(pathname).canRead()) {
						setTextValue("copyright-label", "Contents of Selected Copyright Template");
						setTextValue("copyright", getFileContents(pathname));
					} else {
						setTextValue("copyright-label", "Currently Assigned Copyright Template ("
								+ getCopyrightType() + ")");
						setTextValue("copyright", getCurrentCopyrightTemplate(projects.getProject()));
					}
				}
			}
		};
	}
	
}