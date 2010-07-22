package au.com.langdale.cimtoole.wizards;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.builder.EcoreSchemaBuildlet;
import au.com.langdale.cimtoole.project.EcoreTask;
import au.com.langdale.cimtoole.project.Info;
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
		if( main.isInternal())
			return Jobs.runInteractive(new InternalSchemaTask(), main.getProject(), getContainer(), getShell());
		else
			try {
				return Jobs.runInteractive(exportEcoreSchema(main.getProject(), main.getPathname(), Info.getSchemaNamespace(main.getProject())), null, getContainer(), getShell());
			} catch (CoreException e) {
				e.printStackTrace();
				return false;
			}
	}

	public static IWorkspaceRunnable exportEcoreSchema(final IProject project,	final String pathname, final String namespace) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFolder folder = Info.getSchemaFolder(project);
				System.out.println("Getting merged Schema at "+folder.getLocation().toString());
				OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(folder);

				EPackage ecoreModel = new EcoreTask(schema).createEcore(true, "cim", namespace);
				URI fileURI = URI.createFileURI(pathname);
				Resource ecore = new ResourceSetImpl().createResource(fileURI);
				ecore.getContents().add(ecoreModel);
				try {
					ecore.save(Collections.EMPTY_MAP);
				} catch (IOException e) {
					Info.error("can't write to " + pathname);
				}

			}
		};
	}


}
