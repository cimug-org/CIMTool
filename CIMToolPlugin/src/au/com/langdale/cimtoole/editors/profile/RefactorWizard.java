/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.ProfileReorganizer;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.profiles.Remapper;
import au.com.langdale.profiles.Reorganizer;
import au.com.langdale.profiles.SchemaReorganizer;
import au.com.langdale.ui.binding.BooleanBinding;
import au.com.langdale.ui.binding.BooleanModel;
import au.com.langdale.ui.binding.BooleanModel.BooleanValue;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;

public class RefactorWizard extends Wizard  {
	private ProfileEditor master;
	
	public RefactorWizard(ProfileEditor master) {
		this.master = master;
		setNeedsProgressMonitor(true);
	}
	
	public void run() {
		Shell shell = master.getSite().getWorkbenchWindow().getShell();
        WizardDialog dialog = new WizardDialog(shell, this);
        dialog.create();
        dialog.open();
	}
	
	BooleanValue refs = new BooleanValue("Stereotype all properties as By Reference");
	BooleanValue concrete = new BooleanValue("Stereotype leaf classes as Concrete");
	BooleanValue remap = new BooleanValue("Repair and remap profile to schema");
	BooleanValue reorg = new BooleanValue("Reorganise profile per RDFS rules");
	BooleanValue useProfileCardinality = new BooleanValue("Reorganise profile per RDFS rules but keep profile cardinalities as currently defined in the profile");
	BooleanValue useSchemaCardinality = new BooleanValue("Reorganise profile per RDFS rules but override profile cardinalities with those defined in the base schema");

	BooleanBinding options = new BooleanBinding() {
		@Override
		protected BooleanModel[] getFlags() {
			return new BooleanModel[] {refs, concrete, remap, reorg, useProfileCardinality, useSchemaCardinality};
		}
		
		@Override
		public String validate() {
			if( refs.isTrue() || concrete.isTrue() || remap.isTrue() || reorg.isTrue() || useProfileCardinality.isTrue() || useSchemaCardinality.isTrue())
				return null;
			else
				return "At least one option must be selected";
		}
	};
	
	@Override
    public void addPages() {
		setWindowTitle("Repair and Reorganize"); 
        addPage(new RefactorWizardPage());        
    }
	
	public class WizardAction implements IWorkspaceRunnable {
		OntModel profileModel = master.getProfileModel();
		OntModel projectModel = master.getProjectModel();

		public void run(IProgressMonitor monitor) throws CoreException {
			if( profileModel == null || projectModel == null)
				return;

			if( remap.isTrue()) {
				Remapper utility = new Remapper(profileModel, projectModel);
				utility.run();
				monitor.worked(1);
			}
			if( reorg.isTrue() || useProfileCardinality.isTrue() || useSchemaCardinality.isTrue()) {
				Reorganizer utility;
				if (reorg.isTrue()) {
					utility = new Reorganizer(profileModel, projectModel, refs.isTrue());
					utility.run();
					profileModel = utility.getResult();
					monitor.worked(1);
				}
				if (useSchemaCardinality.isTrue()) {
					utility = new SchemaReorganizer(profileModel, projectModel, refs.isTrue());
					utility.run();
					profileModel = utility.getResult();
					monitor.worked(1);
				}
				if (useProfileCardinality.isTrue()) {
					utility = new ProfileReorganizer(profileModel, projectModel, refs.isTrue());
					utility.run();
					profileModel = utility.getResult();
					monitor.worked(1);
				}
			}
			else if( refs.isTrue()) {
				createRefactory().setByReference();
				monitor.worked(1);
			}
			if( concrete.isTrue()) {
				createRefactory().setConcrete();
				monitor.worked(1);
			}
		}

		private Refactory createRefactory() {
			return new Refactory(profileModel, Composition.merge(profileModel, projectModel));
		}

		public void updateProfileModel() {
			master.updateProfileModel(profileModel);
		}
	}

	@Override
	public boolean performFinish() {
		WizardAction action = new WizardAction();
		Jobs.runInteractive(action, null, getContainer(), getShell());
		action.updateProfileModel();
		return true;
	}
	
	private class RefactorWizardPage extends FurnishedWizardPage {
		public RefactorWizardPage() {
			super("main");
			setTitle(getWindowTitle());
			setDescription("Repair and reorganize the profile.");
		}
		
		@Override
		protected Content createContent() {
			return new Content() {

				@Override
				protected Template define() {
					return CheckboxTableViewer("options", true);
				}
				
				@Override
				protected void addBindings() {
					options.bind("options", this);
				}
			};
		}
	}
}
