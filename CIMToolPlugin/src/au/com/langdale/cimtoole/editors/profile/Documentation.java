/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import static au.com.langdale.ui.builder.Templates.Form;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.HRule;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.TextAreaNoListener;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.JenaTreeModelBase.ModelNode;
import au.com.langdale.kena.OntResource;
import au.com.langdale.profiles.ProfileModel.NaturalNode.EnumValueNode;
import au.com.langdale.profiles.ProfileModel.SortedNode;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.xmi.UML;

public class Documentation extends FurnishedEditor {
	
	private static final String CRLF = "\r\n";
	
	private ProfileEditor master;

	public Documentation(String name, ProfileEditor master) {
		super(name);
		this.master = master;
	}

	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			public Template define() {
				return Form(
					Grid(
						Group(HRule()),
						Group(Label("selected-type", "Documentation")),
						Group(TextAreaNoListener("asciidoc", true))
					)
				);
			}
			
			@Override
			protected void addBindings() {
				getText("asciidoc").addFocusListener(new FocusAdapter() {
		            @Override
		            public void focusLost(FocusEvent e) {
						if (master.getNode() instanceof SortedNode) { 
							String documentation;
							if (getText("asciidoc").getText() != null && !getText("asciidoc").getText().endsWith(CRLF)) {
								/** 
								 * For GitHub comparisons and resolving GitHub merge conflicts
								 * of profile descriptions we need to have a CRLF at the end.
								 */
								documentation = getText("asciidoc").getText() + CRLF;
							} else {
								documentation = getText("asciidoc").getText();
							}
							SortedNode pnode = (SortedNode) master.getNode();	
							pnode.setAsciiDoc(documentation);
						}
		            }
		        });
				getText("asciidoc").addKeyListener(new KeyAdapter() {
		            public void keyReleased(KeyEvent e) {
		            	markDirty();
		            }
		        });
			}

			@Override
			public void refresh() {
				getForm().setImage(IconCache.getIcons().get(master.getNode()));
				getForm().setText(master.getComment());
				
				ModelNode mnode = null;
				SortedNode snode = null;
				
				if (master.getNode() instanceof ModelNode) 
					mnode = (ModelNode) master.getNode();
				if (master.getNode() instanceof SortedNode) 
					snode = (SortedNode) master.getNode();

				OntResource base = (mnode != null ? mnode.getBase() : null);
				
				if (base != null && base.hasProperty(RDF.type)) {
					if (base.hasProperty(RDF.type, OWL.Ontology)) {
						setTextValue("selected-type", "Profile Level Documentation");
					} else if (base.isClass()) {
						OntResource subject = mnode.getSubject();
						if (subject.hasProperty(UML.hasStereotype, UML.enumeration))
							setTextValue("selected-type", "Enumeration Documentation");
						else if (subject.hasProperty(UML.hasStereotype, UML.compound))
							setTextValue("selected-type", "Compound Documentation");
						else if (subject.hasProperty(UML.hasStereotype, UML.concrete))
							setTextValue("selected-type", "Class Documentation");
						else 
							setTextValue("selected-type", "Abstract Class Documentation");
					} else if (base.isObjectProperty()) {
						if (base.hasProperty(UML.hasStereotype, UML.attribute))
							setTextValue("selected-type", "Attribute Documentation");
						else
							setTextValue("selected-type", "Association Documentation");
					} else if (base.isFunctionalProperty()) {
						setTextValue("selected-type", "Attribute Documentation");
					} else {
						setTextValue("selected-type", "Documentation");
					}
				}
			
				if( snode != null) {
					setTextValue("asciidoc", snode.getSubject().getString(UML.asciidoc, null)).setEnabled(true);
					boolean readOnly = !(snode instanceof EnumValueNode);
					getText("asciidoc").setEditable(readOnly);
				} else {
					setTextValue("asciidoc", "").setEnabled(false);
				}
			}
			
			@Override
			public void update() {
			}
		};
	}
}