/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.ui.binding.TableBinding;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.util.TextUtil;
import au.com.langdale.validation.DiagnosisModel;
import au.com.langdale.validation.DiagnosisModel.DetailNode;

import au.com.langdale.kena.OntResource;

public class RepairEditor extends ModelEditor {

	private JenaTreeModelBase tree;
	private TableBinding diagnostics = new TableBinding() {
		
		private LabelProvider labelProvider = new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return IconCache.get(element);
			}
			
			@Override
			public String getText(Object element) {
				if( element instanceof DetailNode ) {
					DetailNode detailNode = (DetailNode)element;
					return TextUtil.wrap(detailNode.getDescription(), 40);
				} else
					return element.toString();
			}
		};

		@Override
		protected void configureViewer(StructuredViewer viewer) {
			viewer.setLabelProvider(labelProvider);
		}

		@Override
		protected Object getInput() {
			Node node = getNode();
			
			if( node instanceof DetailNode) {
				return new Object[] { node };
			}
			else if( node  instanceof ModelNode) {
				return node.getChildren();
			}
			else {
				return null;
			}
		}
	};
	
	@Override
	public JenaTreeModelBase getTree() {
		if( tree == null ) {
			tree = new DiagnosisModel();
			//tree.setRootResource(DiagnosisModel.DIAGNOSIS_ROOT);
			tree.setSource(getFile().getFullPath().toString());
			modelCached(null);
		}
		return tree;
	}

	public void modelCached(IResource key) {
		tree.setOntModel(models.getOntology(getFile()));
		tree.setRootResource(DiagnosisModel.DIAGNOSIS_ROOT);
		doRefresh();
	}

	public void modelDropped(IResource key) {
		close();
	}

	@Override
	protected void createPages() {
		addPage(main);
	}
	
	FurnishedEditor main = new FurnishedEditor("Diagnostics") {

		@Override
		protected Content createContent() {
			return new Content(getToolkit()) {

				@Override
				protected Template define() {
					return Form(
						Grid(
							Group(Label("Name:"), DisplayField("name")),
							Group(Label("URI:"),  DisplayField("uri")),
							Group(CheckboxTableViewer("diagnostics", true))
						)
					);
				}
				
				@Override
				public Control realise(Composite parent) {
					Control control = super.realise(parent);
					diagnostics.bind("diagnostics", this);
					return control;
				}

				@Override
				public void refresh() {
					Node node = getNode();
					getForm().setImage(IconCache.get(node));

					if( node  instanceof ModelNode) {
						OntResource subject = ((ModelNode)node).getBase();
						setTextValue("name", DiagnosisModel.label(subject));
						setTextValue("uri", subject.isAnon()? "": subject.getURI());
					}
					else {
						setTextValue("name", "");
						setTextValue("uri", "");
					}
				}
			};
		}
	};
}
