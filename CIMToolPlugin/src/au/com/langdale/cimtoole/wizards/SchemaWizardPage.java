/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.ResourceUI.LocalFileBinding;
import au.com.langdale.ui.binding.ResourceUI.ProjectBinding;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.validation.Validation;

public class SchemaWizardPage extends FurnishedWizardPage {

	private String NAMESPACE = Info.getPreference(Info.SCHEMA_NAMESPACE);
	private static final String[] sources = {"*.xmi", "*.owl"};

	private IFile file;
	boolean importing;

	private TextBinding source = new TextBinding(Validation.EXTANT_FILE);
	private LocalFileBinding filename = new LocalFileBinding(sources);
	private TextBinding namespace = new TextBinding(Validation.NAMESPACE, NAMESPACE);

	private ProjectBinding projects = new ProjectBinding();

	public SchemaWizardPage() {
		super("schema");
	}
	
	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}

	public IFile getFile() {
		return file;
	}
	
	public String getNamespace() {
		return namespace.getText();
	}

	public String getPathname() {
		return source.getText();
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(
					Group(FileField("source", "File to import:", sources)),
					Group(Label("Namespace URI:"), Field("namespace")),
					Group(Label("Project")), 
					Group(CheckboxTableViewer("projects")),
					Group(Label("Schema name:"), Field("filename"))
				);
			}

			@Override
			public Control realise(Composite parent) {
				Control panel = super.realise(parent);
				projects.bind("projects", this);
				source.bind("source", this);
				filename.bind("filename", this, source);
				return panel;
			}

			@Override
			public String validate() {
				// TODO: incorporate replace checkbox
				file = filename.getFile(Info.getSchemaFolder(projects.getProject()));
				if( file.exists())
					return "A schema of that name already exists.";
				return null;
			}
		};
	}
}