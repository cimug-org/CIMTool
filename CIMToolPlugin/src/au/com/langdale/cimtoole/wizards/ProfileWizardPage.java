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

public class ProfileWizardPage extends FurnishedWizardPage {

	private final String NAMESPACE = Info.getPreference(Info.PROFILE_NAMESPACE);
	private final String ENVELOPE = Info.getPreference(Info.PROFILE_ENVELOPE);
	
	private IFile file;
	
	private String[] sources;
	private boolean importing;

	private ProjectBinding projects = new ProjectBinding();
	private TextBinding source = new TextBinding(Validation.EXTANT_FILE);
	private LocalFileBinding filename = new LocalFileBinding("owl");
	private TextBinding namespace = new TextBinding(Validation.NAMESPACE, NAMESPACE);
	private TextBinding envelope = new TextBinding(Validation.NCNAME, ENVELOPE);
	
	public ProfileWizardPage() {
		super("main");
	}

	public boolean isImporting() {
		return importing;
	}

	public void setImporting(boolean importing) {
		this.importing = importing;
	}

	public String getPathname() {
		return source.getText();
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

	public String getEnvname() {
		return envelope.getText();
	}

	public String[] getSources() {
		return sources;
	}

	public void setSources(String[] sources) {
		this.sources = sources;
		importing = sources != null;
	}
	
	@Override
	protected Content createContent() {
		return new Content() {

			
			@Override
			protected Template define() {
				return Grid(
					Source(),
					Group(Label("Namespace URI:"), Field("namespace")),
					Group(Label("Project")),
					Group(CheckboxTableViewer("projects")),
					Group(Label("Profile name:"), Field("filename")),
					Group(Label("Envelope Element Name"), Field("envelope"))
				);
			}
			
			private Group Source() {
				if(importing)
					return Group(FileField("source", "File to import:", sources));
				else
					return null;
			}

			@Override
			public Control realise(Composite parent) {
				Control panel = super.realise(parent);
				projects.bind("projects", this);
				if( importing )
					source.bind("source", this);
				filename.bind("filename", this, source);
				namespace.bind("namespace", this);
				envelope.bind("envelope", this);
				return panel;
			}

			@Override
			public String validate() {
				file = filename.getFile(Info.getProfileFolder(projects.getProject()));
				
				if( file.exists())
					return "A profile of that name already exists.";

				return null;
			}
		};
	}
}