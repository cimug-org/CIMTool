/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
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
import au.com.langdale.jena.Models;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.MESSAGE;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.ui.binding.JenaTreeProvider;
import au.com.langdale.ui.util.Tickler;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

public class ProfileEditor extends ModelEditor {
	private ProfileModel tree;
	private OntModel profileModel;
	private Refactory refactory;
	private OntModel backgroundModel;
	
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
			tree.setRootResource(MESSAGE.Message);
			tree.setNamespace(getFileNamespace()); // this is used in Node.create()
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
		backgroundModel = models.getProjectOntology(Info.getSchemaFolder(getFile().getProject()));
		OntModel raw = models.getOntology(getFile());
		profileModel = raw != null? Models.copy(raw) : null;
		resetModels();
	}
	
	public void resetModels() {
		if( backgroundModel == null || profileModel == null || tree == null)
			return;
		
		tree.setOntModel(profileModel);
		tree.setBackgroundModel(backgroundModel);
		tree.setNamespace(getFileNamespace());
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

	private String getFileNamespace() {
		try {
			return Info.getProperty(Info.PROFILE_NAMESPACE, getFile());
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
