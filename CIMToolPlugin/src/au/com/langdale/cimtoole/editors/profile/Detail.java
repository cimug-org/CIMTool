/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.profiles.ProfileModel.ProfileNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.ui.util.IconCache;

import com.hp.hpl.jena.ontology.OntResource;

public class Detail extends FurnishedEditor {
	private ProfileEditor master;

	public Detail(String name, ProfileEditor master) {
		super(name);
		this.master = master;
	}

	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return Form(
					Grid(
						Group(Label("Name:"), Field("name")),
						Group(Label("Profile Description")),
						Group(TextArea("notes")),
						Group(Label("Schema Description")),
						Group(DisplayArea("base-notes")),
						Group(Label("extra-label", "Description of Type")),
						Group(DisplayArea("extra-notes"))
					)
				);
			}

			@Override
			public void refresh() {
				getForm().setImage(IconCache.get(master.getNode()));
				getForm().setText(master.getComment());

				ElementNode enode = null;
				ProfileNode pnode = null;
				ModelNode   mnode = null;
				
				if (master.getNode() instanceof ModelNode) 
					mnode = (ModelNode) master.getNode();
				if (master.getNode() instanceof ProfileNode) 
					pnode = (ProfileNode) master.getNode();
				if (master.getNode() instanceof ElementNode) 
					enode = (ElementNode) master.getNode();
				
				if( mnode != null && mnode.getBase() != null) {
					setTextValue("base-notes", mnode.getBase().getComment(null));
				}
				else {
					setTextValue("base-notes", "");
				}

				if( pnode != null) {
					setTextValue("name", pnode.getName()).setEnabled(true);
					setTextValue("notes", pnode.getSubject().getComment(null)).setEnabled(true);
				}
				else {
					setTextValue("name", "").setEnabled(false);
					setTextValue("notes", "").setEnabled(false);
				}

				if(enode != null) {
					String pnotes = enode.getBaseProperty().getComment(null);
					String cnotes = enode.getBaseClass().getComment(null);
					if(pnotes != null && cnotes != null && ! pnotes.equals(cnotes)) {
						setTextValue("extra-notes", cnotes).setVisible(true);
						getLabel("extra-label").setVisible(true);
					}
					else {
						getText("extra-notes").setVisible(false);
						getLabel("extra-label").setVisible(false);
					}
				}
				else {
					getText("extra-notes").setVisible(false);
					getLabel("extra-label").setVisible(false);
				}
			}
			
			@Override
			public void update() {
				if (master.getNode() instanceof ProfileNode) { 
					ProfileNode pnode = (ProfileNode) master.getNode();	
					pnode.setName(getText("name").getText().trim());
					pnode.getSubject().setComment(getText("notes").getText(), null);
				}
			}
		};
	}
}