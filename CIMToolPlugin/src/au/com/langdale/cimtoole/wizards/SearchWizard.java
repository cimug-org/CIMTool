/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.splitmodel.SearchIndex;
import au.com.langdale.ui.binding.ListBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.builder.FurnishedWizard;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.validation.Validation;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.ModelFactory;

import au.com.langdale.kena.Resource;

public class SearchWizard extends FurnishedWizard implements IWorkbenchWizard {
	
	public SearchWizard(Searchable searchArea) {
		setSearchArea(searchArea);
	}
	
	public SearchWizard() {}

	public interface Searchable {
		OntModel getOntModel();
		void selectTarget(Resource target);
		boolean previewTarget(Resource target);
	}

	public static OntModel findModel( ISelection selection) {
		OntModel model = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			Object item = structured.getFirstElement();
			if( item instanceof IResource) {
				IResource resource = (IResource)item;
				IProject project = resource.getProject();
				if( project != null) 
					model = CIMToolPlugin.getCache().getMergedOntology(Info.getSchemaFolder(project));
			}
			else if (item instanceof ModelNode) {
				ModelNode node = (ModelNode) item;
				model = node.getModel().getOntModel();
			}
		}
		return model;
	}

	public class Indexer extends SearchIndex implements IWorkspaceRunnable {
		public Indexer() {
			super(100);
		}
		
		public void run(IProgressMonitor monitor) throws CoreException {
			scan(model);
		}

		public Set locate(String name) {
			return super.locate(name, model);
		}
	}
	
	public class Matches extends ListBinding {
		private boolean indexed;

		@Override
		protected void configureViewer(StructuredViewer viewer) {
//			viewer.setContentProvider(new MatchProvider());
		}

		@Override
		protected Object getInput() {
			if( ! indexed) {
				indexed = true;
				run(indexer, null);
			}
			
			String prefix = getParent().getValue().toString();
			if( prefix.length() == 0)
				return Collections.EMPTY_SET;
			else
				return indexer.match(prefix);
		}	
	}
	
	private Searchable searchArea= EMPTY_AREA;
	private Indexer indexer = new Indexer();
	private TextBinding search = new TextBinding(Validation.NONE);
	private Matches matches = new Matches();
	private OntModel model;
	private Resource target;
	private boolean found;
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Search Schema");
		page.setTitle(getWindowTitle());
		page.setDescription("Search for a class or property in the schema");
		setNeedsProgressMonitor(true);
	}
	
	public void setSearchArea(Searchable area) {
		searchArea = area;
		model = area.getOntModel();
	}

	private static final OntModel EMPTY_MODEL = ModelFactory.createMem();
	
	private static final Searchable EMPTY_AREA = new Searchable() {
		public void selectTarget(Resource target) {}
		public OntModel getOntModel() {	return EMPTY_MODEL;	}
		public boolean previewTarget(Resource target) {return true;}
	};
	
	@Override
	public void addPages() {
		addPage(page);
	}
	
	IWizardPage page = new FurnishedWizardPage("search") {

		@Override
		protected Content createContent() {
			return new Content() {

				@Override
				protected Template define() {
					
					return Column(
							Label("Search string:"),
							Field("search"),
							Label("Matches"),
							TableViewer("matches"),
							DisplayField("namespace")
						);
				}
				
				@Override
				public Control realise(Composite parent) {
					Control panel = super.realise(parent);
					search.bind("search", this);
					matches.bind("matches", this, search);
					return panel;
				}
				
				@Override
				public void refresh() {
					Object value = matches.getValue();
					if(value != null) {
						Set resources = indexer.locate(value.toString());
						target = (Resource) resources.iterator().next();
						setTextValue("namespace", target.toString());
						found = searchArea.previewTarget(target);
						
					}
					else {
						setTextValue("namespace", "");
						target = null;
						found = false;
						
					}
				}
				
				@Override
				public String validate() {
					// TODO Auto-generated method stub
					return found? null: "";
				}
			};
		}
	};

	@Override
	public boolean performFinish() {
		if( found ) 
			searchArea.selectTarget(target);
		
		return found;
	}

}
