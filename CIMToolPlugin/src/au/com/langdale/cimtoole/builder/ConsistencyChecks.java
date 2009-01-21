package au.com.langdale.cimtoole.builder;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.project.Cache;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.kena.OntModel;
import au.com.langdale.validation.ConsistencyChecker;

public class ConsistencyChecks extends Task {

	public static OntModel getBackgroundModel(IFile file) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		IFolder schema = getSchemaFolder(file.getProject());
		return cache.getMergedOntologyWait(schema);
	}

	public static OntModel getProfileModel(IFile file) throws CoreException {
		Cache cache = CIMToolPlugin.getCache();
		return cache.getOntologyWait(file);
	}
	
	public static class ProfileChecker extends Buildlet {

		@Override
		protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
			IFile file = getRelated(result, "owl");
			if( ! file.exists()) {
				clean(result, monitor);
				return;
			}
			
			CIMBuilder.removeMarkers(file);

			ConsistencyChecker checker = new ConsistencyChecker(getProfileModel(file), getBackgroundModel(file), getProperty(PROFILE_NAMESPACE, file));
			checker.run();
			
			if( checker.errorCount() > 0) {
				write(checker.getLog(), null, false, result, "TURTLE", monitor);
				result.setDerived(true);				
				CIMBuilder.addMarker(file, "Profile " + file.getName() + " has " + checker.errorCount() + " consistency error(s) with respect to its schema");
			}
			else {
				if( result.exists())
					result.delete(false, monitor);
			}
		}

		@Override
		protected Collection getOutputs(IResource file) throws CoreException {
			if(isProfile(file))
				return Collections.singletonList(getRelated(file, "diagnostic"));
			else
				return Collections.EMPTY_LIST;
		}
		
	}
}
