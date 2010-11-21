package com.cimphony.cimtoole.wizards;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ErrorSupportProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.cimphony.cimtoole.CimphonyCIMToolPlugin;
import com.cimphony.cimtoole.buildlet.EcoreSchemaBuildlet;
import com.cimphony.cimtoole.ecore.EcoreGenerator;
import com.cimphony.cimtoole.ecore.EcoreTask;
import com.cimphony.cimtoole.util.CIMToolEcoreUtil;
import com.hp.hpl.jena.vocabulary.OWL;


import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.cimtoole.wizards.SchemaExportPage;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;
import au.com.langdale.util.Jobs;

public class ExportEcore extends Wizard implements IExportWizard {

	public static final String SCHEMA = "schema.ecore";
	public static final String FILE_EXT = "ecore";

	private SchemaExportPage main = new SchemaExportPage(SCHEMA, FILE_EXT);

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Export Schema"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Export the merged schema as Ecore.");
		main.setSelected(selection);
	}

	@Override
	public void addPages() {
		addPage(main);        
	}

	class InternalSchemaTask extends EcoreSchemaBuildlet implements IWorkspaceRunnable {
		IProject project = main.getProject();

		public void run(IProgressMonitor monitor) throws CoreException {
			Info.putProperty( project, Info.MERGED_SCHEMA_PATH, SCHEMA);
			Info.putProperty( project, Info.SCHEMA_NAMESPACE, Info.getSchemaNamespace(project));
			build(project.getFile(SCHEMA), monitor);
		}
	}

	@Override
	public boolean performFinish() {
		String path;
		if( main.isInternal())
			path = main.getProject().getLocation().toString()+"/"+SCHEMA;
		else
			path = main.getPathname();
		try{
		//return Jobs.runInteractive(new InternalSchemaTask(), main.getProject(), getContainer(), getShell());
			return Jobs.runInteractive(exportEcoreSchema(main.getProject(), path, Info.getSchemaNamespace(main.getProject())), null, getContainer(), getShell());
		} catch (CoreException e) {
				ErrorDialog.openError(
						getShell(),
						"Error Exporting Ecore",
						e.getMessage(),
						new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, e.getMessage()));e.printStackTrace();
				return false;
		}
	}

	public static IWorkspaceRunnable exportEcoreSchema(final IProject project,	final String pathname, final String namespace) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFolder folder = Info.getSchemaFolder(project);
				OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(folder);
				
				EcoreGenerator gen = new EcoreGenerator(schema, schema, namespace, namespace, true, true, true, true);
				gen.run();
				EPackage ecoreModel = gen.getResult();
				URI fileURI = URI.createFileURI(pathname);
				Resource ecore = new ResourceSetImpl().createResource(fileURI);
				ecore.getContents().add(ecoreModel);
				try {
					ecore.save(Collections.EMPTY_MAP);
				} catch (IOException e) {
					Info.error("can't write to " + pathname);
				}
				project.refreshLocal(1, new NullProgressMonitor());

			}
		};
	}


}
