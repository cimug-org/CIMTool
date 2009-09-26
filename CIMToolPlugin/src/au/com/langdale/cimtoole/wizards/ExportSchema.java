/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import au.com.langdale.cimtoole.builder.SchemaBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class ExportSchema extends Wizard implements IExportWizard {
	
	private SchemaExportPage main = new SchemaExportPage();
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Export Schema"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Export the merged schema as OWL.");
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	class InternalSchemaTask extends SchemaBuildlet implements IWorkspaceRunnable {
		IProject project = main.getProject();
		String ns = main.getNamespace();
		
		public void run(IProgressMonitor monitor) throws CoreException {
			Info.putProperty( project, Info.MERGED_SCHEMA_PATH, SchemaExportPage.SCHEMA);
			Info.putProperty( project, Info.PROFILE_NAMESPACE, ns);
			build(project.getFile(SchemaExportPage.SCHEMA), monitor);
		}
	}
	
	@Override
	public boolean performFinish() {
		if( main.isInternal())
			return Jobs.runInteractive(new InternalSchemaTask(), main.getProject(), getContainer(), getShell());
		else
			return Jobs.runInteractive(Task.exportSchema(main.getProject(), main.getPathname(), main.getNamespace()), null, getContainer(), getShell());
	}
}
