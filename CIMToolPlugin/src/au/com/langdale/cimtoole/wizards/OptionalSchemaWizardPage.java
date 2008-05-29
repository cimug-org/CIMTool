/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.validation.Validation;

public class OptionalSchemaWizardPage extends FurnishedWizardPage {

	private IFile file;
	private String pathname;
	private String namespace;
	private IProject project;
	private String[] sources = new String[]{"*.xmi", "*.owl"};

	public OptionalSchemaWizardPage() {
		super("schema");
	}

	public IFile getFile() {
		return file;
	}
	
	public String getNamespace() {
		return namespace;
	}

	public String getPathname() {
		return pathname;
	}

	public String[] getSources() {
		return sources;
	}

	public void setSources(String[] sources) {
		this.sources = sources;
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			final String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);
			
			@Override
			protected Template define() {
				return Grid(
					Group(FileField("source", "File to import:", sources)),
					Group(Label("Namespace URI:"), Field("namespace", NAMESPACE)),
					Group(Label("Schema name:"), Field("filename"))
				);
			}

			@Override
			public String validate() {
				file = null;
				
				// TODO: replace with TextBinding.
				// the source file
				String candidate = getText("source").getText().trim();
				boolean changed = pathname == null || ! pathname.equals(candidate);
				pathname = candidate;
				if( pathname.length() == 0)
					return null;
				File source = new File(pathname);
				if( ! source.canRead())
					return "The chosen file cannot be read";
				if(changed)
					setTextValue("filename", source.getName());
				
				// TODO: replace with TextBinding.
				String filename = getText("filename").getText().trim();
				if( filename.length() == 0 || ! (filename.endsWith(".xmi") || filename.endsWith(".owl")))
					return "A name ending in .owl or .xmi is required";
				file = Info.getSchemaFolder(project).getFile(filename);

				// TODO: replace with TextBinding.
				namespace = getText("namespace").getText().trim();
				return Validation.NAMESPACE.validate(namespace);
			}
		};
	}

	public void setProject(IProject project) {
		this.project = project;
		if(getContent() != null)
			getContent().doRefresh();
		
	}
}