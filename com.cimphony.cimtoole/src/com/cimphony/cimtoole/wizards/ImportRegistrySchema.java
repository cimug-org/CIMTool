/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package com.cimphony.cimtoole.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.poi.util.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import com.cimphony.cimtoole.CimphonyCIMToolPlugin;

import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.util.Jobs;

public class ImportRegistrySchema extends Wizard implements IImportWizard {
	
	private RegistrySchemaWizardPage main = new RegistrySchemaWizardPage();
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Schema"); 
		setNeedsProgressMonitor(true);
		main.setTitle(getWindowTitle());
		main.setDescription("Import an additional schema from the registry.");
		main.setSelected(selection);
	}
	
	@Override
    public void addPages() {
        addPage(main);        
    }
	
	@Override
	public boolean performFinish() {
		EPackage ePackage = main.getEPackage();
		String ns = ePackage.getNsURI();
		if (!ns.endsWith("#")) ns += "#";
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try{
		buf.write(ePackage.getNsURI().getBytes(Charset.forName("UTF-8")));
		buf.write('\n');
		
		IFile file = main.getFile();
		if (!file.exists())
			file.create(new ByteArrayInputStream(buf.toByteArray()), false, new NullProgressMonitor());
		else
			file.setContents(new ByteArrayInputStream(buf.toByteArray()), false, true, new NullProgressMonitor());
		Info.putProperty( file, Task.SCHEMA_NAMESPACE, ns);
		}catch (IOException ex){
			ErrorDialog.openError(this.getShell(), "IO Error", ex.getMessage(),
					new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, ex.getMessage(), ex));
			return false;
		}catch (CoreException ex){
			ErrorDialog.openError(this.getShell(), "Core Exception", ex.getMessage(),
					new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, ex.getMessage(), ex));
			return false;
		}
		return true;
	}
}
