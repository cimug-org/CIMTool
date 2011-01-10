/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.CheckboxTreeViewer;
import static au.com.langdale.ui.builder.Templates.Form;
import static au.com.langdale.ui.builder.Templates.Column;
import static au.com.langdale.ui.builder.Templates.Span;
import static au.com.langdale.ui.builder.Templates.PushButton;
import static au.com.langdale.ui.builder.Templates.Row;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.Stack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.jena.JenaCheckTreeBinding;
import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.kena.OntResource;
import au.com.langdale.profiles.HierarchyModel;
import au.com.langdale.profiles.ProfileClass;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileModel.GeneralTypeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.TypeNode;
import au.com.langdale.profiles.Refactory;
import au.com.langdale.profiles.ProfileModel.Cardinality;
import au.com.langdale.profiles.ProfileModel.NaturalNode;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.xmi.UML;

public class Hierarchy extends FurnishedEditor {
	private ProfileEditor master;
	private SuperClassBinding bases;
	private UnionBinding assoc;
	
	public Hierarchy(String name, ProfileEditor master) {
		super(name);
		this.master = master;
		bases = new SuperClassBinding();
		assoc = new UnionBinding();
	}
	
	public abstract class TypeBinding extends JenaCheckTreeBinding {

		public TypeBinding(HierarchyModel tree) {
			super(tree);
			setRootVisible(false);
		}
		
		protected abstract boolean isEnabled();
		protected abstract Set findRelated();
		protected abstract void addRelated(OntResource related);
		protected abstract void removeRelated(OntResource related);
		
		protected HierarchyModel getHierarchyModel() {
			return (HierarchyModel) getTree();
		}

		@Override
		public void refresh() {
			if(isEnabled()) {
				super.refresh();
				getCheckViewer().getTree().setVisible(true);
				getCheckViewer().expandAll();
			}
			else {
				getCheckViewer().getTree().setVisible(false);
			}
		}

		@Override
		public void update() {
			if( isEnabled())
				super.update();
		}
		
		private Set related;

		@Override
		protected void fillChecks() {
			related = findRelated();
			super.fillChecks();
		}

		@Override
		protected boolean toBeChecked(Node node) {
			return related.contains(node.getSubject());
		}
		
		@Override
		protected void fetchChecks() {
			for (Iterator it = findRelated().iterator(); it.hasNext();) {
				OntResource clss = (OntResource) it.next();
				removeRelated(clss);
			}
			super.fetchChecks();
		}

		@Override
		protected void hasBeenChecked(Node node) {
			OntResource related = node.getSubject();
			addRelated(related);
		}
	}
	
	public class  SuperClassBinding extends TypeBinding {
		
		public SuperClassBinding() {
			super( new HierarchyModel(false));
		}
		
		@Override
		protected void fillTree() {
			getHierarchyModel().setRefactory(getTarget().getProfileModel().getRefactory());
			getHierarchyModel().setRootResource(getTarget().getBaseClass());
		}

		protected boolean isEnabled() {
			return master.getNode() instanceof NaturalNode;
		}

		private NaturalNode getTarget() {
			return (NaturalNode) (isEnabled()? master.getNode(): null);
		}

		protected Set findRelated() {
			return Refactory.asSet(getTarget().getProfile().getSuperClasses());
		}
		
		protected void addRelated(OntResource related) {
			if( ! getTarget().getSubject().equals(related))
				getTarget().getProfile().addSuperClass(related);
		}
		
		protected void removeRelated(OntResource related) {
			getTarget().getProfile().removeSuperClass(related);
		}
	}
	
	public class  UnionBinding extends TypeBinding {
		
		public UnionBinding() {
			super( new HierarchyModel(true));
		}
		
		@Override
		protected void fillTree() {
			getHierarchyModel().setRefactory(getTarget().getProfileModel().getRefactory());
			getHierarchyModel().setRootResource(getTarget().getBaseClass());
		}

		public boolean isEnabled() {
			return master.getNode() instanceof ElementNode;
		}

		private ElementNode getTarget() {
			return (ElementNode) (isEnabled()? master.getNode(): null);
		}

		@Override
		protected Set findRelated() {
			Set result = new HashSet();
			for( Object member: getTarget().getProfile().getUnionMembers())
				result.add(((ProfileClass)member).getSubject());
			return result;
		}

		@Override
		protected void addRelated(OntResource related) {
			getTarget().getProfile().addUnionMember(related);
		}

		@Override
		protected void removeRelated(OntResource related) {
			getTarget().getProfile().removeUnionMember(related);
		}
	}
	
	@Override
	protected Content createContent() {
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return 
					Form(
						Stack(
							Column(
								Stack(
									Span(
										Label("class", "Restrict this class."),
										CheckBox("concrete", "Make this class concrete")
									),
									Span(
										Label("prop", "Restrict this property."),	
										CheckBox("reference", "By Reference")
									)
								),
							    Span(
									Label("card", "Min"), 
									Row(Field("minimum")),
									Label("Max"), 
									Row(Field("maximum")),
									Row(
									    CheckBox("require", "At least one"), 
										CheckBox("single", "At most one"),
										CheckBox("unbounded", "Unbounded"))
								),
								Stack(
									Column(
										Label("Select Super Class:"),
										CheckboxTreeViewer("bases")
									),
									Column(
										Label("Select Associated Class or Classes:"),
										CheckboxTreeViewer("assoc", true)
									),
									Label("datatype", "Datatype")
								)
							),
							Span(
								Label("nothing", "Select a class or property to restrict or "), 
								PushButton("jump", "Goto Add/Remove Page")
							)
						)
					);
			}
			
			private void bindTree( JenaCheckTreeBinding binding, String name) {
				binding.bind(name, this);
				TreeViewer viewer = getTreeViewer(name);
				master.listenToDoubleClicks(viewer);
				master.listenToSelection(viewer);
			}

			@Override
			protected void addBindings() {
				bindTree(bases, "bases");
				bindTree(assoc, "assoc");
				addListener("jump", jump);
			}
			
			private final SelectionListener jump = new SelectionListener() {
				
				public void widgetSelected(SelectionEvent e) {
					master.setActivePageByName("Add/Remove");
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			};

			@Override
			public void refresh() {
				Node node = master.getNode();
				getForm().setImage(IconCache.getIcons().get(node));
				getForm().setText(master.getComment());
				if(node instanceof ElementNode) {
					showStackLayer("prop");
					ElementNode enode = (ElementNode)node;
					if(enode.isDatatype() ) {
						setTextValue("datatype", "Datatype is " + TreeModelBase.label(enode.getBase().getRange()));
						showStackLayer("datatype");
					}
					else {
					    showStackLayer("assoc");
					}
					setButtonValue("reference", enode.isReference()).setEnabled(! enode.isDatatype());
					refreshCardinality(enode);
					
				}
				else if( node instanceof GeneralTypeNode ) {
					GeneralTypeNode tnode = (GeneralTypeNode)node;
					if( ! tnode.isEnumerated()) {
						showStackLayer("class");
						showStackLayer("bases");
						boolean concrete = tnode.hasStereotype(UML.concrete);
						setButtonValue("concrete", concrete).setEnabled(tnode.getSubject().isURIResource());
						refreshCardinality(tnode);
					}
					else
						showStackLayer("nothing");
				}
				else
					showStackLayer("nothing");
			}
			
			@Override
			public void update() {
				Node node = master.getNode();
				if( node instanceof ElementNode) {
					ElementNode enode = (ElementNode) node;
					enode.setReference(getButton("reference").getSelection());
					updateCardinality(enode);
				}
				else if(node instanceof TypeNode) {
					TypeNode tnode = (TypeNode) node;
					boolean selection = getButton("concrete").getSelection();
					if( ! selection ) {
						tnode.setMinCardinality(0);
						tnode.setMaxCardinality(Integer.MAX_VALUE);
					}
					tnode.setStereotype(UML.concrete, selection);
					if( selection )
						updateCardinality(tnode);
						
				}
				node.structureChanged();
			}

			private void refreshCardinality(Cardinality cnode) {
				setButtonValue("single", (cnode.getMaxCardinality() == 1)).setEnabled(cnode.isMaxVariable());
				setButtonValue("require", (cnode.getMinCardinality() > 0)).setEnabled(cnode.isMinVariable());
				setButtonValue("unbounded", (cnode.getMaxCardinality() == Integer.MAX_VALUE)).setEnabled(cnode.isMaxVariable());
				setTextValue("minimum", ProfileModel.cardString(cnode.getMinCardinality())).setEnabled(cnode.isMinVariable());
				setTextValue("maximum", ProfileModel.cardString(cnode.getMaxCardinality())).setEnabled(cnode.isMaxVariable());
			}

			private void updateCardinality(Cardinality node) {
				int max = node.getMaxCardinality();
				int min = node.getMinCardinality();

				int newMax, newMin;

				try {
					newMax = ProfileModel.cardInt(getText("maximum").getText());
					newMin = ProfileModel.cardInt(getText("minimum").getText());
				}
				catch( NumberFormatException e) {
					return;
				}

				if( newMax == max ) {

					if(getButton("single").getSelection()) 
						newMax = 1;
					else if( max == 1 ) 
						newMax = Integer.MAX_VALUE;
				}
				
				if( newMax == max ) {
					if(getButton("unbounded").getSelection())
						newMax = Integer.MAX_VALUE;
					else if (max == Integer.MAX_VALUE)
						newMax = 1;
				}
				
				if( newMin == min ) {
				   if(! getButton("require").getSelection())
					   newMin = 0;
				   else if( min == 0 )
					   newMin = 1;
				}
				
				node.setMaxCardinality(newMax);
				node.setMinCardinality(newMin);
			}
		};
	}
}
