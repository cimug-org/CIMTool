/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package com.cimphony.cimtoole.wizards;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.CheckboxTableViewer;
import static au.com.langdale.ui.builder.Templates.Field;
import static au.com.langdale.ui.builder.Templates.Grid;
import static au.com.langdale.ui.builder.Templates.Group;
import static au.com.langdale.ui.builder.Templates.Label;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.binding.TextBinding;
import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.FurnishedWizardPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.workspace.ResourceUI.ProjectBinding;

public class RegistrySchemaWizardPage extends FurnishedWizardPage {
	
	public static final Validator RegistryEPackage(final boolean required) {
		return new Validator() {
			@Override
			public String validate(String schemaUri) {
				if( schemaUri.length() == 0)
					if(required)
						return "A URI is required";
					else
						return null;
				EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(schemaUri);
				if(ePackage == null)
					return "The URI is not in the registry";
				return null;
			}
		};
	}

	public class DialogTemplate implements Template{

		protected int style;
		protected String text;
		protected String name;
		protected String target;
		
		DialogTemplate(int style, String name, String text, String target) {
			this.name = name;
			this.style = style;
			this.text = text;
			this.target = target;
		}
		
		protected void listen(Button widget, final Assembly assembly){
			widget.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {}
				
				public void widgetSelected(SelectionEvent e) {
					Text entry = assembly.getText(target);
			    	EPackageSelectionDialog dialog = new EPackageSelectionDialog(RegistrySchemaWizardPage.this.getShell());
			    	dialog.setBlockOnOpen(true);
			    	dialog.setInitialPattern("?", FilteredItemsSelectionDialog.FULL_SELECTION);
			    	int result = dialog.open();
			    	if (result == Dialog.OK){
			    		Object[] schemas = dialog.getResult();
			    		for (Object o :schemas){
			    			if (o instanceof String){
			    				String fullName = (String)o;
			    				String uri = fullName.substring(fullName.indexOf("('")+2, fullName.indexOf("')"));
								entry.setText(uri);
			    			}
			    		}
			    	}
				}
			});

		}
		
		public Control realise(Composite parent, Assembly assembly) {
			Button widget = assembly.getToolkit().createButton(parent, text, style);
			register(widget, assembly);
			listen(widget, assembly);
			return widget;
		}
		
		protected void register(Control widget, Assembly assembly) {
			if(name != null)
				assembly.putControl(name, widget);
		}

		
	}
	
	private final boolean expectNewProject;

	public RegistrySchemaWizardPage(boolean expectNewProject) {
		super("schema");
		this.expectNewProject = expectNewProject;
	}

	public RegistrySchemaWizardPage() {
		this(false);
	}

	private IFile file;
	boolean importing;

	private TextBinding source = new TextBinding(RegistryEPackage(true));
	
	private ProjectBinding projects = new ProjectBinding();
	private IProject newProject;

	public void setSelected(IStructuredSelection selection) {
		projects.setSelected(selection);
	}

	public void setNewProject(IProject newProject) {
		this.newProject = newProject;
	}

	public EPackage getEPackage() {
		return EPackage.Registry.INSTANCE.getEPackage(source.getText());
	}
	
	public IFile getFile(){
		return file;
	}

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				return Grid(
					Group(Label("Namespace URI:"), Field("source"), new DialogTemplate(SWT.PUSH, "registryButton", "Browse Registered", "source")),
					Group(Label("Project")), 
					expectNewProject? null :Group(CheckboxTableViewer("projects")),
					Group(CheckBox("replace", "Replace existing schema.")),
					Group(Label("* Set this under Windows > Preferences > CIMTool"))
				);
			}

			@Override
			protected void addBindings() {
				if( ! expectNewProject )
					projects.bind("projects", this);
				source.bind("source", this);
			}

			@Override
			public String validate() {
				if( source.getText().length() == 0)
					if(expectNewProject)
						return null;
					else
						return "A schema is required";
				if (getEPackage() == null)
					return "A valid schema is required";
				IProject project = expectNewProject? newProject: projects.getProject();
				file = Info.getSchemaFolder(project).getFile(getEPackage().getName()+".ecore-registry");
				if( file == null )
					return "A project resource name is required";

				boolean exists = file.exists();
				getButton("replace").setEnabled(exists);
				if( exists && ! getButton("replace").getSelection())
					return "A schema named " + getEPackage().getName() + " already exists. " +
					"Check option to replace.";

				return null;
			}
		};
	}
}