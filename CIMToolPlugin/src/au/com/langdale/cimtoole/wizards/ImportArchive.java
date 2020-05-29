/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.FileField;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.SplitModelImporter;
import au.com.langdale.ui.binding.TableBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.util.Jobs;
import au.com.langdale.workspace.ResourceUI.ProfileBinding;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class ImportArchive extends Wizard implements IImportWizard {
	private String pathname = "";
	private String suggestion = "";
	private IResource destin;
	private IFolder instances;

	private ProjectBinding projects = new ProjectBinding();
	private TextBinding namespace = new TextBinding(Validators.NAMESPACE, Info.getPreference(Info.INSTANCE_NAMESPACE));
	private ZipBinding zip = new ZipBinding();

	private ProfileBinding profiles = new ProfileBinding();
	
	public static class ZipBinding extends TableBinding {
		private ZipFile archive = null;

		public static class ZipLabel extends LabelProvider {
			@Override
			public Image getImage(Object element) {
				if (element.toString().endsWith(".xml") || element.toString().endsWith(".rdf")) {
					return IconCache.getIcons().get("individuals", false, 16);
				} else
					return IconCache.getIcons().get("empty", false, 16);
			}
		}

		public static class ZipContent implements IStructuredContentProvider {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof ZipFile) {
					ZipFile archive = (ZipFile) inputElement;
					ArrayList buffer = new ArrayList();
					Enumeration entries = archive.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = (ZipEntry) entries.nextElement();
						if (!entry.isDirectory())
							buffer.add(entry);
					}
					buffer.toArray();
				}
				return new Object[] {};
			}

			public void dispose() {
			}
		}

		@Override
		protected void configureViewer(StructuredViewer viewer) {
			viewer.setContentProvider(new ZipContent());
			viewer.setLabelProvider(new ZipLabel());
		}

		@Override
		protected Object getInput() {
			return archive;
		}

		public ZipFile getArchive() {
			return archive;
		}

		public void setInput(ZipFile input) {
			archive = input;
		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		projects.setSelected(selection);
	};

	private FurnishedWizardPage select = new FurnishedWizardPage("select", "Import a Model", null) {
		{
			setDescription("Import Model(s) (CIM/XML files) from a Zip Archive.");
		}

		@Override
		protected Content createContent() {

			return new Content() {

				@Override
				protected Template define() {
					return Grid(Group(FileField("source", "Archive file:", new String[] { "*.zip", "*.zipped" })),
							Group(Grid(Group(Label("Default Namespace:"), Field("namespace")))),
							Group(Label("Project")), Group(CheckboxTableViewer("projects"))

					);
				}

				@Override
				protected void addBindings() {
					projects.bind("projects", this);
					namespace.bind("namespace", this);
				}

				@Override
				public String validate() {
					pathname = getText("source").getText().trim();
					if (pathname.length() == 0)
						return "A zip archive must be chosen";
					try {
						zip.setInput(new ZipFile(pathname));
					} catch (IOException ex) {
						return "A valid zip archive must be chosen. " + ex.getMessage();
					}
					instances = Info.getInstanceFolder(projects.getProject());
					return null;
				}
			};
		}
	};

	private FurnishedWizardPage detail = new FurnishedWizardPage("detail", "Model Details", null) {
		{
			setDescription("Create the model.");
		}

		@Override
		protected Content createContent() {

			return new Content() {

				@Override
				protected Template define() {
					return Grid(Group(Label("Models"), Label("Profiles")),
							Group(CheckboxTableViewer("models", true), CheckboxTableViewer("profiles")));
				}

				protected void addBindings() {
					zip.bind("models", this);
					profiles.bind("profiles", this, projects);
				};

				@Override
				public String validate() {
					// display size
					File source = new File(pathname);
					long length = source.length();
					getLabel("size").setText("Size of source is " + Long.toString(length) + " bytes.");

					// setup the destination resource
					String filename = getText("filename").getText();
					if ((filename.equals(suggestion)) && pathname.length() > 0) {
						Path path = new Path(pathname);
						suggestion = path.removeFileExtension().lastSegment().replaceAll("[^0-9a-zA-Z._-]", "");
						filename = suggestion;
						setTextValue("filename", filename);
					}

					if (filename.length() == 0)
						return "A name is required for the imported model.";

					destin = instances.getFolder(filename);

					// overwrite model
					boolean exists = destin.exists();
					getButton("replace").setEnabled(exists);
					if (exists && !getButton("replace").getSelection())
						return "A model named " + filename + " already exists. " + "Check option to replace.";

					return null;
				}
			};
		}
	};

	@Override
	public void addPages() {
		addPage(select);
		addPage(detail);
	}

	@Override
	public boolean performFinish() {
		IWorkspaceRunnable op = new SplitModelImporter((IFolder) destin, pathname, namespace.getText(),
				profiles.getFile(), null);
		Jobs.runJob(op, instances, ("Importing model " + destin.getName()));
		return true;
	}

}
