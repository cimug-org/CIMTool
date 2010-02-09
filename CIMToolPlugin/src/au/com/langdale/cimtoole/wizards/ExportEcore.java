package au.com.langdale.cimtoole.wizards;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
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

	private SchemaExportPage main = new SchemaExportPage(SCHEMA);

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
		String ns = main.getNamespace();

		public void run(IProgressMonitor monitor) throws CoreException {
			Info.putProperty( project, Info.MERGED_SCHEMA_PATH, SCHEMA);
			Info.putProperty( project, Info.SCHEMA_NAMESPACE, ns);
			build(project.getFile(SCHEMA), monitor);
		}
	}

	@Override
	public boolean performFinish() {
		if( main.isInternal())
			return Jobs.runInteractive(new InternalSchemaTask(), main.getProject(), getContainer(), getShell());
		else
			return Jobs.runInteractive(exportEcoreSchema(main.getProject(), main.getPathname(), main.getNamespace()), null, getContainer(), getShell());
	}

	public static IWorkspaceRunnable exportEcoreSchema(final IProject project,	final String pathname, final String namespace) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFolder folder = Info.getSchemaFolder(project);
				System.out.println("Getting merged Schema at "+folder.getLocation().toString());
				OntModel schema = CIMToolPlugin.getCache().getMergedOntologyWait(folder);

				Resource ecore = new EcoreTask(schema).createEcore(true, "cim", namespace);
				/*
				OntModel filledProfile = Task.fillProfile(schema, "http://example.com/profile/filled");
				ECoreGenerator gen  = new ECoreGenerator(filledProfile, schema,
						namespace, true, true,
						true);
				gen.run();
				EPackage pkg = gen.getResult();
				 */
				OutputStream output;
				try {
					output = new BufferedOutputStream( new FileOutputStream(pathname));
				}
				catch( IOException ex) {
					throw Info.error("can't write to " + pathname);
				}
				try {
					ecore.save(output, Collections.EMPTY_MAP);
				} catch (IOException e) {
					Info.error("can't write to " + pathname);
				}

			}
		};
	}


}
