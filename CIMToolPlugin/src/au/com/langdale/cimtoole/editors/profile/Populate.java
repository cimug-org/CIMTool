/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.editors.profile;


import static au.com.langdale.ui.builder.Templates.Form;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Span;
import static au.com.langdale.ui.builder.Templates.Label;
import static au.com.langdale.ui.builder.Templates.PushButton;
import static au.com.langdale.ui.builder.Templates.Right;
import static au.com.langdale.ui.builder.Templates.Row;
import static au.com.langdale.ui.builder.Templates.Stack;
import static au.com.langdale.ui.builder.Templates.TreeViewer;
import static au.com.langdale.ui.builder.Templates.ViewCheckBox;

import java.util.Collection;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import au.com.langdale.cimtoole.editors.ProfileEditor;
import au.com.langdale.cimtoole.editors.profile.PopulateBinding.LeftBinding;
import au.com.langdale.cimtoole.editors.profile.PopulateBinding.RightBinding;
import au.com.langdale.cimtoole.wizards.SearchWizard;
import au.com.langdale.jena.TreeModelBase;
import au.com.langdale.jena.TreeModelBase.Node;
import au.com.langdale.profiles.ProfileModel.CatalogNode;
import au.com.langdale.profiles.ProfileModel.EnvelopeNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode;
import au.com.langdale.profiles.ProfileModel.NaturalNode.ElementNode;
import au.com.langdale.profiles.ProfileModel.SortedNode;
import au.com.langdale.ui.builder.FurnishedEditor;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.util.IconCache;
import au.com.langdale.ui.util.WizardLauncher;


public class Populate extends FurnishedEditor {
	private ProfileEditor master;
	
	LeftBinding leftBinding = new LeftBinding();
	RightBinding rightBinding = new RightBinding();
	
	public Populate(String name, ProfileEditor master) {
		super(name);
		this.master = master;
	}
	
	public SortedNode getProfileNode() {
		return (SortedNode) master.getNode();
	}

	public boolean confirm(String name, int many) {
		Shell shell = new Shell();
		return MessageDialog.openConfirm(
			shell,
			"CIMTool",
			name + " will add " + many + " elements to the profile. Continue?");
	}

	@Override
	protected Content createContent() {
		
		return new Content(master.getToolkit()) {

			@Override
			protected Template define() {
				return 
					Form(
						Grid(
							Group( 
								Stack(
									Span(
										Label("objectprop", "Add associated classes to the profile or"),
										PushButton("jumpa", "Goto Restriction Page"),
										Right(PushButton("anon-left", "Create a local, anonymous profile class", "anontype"))),
									Span(
										Label("dataprop", ""),
										PushButton("jumpb", "Goto Restriction Page")),
									Span(
										Label("class", "Select members of this class."),	
										ViewCheckBox("showsuper","Show superclass members"),
										ViewCheckBox("showsub","Show subclass members")
									),
									Span(
										Label("top", "Select classes or packages to profile."),
										ViewCheckBox("duplicates", "Allow multiple profiles per class"),
										Right( PushButton("search", "Search Schema", "search"))
									),
									Label("nothing", "Select another item in the profile outline")
								)
							),
							Group( 
								TreeViewer("left", true), 
								TreeViewer("right", true)),
							Group( 
								Right(
									Row(
										PushButton("all-left", "Add the selected classes to the profile including members", "leftfast"),
										PushButton("to-left", "Add the selected items to the profile", "left")
									)
								), 
								Row(PushButton("to-right", "Remove the selected items from the profile", "right")))
						)
					);
			}

			@Override
			protected void addBindings() {
				
				leftBinding.bind("left", this, master);
				rightBinding.bind("right", "duplicates", "showsuper", "showsub", this, master);
				leftBinding.listenTo(rightBinding);
				rightBinding.listenTo(leftBinding);
				
				toRight.bind("to-right", "left");
				toLeft.bind("to-left", "right");
				anonLeft.bind("anon-left", "right");
				allLeft.bind("all-left", "right");
				addListener("search", search);
				addListener("jumpa", jump);
				addListener("jumpb", jump);
			}
			
			abstract class Picker implements ISelectionChangedListener, SelectionListener {
				private Button button;
				private TreeViewer viewer;

				public void bind( String name, String source) {
					button = getButton(name);
					viewer = getTreeViewer(source);
					button.addSelectionListener(this);
					viewer.addSelectionChangedListener(this);
					button.setEnabled(false);
				}

				public void widgetSelected(SelectionEvent e) {
					ITreeSelection selected = (ITreeSelection) viewer.getSelection();
					TreePath[] paths = selected.getPaths();
					for(int ix = 0; ix < paths.length; ix++) 
						handle((Node)paths[ix].getLastSegment());
					fireUpdate();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// no action
				}
				
				public void selectionChanged(SelectionChangedEvent event) {
					button.setEnabled(!event.getSelection().isEmpty());
				}
				
				protected abstract void handle(Node node);
			}
			
			private Picker allLeft = new Picker() {
				protected void handle(Node node) {
					SortedNode target = getProfileNode();
					Collection args = target.profileExpandArgs(node);
					if(args.size() < 50 || confirm(node.toString(), args.size())) {
						target.profileAddAllDeep(args); 
					}
				}
			};
			
			private Picker anonLeft = new Picker() {
				protected void handle(Node node) {
					SortedNode target = getProfileNode();
					target.profileAddAnon(node); 
				}
			};
			
			private Picker toLeft = new Picker() {
				protected void handle(Node node) {
					SortedNode target = getProfileNode();
					Collection args = target.profileExpandArgs(node);
					if(args.size() < 50 || confirm(node.toString(), args.size())) {
						target.profileAddAll(args); 
					}
				}
			};
			
			private Picker toRight = new Picker() {
				protected void handle(Node node) {
					getProfileNode().profileRemove(node);
				}
			};
			
			private SelectionListener search = new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					Node node = master.getNode();
					if( node instanceof CatalogNode || node instanceof EnvelopeNode) {
						SearchWizard wizard = new SearchWizard(rightBinding);
						WizardLauncher.run(wizard, getSite().getWorkbenchWindow(), StructuredSelection.EMPTY);
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// no action
				}
			};
			
			private final SelectionListener jump = new SelectionListener() {
				
				public void widgetSelected(SelectionEvent e) {
					master.setActivePageByName("Restriction");
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			};
			
			@Override
			public void refresh() {
				Node node = master.getNode();
				getForm().setImage(IconCache.getIcons().get(node));
				getForm().setText(master.getComment());
				
				if (node instanceof CatalogNode || node instanceof EnvelopeNode) {
					showStackLayer("top");
				}
				else if(node instanceof NaturalNode) {
					showStackLayer("class");
				}
				else if( node instanceof ElementNode) {
					ElementNode enode = (ElementNode)node;
					if(enode.isDatatype() ) {
						showStackLayer("dataprop");
						setTextValue("dataprop", "The datatype " + TreeModelBase.label(enode.getBase().getRange()) + " will be added to the profile.");
					}
					else {
						showStackLayer("objectprop");
					}
				}
				else {
					showStackLayer("nothing");
				}
			}
		};
	}
}
