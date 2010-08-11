/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import au.com.langdale.cimtoole.editors.profile.Detail;
import au.com.langdale.cimtoole.editors.profile.Hierarchy;
import au.com.langdale.cimtoole.editors.profile.Populate;
import au.com.langdale.cimtoole.editors.profile.Stereotype;
import au.com.langdale.cimtoole.editors.profile.Summary;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.wizards.SearchWizard;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeProvider;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.ui.util.WizardLauncher;

import com.hp.hpl.jena.vocabulary.RDFS;

public class ProfileEditor extends ModelEditor {
	private ProfileModel tree;
	private Refactory refactory;
	private OntModel backgroundModel, rawBackgroundModel, diagnosticModel, profileModel;
	private boolean hasDiagnostics;
	
	@Override
	protected void createPages() {
		addPage(new Populate("Add/Remove", this));
		addPage(new Hierarchy("Hierarchy", this));
		addPage(new Detail("Detail", this));
		addPage( new Stereotype("Stereotypes", this));
		addPage(new Summary("Summary", this));
	}

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)throws PartInitException {
		super.init(site, editorInput);
		fetchModels();
	}
	
	@Override
	public JenaTreeModelBase getTree() {
		if( tree == null ) {
			tree = new ProfileModel();
			tree.setSource(getFile().getFullPath().toString());
			resetModels();
		}
		return tree;
	}

	@Override
	public JenaTreeProvider getProvider() {
		return new JenaTreeProvider(true);
	}

	public void modelCached(IResource key) {
		// FIXME: check dirty state here and tell user about conflict
		if( isDirty())
			return;
		fetchModels();
	}

	public void modelDropped(IResource key) {
		close();
	}

	private void fetchModels() {
		rawBackgroundModel = models.getProjectOntology(Info.getSchemaFolder(getFile().getProject()));
		OntModel rawProfileModel = models.getOntology(getFile());
		if(rawProfileModel != null && rawBackgroundModel != null) {
			profileModel = Task.fixupProfile(getFile(), Composition.copy(rawProfileModel), rawBackgroundModel);
		}
		else
			profileModel = null;
		IFile diagnostics = Info.getRelated(getFile(), "diagnostic");
		hasDiagnostics = diagnostics.exists();
		if( hasDiagnostics )
			diagnosticModel = models.getOntology(diagnostics);
		else 
			diagnosticModel = null;
		resetModels();
	}

	public void resetModels() {
		if( rawBackgroundModel == null || profileModel == null || tree == null || hasDiagnostics && diagnosticModel == null) {
			searchAction.setEnabled(false);
			return;
		}

		if( hasDiagnostics)
			backgroundModel = Composition.simpleMerge( rawBackgroundModel, diagnosticModel);
		else
			backgroundModel = rawBackgroundModel;
		
		tree.setOntModel(profileModel);
		tree.setBackgroundModel(backgroundModel);
		refactory = new Refactory(profileModel, backgroundModel);
		searchAction.setEnabled(true);
		doRefresh();
	}
	
	public OntModel getProjectModel() {
		return backgroundModel;
	}
	
	public OntModel getProfileModel() {
		return profileModel;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if( profileModel != null) {
			try {
				ResourcesPlugin.getWorkspace().run(Task.saveProfile(getFile(), profileModel), monitor);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}
		super.doSave(monitor);
	}

	public boolean isLoaded() {
		return profileModel != null && backgroundModel != null;
	}
	
	public void updateProfileModel(OntModel profileModel) {
		this.profileModel = profileModel;
		markDirty();
		resetModels();
	}

	public Refactory getRefactory() {
		return refactory;
	}
	
	public String getNamespace() {
		if( tree != null)
			return tree.getNamespace();
		else
			return null;
	}
	
	@Override
	protected void configureOutline(ModelOutliner outline) {
		outline.getSite().getActionBars().getToolBarManager().add(searchAction);
		
	}
	
	private IAction searchAction = new Action("Search Profile", ImageDescriptor.createFromImage(IconCache.getIcons().get("prosearch"))) {
		@Override
		public void run() {
			if(isLoaded()) {
				SearchWizard wizard = new SearchWizard(searchArea);
				WizardLauncher.run(wizard, getSite().getWorkbenchWindow(), StructuredSelection.EMPTY);
			}
		}
	};
	
	private Searchable searchArea = new Searchable() {
		
		public Property getCriterion() {
			return  ResourceFactory.createProperty(RDFS.label);
		}

		public OntModel getOntModel() {
			return profileModel;
		}

		public void previewTarget(Node node) {
			ModelOutliner outline = getOutline();
			if( outline != null) 
				outline.drillTo(node.getPath(true));
		}

		public Node findNode(Resource target) {
			Node[] path = getTree().findPathTo(target, true);
			return path != null? path[path.length-1]: null;
		}

		public String getDescription() {
			return "Search the profile for classes or their members by name.";
		}
	};
}
