/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;

import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.Renamer;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;
import au.com.langdale.util.NSMapper;

public class NamespaceWizard extends Wizard  {
	private ProfileEditor master;
	
	public NamespaceWizard(ProfileEditor master) {
		this.master = master;
		setNeedsProgressMonitor(true);
	}
	
	public void run() {
		Shell shell = master.getSite().getWorkbenchWindow().getShell();
        WizardDialog dialog = new WizardDialog(shell, this);
        dialog.create();
        dialog.open();
	}

	@Override
    public void addPages() {
		setWindowTitle("Repair and Reorganize"); 
        addPage(new NamespaceWizardPage());        
    }
	
	public class WizardAction implements IWorkspaceRunnable {
		OntModel profileModel = master.getProfileModel();
		OntModel projectModel = master.getProjectModel();
		String ns = namespace.getText();

		public void run(IProgressMonitor monitor) throws CoreException {
			if( profileModel == null || projectModel == null)
				return;
			
			Set nss = NSMapper.extractNamespaces(profileModel);
			if( nss.contains(ns))
				throw Task.error("cannot change namespace because target already in use");
			monitor.worked(1);

			Renamer renamer = new Renamer.NamespaceChanger(profileModel, ns);
			profileModel = renamer.applyRenamings();
			monitor.worked(1);
			
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
	
	private TextBinding namespace = new TextBinding(Validators.NAMESPACE );
	
	private class NamespaceWizardPage extends FurnishedWizardPage {
		
		public NamespaceWizardPage() {
			super("main");
			setTitle(getWindowTitle());
			setDescription("Change profile namespace.");
			namespace.setText(master.getNamespace());
		}
		
		@Override
		protected Content createContent() {
			return new Content() {

				@Override
				protected Template define() {
					return Grid(
							Group(Label("Enter new namespace:")),
							Group(Field("namespace")));
				}

				@Override
				protected void addBindings() {
					namespace.bind("namespace", this);
				}
			};
		}
	}
}
