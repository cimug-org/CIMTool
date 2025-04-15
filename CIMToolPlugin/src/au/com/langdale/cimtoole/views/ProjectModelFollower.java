package au.com.langdale.cimtoole.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import au.com.langdale.cimtoole.project.Cache.CacheListener;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.ModelMinder;
import au.com.langdale.kena.OntModel;

public abstract class ProjectModelFollower extends SelectionFollower implements CacheListener {
	private ModelMinder models = new ModelMinder(this);
	private IProject activeProject;

	public abstract void selectModel(OntModel model);

	@Override
	public void dispose() {
		models.dispose();
		super.dispose();
	}

	public void selectProject(IProject project) {
		if( activeProject == null || ! project.equals(activeProject)) {
			//System.out.println("ProjectModelView switching to " + project);
			activeProject = project;
			viewActiveProject();
		}
	}

	private void viewActiveProject() {
		OntModel merger = models.getProjectOntology(Info.getSchemaFolder(activeProject));
		selectModel(merger);
	}

	public void modelCached(IResource key) {
		if( activeProject != null) {
			viewActiveProject();
			followSelection(getSite().getPage().getSelection());
		}
	}

	public void modelDropped(IResource key) {
		// ignored
	}
}
