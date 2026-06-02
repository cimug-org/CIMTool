/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.FileField;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.SpreadsheetImporter;
import au.com.langdale.cimtoole.project.SpreadsheetImporter.Choice;
import au.com.langdale.profiles.SpreadsheetParser.CellSpec;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class ImportSpreadsheet extends Wizard  implements IImportWizard {

	private String namespace;
	private Choice profileChoice;
	private IFile destin;
	private String pathname;
	private SpreadsheetImporter importer= new SpreadsheetImporter();
	
	private ProjectBinding projects = new ProjectBinding();

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
		projects.setSelected(selection);
	}

	private FurnishedWizardPage read = new FurnishedWizardPage("read", "Import from Spreadsheet", null) {
		{
			setDescription(
				"Extract a profile definition from a spreadsheet."
			);
		}

		@Override
		protected Content createContent() {

			return new Content() {

				@Override
				protected Template define() {
					return Grid(
						Group(FileField("source", "Spreadsheet file:", new String[]{"*.xls", "*.xlsx"})),
						Group(Label("Profiles Found in Spreadsheet")),
						Group(CheckboxTableViewer("profiles"))
					);
				}

				@Override
				public String validate() {
					// TODO: replace with TextBinding.
					pathname = getText("source").getText().trim();

					// the source file
					if( pathname.length() == 0)
						return "A source spreadsheet is required";
					File source = new File(pathname);
					if( ! source.canRead())
						return "The spreadsheet file cannot be read";
					
					// preview the spreadsheet
					if( importer.getPathName() == null || ! importer.getPathName().equals(pathname)) {
						if( ! Jobs.runInteractive(importer.getReader(pathname), null, getContainer(), getShell()))
							return "failed to read spreadsheet";
						getCheckboxTableViewer("profiles").setInput(importer.getStandardFlags());
					}
					
					// choose a profile
					Object[] profiles = getCheckboxTableViewer("profiles").getCheckedElements();
					if( profiles.length == 0)
						return "A profile from the spreadsheet must be selected.";
					profileChoice = (Choice) profiles[0];
					return null;
				}
				
			};
		}
	};
	
	private FurnishedWizardPage create = new FurnishedWizardPage("create", "Create Profile", null) {
		{
			setDescription(
				"Create a new OWL profile definition in the selected CIMTool project. "
			);
		}

		@Override
		protected Content createContent() {

			return new Content() {

				final String NAMESPACE = Info.getPreference(Info.PROFILE_NAMESPACE);

				@Override
				protected Template define() {
					return Grid(
						Group(Label("Namespace URI:"), Field("namespace", NAMESPACE)),
						Group(Label("Project")),
						Group(CheckboxTableViewer("projects")),
						Group(Label("Profile name:"), Field("filename")),
						Group(CheckBox("replace", "Replace Existing Profile"))
					);
				}

				@Override
				protected void addBindings() {
					projects.bind("projects", this);
				}

				@Override
				public String validate() {
					if( profileChoice == null)
						return "A profile must be selected on the previous page";
					// the destination resource
					String filename = getText("filename").getText().trim();
					if( filename.length() == 0) {
						filename = profileChoice.name.replaceAll("[^0-9a-zA-Z]", "") + ".owl";
						setTextValue("filename", filename);
					}
					
					// overwrite profile
					destin = Info.getProfileFolder(projects.getProject()).getFile(filename);
					boolean exists = destin.exists();
					getButton("replace").setEnabled(exists);
					if( exists && ! getButton("replace").getSelection())
							return "A profile named " + filename + " already exists. " +
								"Check Replace Existing Profile to overwrite it.";

					// TODO: replace with TextBinding.
					// select the namespace
					namespace = getText("namespace").getText().trim();
					return Validators.NAMESPACE.validate(namespace);
				}
				
			};
		}
	};
	
	@Override
	public void addPages() {
		addPage(read);
		addPage(create);
	}

	@Override
	public boolean performFinish() {
		CellSpec[] standardCells = SpreadsheetImporter.getStandardCells(profileChoice.index);
		return Jobs.runInteractive(importer.getInterpreter(destin, namespace, standardCells), null, getContainer(), getShell());
	}
}
