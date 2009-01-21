/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.kena.OntModel;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.profiles.Remapper;
import au.com.langdale.profiles.Reorganizer;
import au.com.langdale.ui.binding.BooleanBinding;
import au.com.langdale.ui.binding.BooleanModel;
import au.com.langdale.ui.binding.BooleanModel.BooleanValue;
import au.com.langdale.ui.builder.FurnishedWizard;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.plumbing.Template;

public class RefactorWizard extends FurnishedWizard  {
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
	
	BooleanBinding options = new BooleanBinding() {
		@Override
		protected BooleanModel[] getFlags() {
			return new BooleanModel[] {refs, concrete, remap, reorg};
		}
		
		@Override
		public String validate() {
			if( refs.isTrue() || concrete.isTrue() || remap.isTrue() || reorg.isTrue())
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
		String namespace = master.getNamespace();

		public void run(IProgressMonitor monitor) throws CoreException {
			if( profileModel == null || projectModel == null)
				return;

			if( remap.isTrue()) {
				Remapper utility = new Remapper(profileModel, projectModel);
				utility.run();
				monitor.worked(1);
			}
			if( reorg.isTrue()) {
				Reorganizer utility = new Reorganizer(profileModel, projectModel, namespace, refs.isTrue());
				utility.run();
				profileModel = utility.getResult();
				monitor.worked(1);
			}
			else if( refs.isTrue()) {
				Refactory refactory = new Refactory(profileModel, projectModel, namespace);
				refactory.setByReference();
				monitor.worked(1);
			}
			if( concrete.isTrue()) {
				Refactory refactory = new Refactory(profileModel, projectModel, namespace);
				refactory.setConcrete();
				monitor.worked(1);
			}
		}

		public void updateProfileModel() {
			master.updateProfileModel(profileModel);
		}
	}

	@Override
	public boolean performFinish() {
		WizardAction action = new WizardAction();
		run(action, null);
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
				public Control realise(Composite parent) {
					Control viewer = super.realise(parent);
					options.bind("options", this);
					return viewer;
				}
			};
		}
	}
}
