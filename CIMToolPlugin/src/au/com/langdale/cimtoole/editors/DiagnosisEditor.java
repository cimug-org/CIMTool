/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import org.eclipse.core.resources.IResource;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.validation.DiagnosisModel;
import au.com.langdale.validation.DiagnosisModel.DetailNode;

import au.com.langdale.kena.OntResource;
import static au.com.langdale.ui.builder.Templates.*;

public class DiagnosisEditor extends ModelEditor {

	private JenaTreeModelBase tree;

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
							Group(DisplayArea("comment"))
						)
					);
				}

				@Override
				public void refresh() {
					Node node = getNode();
					getForm().setImage(IconCache.getIcons().get(node));

					if( node  instanceof ModelNode) {
						OntResource subject = ((ModelNode)node).getBase();
						setTextValue("name", DiagnosisModel.label(subject));
						setTextValue("uri", subject.isAnon()? "": subject.getURI());
						
						if( node instanceof DetailNode) {
							setTextValue("comment", ((DetailNode)node).getDescription());
						}
						else {
							setTextValue("comment", "This item has " + node.getChildren().size() + " diagnostic reports." );
						}
					}
					else {
						setTextValue("name", "");
						setTextValue("uri", "");
						setTextValue("comment", "");
					}
				}
			};
		}
		
	};
}
