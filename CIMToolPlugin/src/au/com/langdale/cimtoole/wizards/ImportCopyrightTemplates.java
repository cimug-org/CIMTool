/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.util.Jobs;

public class ImportCopyrightTemplates extends Wizard implements IImportWizard {
	
	private ImportCopyrightTemplatesPage main = new ImportCopyrightTemplatesPage(false);
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Copyright Templates"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import copyright templates to be utilized by the builders of the given project.");
		main.setMultiLineCopyrightSources(new String[]{"*.copyright-multi-line", "*.txt"});
		main.setSingleLineCopyrightSources(new String[]{"*.copyright-single-line", "*.txt"});
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		boolean result = false;
		IProject project = main.getMultiLineCopyrightFile().getProject();
		//
		// Must first import copyrights before updating the OWL profiles...
		IWorkspaceRunnable job = Task.importMultiLineCopyright(main.getMultiLineCopyrightFile(), main.getMultiLineCopyrightPathname());
		job = Task.chain(job, Task.importSingleLineCopyright(main.getSingleLineCopyrightFile(), main.getSingleLineCopyrightPathname()));
		//
		// Now proceed to update the OWL profile files with the new imported copyright...
		IFolder profilesFolder = Info.getProfileFolder(project);
		try {
			IResource[] resources = profilesFolder.members(false);
			for (IResource resource : resources) {
				if ((resource instanceof IFile) && ("owl".equalsIgnoreCase(resource.getFileExtension()))) {
					IFile file = (IFile) resource;
					Cache cache = CIMToolPlugin.getCache();
					OntModel model = cache.getOntologyWait(file);
					if (model != null) {
						job = Task.chain(job, Task.saveProfile(file, model));
					}
				}
			}
			//
			result = Jobs.runInteractive(job, project, getContainer(), getShell());
			Jobs.cleanBuildProject(project);
		} catch (CoreException e) {
			result = false;
			e.printStackTrace();
		}
		//
		return result;
	}

}
