/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors;

import org.eclipse.core.resources.IResource;

import au.com.langdale.jena.JenaTreeModelBase;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.xmi.UML;

import au.com.langdale.kena.OntResource;

public class InfoModelEditor extends ModelEditor {
	private JenaTreeModelBase tree;

	@Override
	public JenaTreeModelBase getTree() {
		if( tree == null ) {
			tree = new UMLTreeModel();
			tree.setRootResource(UML.global_package);
			tree.setSource(getFile().getFullPath().toString());
			modelCached(null);
		}
		return tree;
	}

	public void modelCached(IResource key) {
		tree.setOntModel(models.getOntology(getFile()));
	}

	public void modelDropped(IResource key) {
		close();
	}

	@Override
	protected void createPages() {
		addPage(main);
	}
	
	private FurnishedEditor main = new FurnishedEditor("CIM Detail") {

		@Override
		protected Content createContent() {
			return new Content(getToolkit()) {

				@Override
				protected Template define() {
					return Form(
						Grid(
							Group(Label("Name:"), DisplayField("name"), Label("Type:"), DisplayField("range"), Label("card","")),
							Group(Label("URI:"),    DisplayField("uri")),
							Group(Label("XMI ID:"), DisplayField("id")),
							Group(Label("Description:")),	
							Group(DisplayArea("comment"))
						)
					);
				}
				
				@Override
				public void refresh() {
					getForm().setImage(IconCache.get(getNode()));
					getForm().setText(getComment());
					
					OntResource subject = getSubject();
					OntResource prop;
					if( subject != null) {
						setTextValue("comment", subject.getComment(null));
						setTextValue("name", subject.getLabel(null));
						setTextValue("uri", subject.getURI());
						if( subject.isProperty())
							prop = subject;
						else
							prop = null;
						String id = subject.getString(UML.id);
						if(id != null)
							setTextValue("id", id);
						else
							setTextValue("id", "");
					}
					else {
						setTextValue("comment", "");
						setTextValue("name", "");
						setTextValue("uri", "");
						setTextValue("id", "");
						prop = null;
					}
					
					if( prop != null && prop.getRange() != null && ! prop.getRange().isAnon()) {
						setTextValue("range", prop.getRange().getLocalName());
						getLabel("card").setText( prop.isFunctionalProperty()? "0..1":"0..n");
						getText("range").setEnabled(true);
					}
					else {
						setTextValue("range", "");
						getText("range").setEnabled(false);
						getLabel("card").setText("");
					}
				}
			};
		}

		@Override
		protected void markDirty() {
			// we are a readonly view
		}
	};
}
