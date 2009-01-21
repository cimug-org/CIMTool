/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.kena.OntResource;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.HierarchyModel;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.profiles.ProfileModel.NaturalNode;
import au.com.langdale.ui.binding.JenaCheckTreeBinding;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.ui.util.IconCache;

public class Hierarchy extends FurnishedEditor {
	private ProfileEditor master;
	private SuperClassBinding bases;
	private HierarchyModel tree = new HierarchyModel();
	
	public Hierarchy(String name, ProfileEditor master) {
		super(name);
		this.master = master;
		bases = new SuperClassBinding();
	}
	
	public abstract class  BaseHierarchyBinding extends JenaCheckTreeBinding {
		protected NaturalNode nnode;
		
		public BaseHierarchyBinding() {
			super(tree);
		}

		@Override
		public void refresh() {
			if(master.getNode() instanceof NaturalNode) {
				nnode = (NaturalNode) master.getNode();
				super.refresh();
				getCheckViewer().getTree().setVisible(true);
				getCheckViewer().expandAll();
			}
			else {
				nnode = null;
				getCheckViewer().getTree().setVisible(false);
			}
		}

		@Override
		public void update() {
			if( nnode == null)
				return;
			super.update();
		}
	}
	
	public class SuperClassBinding extends BaseHierarchyBinding {
		private Set related;
		
		@Override
		protected void fillTree() {
			related = Refactory.asSet(nnode.getProfile().getSuperClasses());
			tree.setRootResource(nnode.getSubject());
			tree.setRefactory(master.getRefactory());
		}

		@Override
		protected boolean toBeChecked(Node node) {
			return related.contains(node.getSubject());
		}
		
		@Override
		protected void fetchChecks() {
			for (Iterator it = Refactory.asSet(nnode.getProfile().getSuperClasses()).iterator(); it.hasNext();) {
				OntResource clss = (OntResource) it.next();
				nnode.getProfile().removeSuperClass(clss);
			}
			super.fetchChecks();
		}

		@Override
		protected void hasBeenChecked(Node node) {
			if( ! nnode.getSubject().equals(node.getSubject()))
				nnode.getProfile().addSuperClass(node.getSubject());
		}
	}
	
	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return Form(
						Grid(
								Group(Label("Select Super Class:")),
								Group(CheckboxTreeViewer("bases"))
						)
				);
			}

			@Override
			public Control realise(Composite parent) {
				Control form = super.realise(parent);
				bases.bind("bases", this);
				master.listenToDoubleClicks(getTreeViewer("bases"));
				return form;
			}

			@Override
			public void refresh() {
				getForm().setImage(IconCache.get(master.getNode()));
				getForm().setText(master.getComment());
			}
			
			@Override
			public void update() {
				master.getNode().structureChanged();
			}
		};
	}
}