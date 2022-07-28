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
import org.eclipse.jface.viewers.IStructuredSelection;

import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public abstract class ImportCopyrightTemplatePage extends FurnishedWizardPage {

	private String pathname;
	private String[] sources;
	private String type;

	protected ProjectBinding projects = new ProjectBinding();

	public ImportCopyrightTemplatePage() {
		super("main");
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
				return Grid(
						Group(FileField("source", "File to import:", sources)),
						Group(Label("Project")),
						Group(CheckboxTableViewer("projects")),
						Group(Label("copyright-label", "Currently Assigned Copyright Template ("
								+ getCopyrightType() + ")")),
						Group(DisplayArea("copyright", true)));
			}

			@Override
			protected void addBindings() {
				projects.bind("projects", this);
			}

			@Override
			public String validate() {
				// TODO: replace with TextBinding.
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

				return null;
			}
			
			@Override
			public void refresh() {
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
		};
	}
	
}