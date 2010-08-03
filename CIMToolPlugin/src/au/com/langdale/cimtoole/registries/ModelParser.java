package au.com.langdale.cimtoole.registries;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.kena.OntModel;

public interface ModelParser {

	public OntModel getModel();
	public void setFile(IFile file);
	public void run()  throws IOException, CoreException;
	
}
