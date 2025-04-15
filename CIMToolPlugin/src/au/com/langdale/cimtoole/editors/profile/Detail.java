/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import static au.com.langdale.ui.builder.Templates.DisplayArea;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Form;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.TextAreaNoListener;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.EnumValueNode;
import au.com.langdale.profiles.ProfileModel.SortedNode;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;

public class Detail extends FurnishedEditor {
	
	private static final String CRLF = "\r\n";
	
	private ProfileEditor master;

	public Detail(String name, ProfileEditor master) {
		super(name);
		this.master = master;
	}

	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			public Template define() {
				return Form(
					Grid(
						Group(Label("Name:"), Field("name")),
						Group(Label("Profile Description")),
						Group(TextAreaNoListener("notes", true)),
						Group(Label("base-label", "Schema Description")),
						Group(DisplayArea("base-notes", true)),
						Group(Label("extra-label", "Description of Type")),
						Group(DisplayArea("extra-notes"))
					)
				);
			}
			
			@Override
			protected void addBindings() {
				getText("notes").addFocusListener(new FocusAdapter() {
		            @Override
		            public void focusLost(FocusEvent e) {
						if (master.getNode() instanceof SortedNode) { 
							String notes;
							if (getText("notes").getText() != null && !getText("notes").getText().endsWith(CRLF)) {
								/** 
								 * For GitHub comparisons and resolving GitHub merge conflicts
								 * of profile descriptions we need to have a CRLF at the end.
								 */
								notes = getText("notes").getText() + CRLF;
							} else {
								notes = getText("notes").getText();
							}
							SortedNode pnode = (SortedNode) master.getNode();	
							pnode.setComment(notes);
						}
		            }
		        });
				getText("notes").addKeyListener(new KeyAdapter() {
		            public void keyReleased(KeyEvent e) {
		            	markDirty();
		            }
		        });
			}

			@Override
			public void refresh() {
				getForm().setImage(IconCache.getIcons().get(master.getNode()));
				getForm().setText(master.getComment());
				
				ElementNode enode = null;
				SortedNode snode = null;
				ModelNode mnode = null;
				
				if (master.getNode() instanceof ModelNode) 
					mnode = (ModelNode) master.getNode();
				if (master.getNode() instanceof SortedNode) 
					snode = (SortedNode) master.getNode();
				if (master.getNode() instanceof ElementNode) 
					enode = (ElementNode) master.getNode();
				
				if( mnode != null && mnode.getBase() != null && mnode.getBase() != mnode.getSubject()) {
					setTextValue("base-notes", mnode.getBase().getComment(null)).setEnabled(true);
				}
				else {
					setTextValue("base-notes", "").setEnabled(false);
				}

				if( snode != null) {
					setTextValue("name", snode.getName()).setEnabled(true);
					setTextValue("notes", snode.getSubject().getComment(null)).setEnabled(true);
					boolean readOnly = !(snode instanceof EnumValueNode);
					getText("notes").setEditable(readOnly);
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
				if (master.getNode() instanceof SortedNode) { 
					SortedNode pnode = (SortedNode) master.getNode();	
					pnode.setName(getText("name").getText().trim());
				}
			}
		};
	}
}