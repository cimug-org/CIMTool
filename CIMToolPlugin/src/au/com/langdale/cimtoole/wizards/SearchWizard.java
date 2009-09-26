/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.Column;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.TableViewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.LocalNameIndex;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Property;
import au.com.langdale.kena.PropertyIndex;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.SearchIndex;
import au.com.langdale.ui.binding.ListBinding;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.builder.FurnishedWizard;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.validation.Validation;

public class SearchWizard extends FurnishedWizard implements IWorkbenchWizard {
	
	public SearchWizard(Searchable searchArea) {
		this(searchArea, searchArea.getCriterion());
	}
	
	private SearchWizard(Searchable searchArea, Property prop) {
		this.searchArea = searchArea;
		model = searchArea.getOntModel();
		indexer = prop == null? new LocalNameIndex(): new PropertyIndex(prop);
	}

	public interface Searchable {
		OntModel getOntModel();
		Property getCriterion();
		void previewTarget(Node node);
		Node findNode(Resource target);
		String getDescription();
	}

	public class Matches extends ListBinding {
		private static final int LIMIT = 100;
		private boolean indexed;

		@Override
		protected void configureViewer(StructuredViewer viewer) {
//			viewer.setContentProvider(new MatchProvider());
		}

		@Override
		protected Object getInput() {
			if( ! indexed) {
				indexed = true;
				run(job, null);
			}
			
			String prefix = getParent().getValue().toString();
			if( prefix.length() == 0)
				return Collections.EMPTY_SET;
			else {
				Collection result = indexer.match(prefix, LIMIT);
				if(result.size() == 1)
					setValue(result.iterator().next());
				return result;
			}
		}	
	}

	public class SubMatches extends ListBinding {

		@Override
		protected void configureViewer(StructuredViewer viewer) {
			viewer.setLabelProvider(new SubMatchLabel());
		}

		@Override
		protected Object getInput() {
			
			Object value = getParent().getValue();
			if( value != null ) {
				String word = value.toString();
				List result = findNodes(indexer.locate(word, model));
				if( result.size() == 1)
					setValue(result.get(0));
				else
					Collections.sort(result);
				return result;
			}
			else
				return Collections.EMPTY_LIST;
		}

		private List findNodes(Set resources) {
			ArrayList result = new ArrayList(resources.size());
			Iterator it = resources.iterator();
			while( it.hasNext()) {
				Node node = searchArea.findNode((OntResource)it.next());
				if( node != null)
					result.add(node);
			}
			return result;
		}	
	}
	
	public class SubMatchLabel extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return IconCache.get(element);
		}

		@Override
		public String getText(Object element) {
			if( element instanceof Node ) {
				Node node = (Node) element;
				String text = node.toString();
				Node parent = node.getParent();
				if( parent != null)
					text = text + " (" + parent.getName() + ")";
				return text;
			}
			
			if( element == null)
				return "";
			
			return element.toString();
		}
	}

	private Searchable searchArea;
	private SearchIndex indexer;
	private TextBinding search = new TextBinding(Validation.NONE);
	private Matches matches = new Matches();
	private SubMatches submatches = new SubMatches();
	private OntModel model;
	
	private IWorkspaceRunnable job = new IWorkspaceRunnable() {
		public void run(IProgressMonitor monitor) throws CoreException {
			indexer.scan(model);
		}
	};
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("CIMTool Search");
		page.setTitle(getWindowTitle());
		page.setDescription(searchArea.getDescription());
		setNeedsProgressMonitor(true);
	}
	
	private static final OntModel EMPTY_MODEL = ModelFactory.createMem();
	
	public static final Searchable EMPTY_AREA = new Searchable() {
		public OntModel getOntModel() {	return EMPTY_MODEL;	}
		public void previewTarget(Node node) {}
		public Property getCriterion() { return null; }
		public Node findNode(Resource target) { return null; }
		public String getDescription() { return "";	}
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
							Label("Select a matching term:"),
							TableViewer("matches"),
							Label("Select a matching item:"),
							TableViewer("submatches")
						);
				}
				
				@Override
				public Control realise(Composite parent) {
					Control panel = super.realise(parent);
					search.bind("search", this);
					matches.bind("matches", this, search);
					submatches.bind("submatches", this, matches);
					return panel;
				}
				
				@Override
				public String validate() {
					return submatches.getValue() != null? null: "";
				}
			};
		}
	};

	@Override
	public boolean performFinish() {
		Node node = (Node) submatches.getValue();
		if( node != null ) 
			searchArea.previewTarget(node);
		
		return node != null;
	}

}
