package au.com.langdale.cimtoole.editors;

import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Form;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.PushButton;
import static au.com.langdale.ui.builder.Templates.RadioButton;
import static au.com.langdale.ui.builder.Templates.Row;
import static au.com.langdale.ui.builder.Templates.TreeViewer;
import static au.com.langdale.jena.TreeModelBase.label;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.wizards.SearchWizard;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.JenaTreeBinding;
import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.MappingTree;
import au.com.langdale.jena.MappingTree.DatatypeNode;
import au.com.langdale.jena.MappingTree.EquivNode;
import au.com.langdale.jena.MappingTree.FunctionalNode;
import au.com.langdale.jena.MappingTree.MappingNode;
import au.com.langdale.jena.MappingTree.SuperClassNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validators;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.ui.util.WizardLauncher;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.vocabulary.RDFS;

public class MappingEditor extends ModelEditor {

	private MappingTree tree  = new MappingTree();

	@Override
	public void init(IEditorSite site, IEditorInput editorInput)throws PartInitException {
		super.init(site, editorInput);
		fetchModels();
	}

	private void fetchModels() {
		fromBinding.fetchModel();
		toBinding.fetchModel();
		tree.setOntModel(models.getOntology(getFile()));
		tree.setSource(getFile().getFullPath().toString());
		tree.setRootResource(UML.global_package);
	}

	public void modelCached(IResource key) {
		fetchModels();
		doRefresh();
	}

	public void modelDropped(IResource key) {
		close();
	}

	@Override
	public JenaTreeModelBase getTree() {
		return tree;
	}

	@Override
	protected void createPages() {
		addPage(main);
	}

	public class UMLTreeBinding extends JenaTreeBinding implements Searchable {

		public UMLTreeBinding() {
			super(new UMLTreeModel());
			setRootVisible(false);
		}
		
		public void bindAfter(String name, Assembly plumbing, Object after) {
			super.bind(name, plumbing, after);
			getViewer().addSelectionChangedListener(plumbing.selectionChangedlistener);
			listenToSelection(getViewer());
		}
		
		public final SelectionListener search = new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				SearchWizard wizard = new SearchWizard(UMLTreeBinding.this);
				WizardLauncher.run(wizard, getSite().getWorkbenchWindow(), StructuredSelection.EMPTY);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		};
		
		public void fetchModel() {
			getTree().setOntModel(models.getProjectOntology(Info.getSchemaFolder(getFile().getProject())));
			getTree().setRootResource(UML.global_package);
		}
		
		public OntModel getOntModel() {
			return getTree().getOntModel();
		}

		public Node findNode(Resource target) {
			Node[] path = getTree().findPathTo(target, false);
			return path != null? path[path.length-1]: null;
		}

		public void previewTarget(Node node) {
			getViewer().setSelection(new TreeSelection(new TreePath(node.getPath(false))), true);
		}

		public String getDescription() {
			return "Search the schema for a class to be mapped.";
		}

		public Property getCriterion() {
			return  ResourceFactory.createProperty(RDFS.label);
		}	
		
		private OntResource getSelectedResource() {
			ITreeSelection selected = (ITreeSelection) getViewer().getSelection();
			if( ! selected.isEmpty()) {
				Node node = (Node) selected.getPaths()[0].getLastSegment();
				return node.getSubject();
			}
			else
				return null;
		}

		public OntResource getResource() {
			return selected;
		}
		
		public void setResource(OntResource selected) {
			this.selected = selected;
		}
		
		private OntResource selected;
		
		public void refresh() {
			if( selected != null && ! selected.equals(getSelectedResource())) {
				Node node = findNode(selected);
				if( node != null )
					previewTarget(node);
			}
			
		}
		
		public void reset() {
			
		}

		public void update() {
			selected = getSelectedResource();
		}

		public String validate() {
			return null;
		}
	}
	

	@Override
	public void doSave(IProgressMonitor monitor) {
		OntModel model = tree.getOntModel();
		if( model != null) {
			try {
				ResourcesPlugin.getWorkspace().run(Task.saveMappings(getFile(), model), monitor);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}
		super.doSave(monitor);
	}
	
	UMLTreeBinding fromBinding = new UMLTreeBinding();
	UMLTreeBinding toBinding = new UMLTreeBinding();
	TextBinding name = new TextBinding(Validators.NCNAME_OPT) {
		protected String createSuggestion() {
			return label(fromBinding.getResource()) + "." + label(toBinding.getResource());
		};
	};

	
	private MappingNode focusNode;
	
	private SelectionListener add = new SelectionListener() {

		public void widgetSelected(SelectionEvent e) {
			focusNode.create();
			markDirty();
			tree.getRoot().structureChanged();
			doRefresh();
		}

		public void widgetDefaultSelected(SelectionEvent e) {

		}
	};
	
	private SelectionListener remove = new SelectionListener() {

		public void widgetSelected(SelectionEvent e) {
			focusNode.remove();
			markDirty();
			tree.getRoot().structureChanged();
			doRefresh();
		}

		public void widgetDefaultSelected(SelectionEvent e) {

		}
		
	};

    FurnishedEditor main = new FurnishedEditor("") {
        
        @Override
        protected Content createContent() {
            return new Content(getToolkit(), false) {

				@Override
				protected Template define() {
				    return Form( 
				    	Grid( 
				    		Group(
				    			Label("Select CIM (common) class"),
				    			Row(PushButton("search_from", "Search 'from' Class", "search")),
				    			Label("Select extension class or type"),
				    			Row(PushButton("search_to", "Search 'to' Class", "search"))),
				    		Group(
				    			TreeViewer("left"), null,
				    			TreeViewer("right")), null,
				    		Group(Grid(Group(
				    			Label("Property Name:"),	
					    		Field("name"),
				    			Row(
				    				RadioButton("functional", "Functional Property", "functional"),
				    				RadioButton("equiv", "Equivalent class", "equiv"),
				    				RadioButton("superclass", "Superclass", "superclass"),
				    				PushButton("add", "Add Mapping", "plus"), 
				    				PushButton("remove", "Remove Mapping", "minus")
				    			)
				    		)))
				    	)
				    );
				}
				
				private MappingNode lastOutlineNode;

				protected void addBindings() {
					fromBinding.bindAfter("left", this, null);
					toBinding.bindAfter("right", this, fromBinding);
					name.bindAfter("name", this, toBinding);
					addListener("add", add);
					addListener("remove", remove);
					addListener("search_from", fromBinding.search);
					addListener("search_to", toBinding.search);
					getOutline().addSelectionChangedListener(selectionChangedlistener);
				};
				
				public void update() {
					Node node = getNode();
					if( node != lastOutlineNode && (node instanceof MappingNode)) {
						focusNode = (MappingNode)node;
						name.setText(focusNode.getName());
						fromBinding.setResource(focusNode.getRelated());
						toBinding.setResource(focusNode.getSubject());
						lastOutlineNode = focusNode;
						
					}
					else {
						focusNode = tree.makeNode(
								name.getText(), 
								toBinding.getResource(), 
								fromBinding.getResource(), 
								getButton("functional").getSelection(), 
								getButton("equiv").getSelection());
					}
				}
				
				public void refresh() {
					getForm().setImage(IconCache.getIcons().get("equiv"));
					getForm().setText("Create mappings between schema classes");
					getButton("remove").setEnabled(focusNode != null && focusNode.extant());
					getButton("add").setEnabled( !(focusNode == null || focusNode.extant() || focusNode.isProperty() && name.getText().isEmpty()));
					getText("name").setEnabled(focusNode != null && focusNode.isProperty());
					getButton("search_from").setEnabled(fromBinding.getOntModel() != null);
					getButton("search_to").setEnabled(toBinding.getOntModel() != null);
					
					boolean radios = focusNode != null && ! (focusNode instanceof DatatypeNode);
					getButton("functional").setEnabled(radios);
					getButton("equiv").setEnabled(radios);
					getButton("superclass").setEnabled(radios);
					getButton("functional").setSelection(focusNode instanceof FunctionalNode);
					getButton("equiv").setSelection(focusNode instanceof EquivNode);
					getButton("superclass").setSelection(focusNode instanceof SuperClassNode);
				}
            };
        }
    };
}
