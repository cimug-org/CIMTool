/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.actions.WizardLauncher;
import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.editors.profile.PopulateBinding.LeftBinding;
import au.com.langdale.cimtoole.editors.profile.PopulateBinding.RightBinding;
import au.com.langdale.cimtoole.wizards.SearchWizard;
import au.com.langdale.cimtoole.wizards.SearchWizard.Searchable;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.jena.UMLTreeModel.PackageNode;
import au.com.langdale.jena.UMLTreeModel.SubClassNode;
import au.com.langdale.jena.UMLTreeModel.SuperClassNode;
import au.com.langdale.profiles.ProfileClass;
import au.com.langdale.profiles.ProfileModel;
import au.com.langdale.profiles.ProfileModel.Cardinality;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.TypeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode.SubTypeNode;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.xmi.UML;

import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;
import com.hp.hpl.jena.reasoner.InfGraph;
import static au.com.langdale.ui.builder.Templates.*;

public class Populate extends FurnishedEditor {
	private ProfileEditor master;
	
	LeftBinding leftBinding = new LeftBinding();
	RightBinding rightBinding = new RightBinding();
	
	public Populate(String name, ProfileEditor master) {
		super(name);
		this.master = master;
	}

	public void profileAdd(Node target, Node node, boolean link) {
		if( target instanceof ElementNode ) {
			profileAddSingle(target, node, link);
			target.structureChanged(); 
		}
		else {
			Collection args = new ArrayList();
			buildArguments(args, node);
			if(args.size() < 50 || confirm(node.toString(), args.size())) {
				for (Iterator it = args.iterator(); it.hasNext();) 
					profileAddSingle(target, (Node) it.next(), link);
				target.structureChanged(); 
			}
		}
	}

	private static void buildArguments(Collection args, Node node) {

		if((node instanceof SubClassNode) 
				|| (node instanceof SuperClassNode)
				|| (node instanceof PackageNode)) {

			Iterator it = node.iterator();
			while (it.hasNext()) 
				buildArguments(args, (Node) it.next());

		}
		else
			args.add(node);
	}
	
	public boolean confirm(String name, int many) {
		Shell shell = new Shell();
		return MessageDialog.openConfirm(
			shell,
			"CIMTool",
			name + " will add " + many + " elements to the profile. Continue?");
	}

	private void profileAddSingle(Node target, Node node, boolean link) {
		OntResource subject = node.getSubject();
		OntResource child = target.create(subject);
		if(child != null && child.isClass() && ! child.isAnon()) {
			master.getRefactory().add(child, subject, link);
		}
	}

	public void profileRemove(Node target, Node node) {
		if( node instanceof TypeNode ) 
			master.getRefactory().remove(node.getSubject());
		node.destroy();
		InfGraph ig = (InfGraph) target.getSubject().getOntModel().getGraph();
		ig.rebind();
		target.structureChanged();
	}

	@Override
	protected Content createContent() {
		
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return Form(
						Grid(
							Group( 
								Stack(
									Column(
										Stack(
											Grid(Group(
												Label("prop", "Select type and cardinality of this property."),	
												CheckBox("reference", "By Reference"))),
											Grid(Group(
												Label("class", "Select members and cardinality of this class."),
												CheckBox("concrete", "Make this class concrete")))),
										Grid(Group(
											Label("card", "Min"), Row(Field("minimum")),
											Label("Max"), Row(Field("maximum")),
											Row(
												CheckBox("require", "At least one"), 
												CheckBox("single", "At most one"),
												CheckBox("unbounded", "Unbounded"))))),
									Grid(Group(
										Label("nested", "Select members of this nested class."), 
										Right(Grid(Group(Label("local",""), Row(PushButton("named", "Change", named))))))),
									Grid(Group(
										Label("info", "Select profile members:"))),
									Grid(Group(
										Label("top", "Select classes or packages to profile."),
										ViewCheckBox("duplicates", "Allow multiple profiles per class"),
										Right( PushButton("search", "Search by name", "search", search))))
								)
							),
							Group( 
								TreeViewer("left", true), 
								TreeViewer("right", true)),
							Group( 
								Right(Row(PushButton("to-left", "Add the selected items to the profile", "left", toLeft))), 
								Row(PushButton("to-right", "Remove the selected items from the profile", "right", toRight)))
						));
			}

			@Override
			public Control realise(Composite parent) {
				Control form = super.realise(parent);
				
				TreeViewer left = getTreeViewer("left");
				//JenaTreeProvider.displayJenaTree(left, master.getSubmodel().getElementTreeModel());
				
				leftBinding.bind("left", this, master);
				rightBinding.bind("right", "duplicates", this, master);
				
				left.addSelectionChangedListener(new Target("right"));
				master.listenToDoubleClicks(left);

				TreeViewer right = getTreeViewer("right");
				right.addSelectionChangedListener(new Target("left"));
				
				return form;
			}
			
			class Target implements ISelectionChangedListener {
				private String side;

				Target(String side) {
					this.side = side;
				}
				
				public void selectionChanged(SelectionChangedEvent event) {
					boolean selected = !event.getSelection().isEmpty();
					getButton("to-" + side).setEnabled(selected);
					if(selected)
						getTreeViewer(side).setSelection(null);
				}
			}

			abstract class Gather implements SelectionListener {
				protected String side;
				
				Gather(String side) {
					this.side = side;
				}

				public void widgetSelected(SelectionEvent e) {
					ITreeSelection selected = (ITreeSelection) getTreeViewer(side).getSelection();
					TreePath[] paths = selected.getPaths();
					for(int ix = 0; ix < paths.length; ix++) 
						handle((Node)paths[ix].getLastSegment());
//					master.resetModels();
					fireUpdate();
//					doRefresh();
				}
				
				protected abstract void handle(Node node);

				public void widgetDefaultSelected(SelectionEvent e) {
					// no action
				}
			}
			
			private SelectionListener toLeft = new Gather("right") {
				@Override
				protected void handle(Node node) {
					profileAdd(master.getNode(), node, ! getButton("duplicates").getSelection());
				}
			};
			
			private SelectionListener toRight = new Gather("left") {

				@Override
				protected void handle(Node node) {
					profileRemove(master.getNode(), node);
				}
			};
			
			private SelectionListener named = new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if( master.getNode() instanceof SubTypeNode) {
						SubTypeNode node = (SubTypeNode) master.getNode();
						ElementNode enode = (ElementNode) node.getParent();
						ProfileClass profile = enode.getProfile();
						if( node.getSubject().isAnon() ) {
							OntResource baseClass = node.getProfile().getBaseClass();
							node.destroy();
							OntResource member = master.getRefactory().findOrCreateNamedProfile(baseClass);
							profile.addUnionMember(member);
//							enode.structureChanged();
						}
						else {
							profile.removeUnionMember(node.getSubject());
							profile.createUnionMember(node.getBase(), false);
//							enode.structureChanged();
						}
//						master.resetModels();
						fireUpdate();
						enode.getModel().getRoot().structureChanged();
//						doRefresh();
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// no action
				}
			};
			
			private SelectionListener search = new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					Node node = master.getNode();
					if( node instanceof CatalogNode || node instanceof EnvelopeNode) {
						SearchWizard wizard = new SearchWizard(searchArea);
						WizardLauncher.run(wizard, getSite().getWorkbenchWindow(), StructuredSelection.EMPTY);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// no action
				}
			};
			
			private Searchable searchArea = new Searchable() {
				public OntModel getOntModel() {
					return rightBinding.getOntModel();
				}

				public boolean previewTarget(Resource base) {
					return rightBinding.previewTarget(base);
				}
				
				public void selectTarget(Resource target) {
					toLeft.widgetSelected(null);
				}
			};
			
			private String kindOfProfile(boolean invert) {
				boolean anon = master.getNode().getSubject().isAnon() ^ invert;
				return anon? "Local": "Global";
			}
			
			@Override
			public void refresh() {
				Node node = master.getNode();
				getForm().setImage(IconCache.get(node));
				getForm().setText(master.getComment());

				
				if (node instanceof CatalogNode) {
					showStackLayer("top");
				}
				else if (node instanceof EnvelopeNode) {
					showStackLayer("top");
				}
				else if(node instanceof SubTypeNode) {
					getLabel("local").setText("This definition is " + kindOfProfile(false));
					getButton("named").setText( "Convert to " + kindOfProfile(true));
					showStackLayer("nested");
				}
				else if(node instanceof TypeNode) {
					TypeNode tnode = (TypeNode)node;
					boolean concrete = tnode.hasStereotype(UML.concrete);
					setButtonValue("concrete", concrete);
					refreshCardinality(tnode);
					showStackLayer("card");
					showStackLayer("class");
				}
				else if( node instanceof ElementNode) {
					ElementNode enode = (ElementNode) node;
					setButtonValue("reference", enode.isReference()).setEnabled(! enode.isDatatype());

					refreshCardinality(enode);
					
					showStackLayer("card");
					showStackLayer("prop");
				}
				else {
					showStackLayer("info");
				}
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
