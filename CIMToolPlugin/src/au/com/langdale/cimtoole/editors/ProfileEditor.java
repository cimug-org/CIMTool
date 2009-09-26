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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
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
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.ui.binding.JenaTreeProvider;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;

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
		//addPage(new Refine("Refine Type", this));
		addPage( new Stereotype("Stereotypes", this));
		addPage(new Summary("Summary", this));
	}

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)throws PartInitException {
		super.init(site, editorInput);
		fetchModels();
		//tickler.start();
	}
	
//	private static Tickler tickler = new Tickler(){
//		private int last_count;
//
//		@Override
//		protected void action() {
//			int create_count = TreeModelBase.create_count;
//			int amount = create_count - last_count;
//			last_count = create_count;
//			if( amount > 0 )
//				System.out.println("Nodes created: " + amount);
//		}
//	};

	@Override
	public JenaTreeModelBase getTree() {
		if( tree == null ) {
			tree = new ProfileModel();
			tree.setNamespace(getFileNamespace()); // this is used in Node.create()
			tree.setRootResource(MESSAGE.profile);
			tree.setSource(getFile().getFullPath().toString());
			resetModels();
		}
		return tree;
	}

	@Override
	protected JenaTreeProvider getProvider() {
		return new JenaTreeProvider(true);
	}

	@Override
	protected void hookOutline(ModelOutliner outline) {
		super.hookOutline(outline);
		listenToDoubleClicks(outline.getTreeViewer());
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
		if(rawProfileModel != null) {
			profileModel = Composition.copy(rawProfileModel);
			String envname;
			try {
				envname = Info.getProperty(getFile(), Info.PROFILE_ENVELOPE);
			} catch (CoreException e) {
				envname = "Profile";
			}
			Task.initProfile(profileModel, envname);
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
		if( rawBackgroundModel == null || profileModel == null || tree == null || hasDiagnostics && diagnosticModel == null)
			return;

		if( hasDiagnostics)
			backgroundModel = Composition.simpleMerge( rawBackgroundModel, diagnosticModel);
		else
			backgroundModel = rawBackgroundModel;
		tree.setNamespace(getFileNamespace());
		tree.setOntModel(profileModel);
		tree.setBackgroundModel(backgroundModel);
		refactory = new Refactory(profileModel, backgroundModel, tree.getNamespace());
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
				ResourcesPlugin.getWorkspace().run(Task.saveProfile(getFile(), profileModel, tree.getNamespace()), monitor);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}
		super.doSave(monitor);
	}

	private IDoubleClickListener drill = new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent event) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			Node node = (Node) selection.getFirstElement();
			OntResource subject = node.getSubject();
			drillTo(subject);
		}
	};
	
	public void listenToDoubleClicks(StructuredViewer source) {
		source.addDoubleClickListener(drill);
	}

	public boolean isLoaded() {
		return profileModel != null && backgroundModel != null;
	}
	
	public void updateProfileModel(OntModel profileModel) {
		this.profileModel = profileModel;
		markDirty();
		resetModels();
	}

	public String getFileNamespace() {
		try {
			return Info.getProperty(getFile(), Info.PROFILE_NAMESPACE);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
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
}
