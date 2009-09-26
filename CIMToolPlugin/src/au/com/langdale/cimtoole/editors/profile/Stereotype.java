/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.OntModelAdapters;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.ui.binding.BooleanBinding;
import au.com.langdale.ui.binding.BooleanModel;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.xmi.UML;
import static au.com.langdale.ui.builder.Templates.*;

public class Stereotype extends FurnishedEditor {
	private ProfileEditor master;
	StereotypeBinding stereos;

	public Stereotype(String name, ProfileEditor master) {
		super(name);
		this.master = master;
		stereos = new StereotypeBinding();
	}

	public class StereotypeBinding extends BooleanBinding {
		@Override
		protected BooleanModel[] getFlags() {
			return OntModelAdapters.findAnnotations(UML.Stereotype, UML.hasStereotype, master);
		}
	}

	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return Form(
						Grid(
							Group(Label("Select stereotypes to apply to this element:")),
							Group(CheckboxTableViewer("stereos", true))
						)
				);
			}

			@Override
			public Control realise(Composite parent) {
				Control form = super.realise(parent);
				stereos.bind("stereos", this);
				return form;
			}

			@Override
			public void refresh() {
				Node node = master.getNode();
				getForm().setImage(IconCache.get(node));
				getForm().setText(master.getComment());
				node.changed();
			}
		};
	}
}