/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.UMLTreeModel;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.UMLTreeModel.ClassNode;
import au.com.langdale.jena.UMLTreeModel.EnumClassNode;
import au.com.langdale.jena.UMLTreeModel.SubClassNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.ui.binding.JenaCheckTreeBinding;
import au.com.langdale.ui.binding.FilteredContentProvider.Filter;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.ui.util.IconCache;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;

public class Refine extends FurnishedEditor {
	private ProfileEditor master;

	public Refine(String name, ProfileEditor master) {
		super(name);
		this.master = master;
	}
	
	private static class SubClassFilter implements Filter {
		public boolean allow(Object value) {
			return value instanceof ClassNode || value instanceof EnumClassNode
			|| value instanceof SubClassNode;
		}

		public boolean flatten(Object value) {
			return false;
		}
		
		public boolean prune(Object value) {
			return false;
		}
	}
	
	public abstract class  BaseRefinementBinding extends JenaCheckTreeBinding {
		protected ElementNode enode;
		
		public BaseRefinementBinding() {
			super(new UMLTreeModel());
			setFilter(new SubClassFilter());
		}

		@Override
		public void refresh() {
			if(master.getNode() instanceof ElementNode) {
				enode = (ElementNode) master.getNode();
				super.refresh();
				getCheckViewer().getTree().setVisible(true);
				getCheckViewer().expandAll();
			}
			else {
				enode = null;
				getCheckViewer().getTree().setVisible(false);
			}
		}

		@Override
		public void update() {
			if( enode == null)
				return;
			super.update();
		}
	}
	
	public class UnionBinding extends BaseRefinementBinding  {

		private Set members;

		@Override
		protected void fillTree() {
			getTree().setRootResource(enode.getProfile().getBaseClass());
			getTree().setOntModel(master.getProjectModel());
		}

		@Override
		protected void fillChecks() {
			members = new HashSet(enode.getProfile().getUnionMembers());
			super.fillChecks();
		}

		@Override
		protected boolean toBeChecked(Node node) {
			OntResource subject = node.getSubject();
			if( ! subject.isClass())
				return false;
			
			OntClass member = master.getRefactory().findNamedProfile(subject.asClass());
			if( member == null)
				return false;
			return members.contains(member);
		}

		@Override
		protected void fetchChecks() {
			members = new HashSet(enode.getProfile().getUnionMembers());
			
			super.fetchChecks();
			
			for (Iterator it = members.iterator(); it.hasNext();) {
				OntResource remain = (OntResource) it.next();
				enode.getProfile().removeUnionMember(remain);
			}
		}

		@Override
		protected void hasBeenChecked(Node node) {
			OntResource subject = node.getSubject();
			if( subject.isClass()) {
				OntClass sub = master.getRefactory().findNamedProfile(subject.asClass());
				if( sub != null && ! members.remove(sub)) {
					enode.getProfile().addUnionMember(sub);
				}
				else if( sub == null) {
					enode.getProfile().addUnionMember(master.getRefactory().findOrCreateNamedProfile(subject.asClass()).getSubject());
				}
			}
		}
	}
	
	public class RefinementBinding extends BaseRefinementBinding {

		@Override
		protected void fillTree() {
			getTree().setRootResource(enode.getBaseProperty().getRange());
			getTree().setOntModel(master.getProjectModel());
		}

		@Override
		protected boolean toBeChecked(Node node) {
			return node.getSubject().equals(enode.getProfile().getBaseClass());
		}

		@Override
		protected void hasBeenChecked(Node node) {
			enode.getProfile().setBaseClass(node.getSubject());
		}

	}
	

	private UnionBinding subs = new UnionBinding();
	private RefinementBinding bases = new RefinementBinding();


	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return Form(
						Grid(
								Group( Row(CheckBox("reference", "By Reference"), CheckBox("require", "At least one"), CheckBox("functional", "At most one"))),
								Group(Label("Refine Base Class:")),
								Group(CheckboxTreeViewer("bases")),
								Group(Label("Check Allowed Subclasses:")),
								Group(CheckboxTreeViewer("subs", true))
						)
				);
			}

			@Override
			public Control realise(Composite parent) {
				Control form = super.realise(parent);
				subs.bind("subs", this);
				bases.bind("bases", this);
				return form;
			}

			@Override
			public void refresh() {
				getForm().setImage(IconCache.get(master.getNode()));
				getForm().setText(master.getComment());
				if( master.getNode() instanceof ElementNode) {
					ElementNode enode = (ElementNode) master.getNode();
					setButtonValue("reference", enode.isReference()).setEnabled(! enode.isDatatype());
					setButtonValue("functional", enode.isFunctional()).setEnabled(! enode.isAlwaysFunctional());
					setButtonValue("require", enode.isRequired()).setEnabled(enode.canBeRequired());
				}
			}
			
			@Override
			public void update() {
				if( master.getNode() instanceof ElementNode) {
					ElementNode enode = (ElementNode) master.getNode();
					enode.setReference(getButton("reference").getSelection());
					enode.setFunctional(getButton("functional").getSelection());
					enode.setRequired(getButton("require").getSelection());
				}
			}
		};
	}
}