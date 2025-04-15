package au.com.langdale.cimtoole.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.inference.RuleParser.ParserException;
import au.com.langdale.kena.Format;
import au.com.langdale.validation.ProfileValidator;

public class ConsistencyChecks extends Task {
	
	public static class ProfileChecker extends Buildlet {

		@Override
		protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
			IFile file = getRelated(result, "owl");
			if( ! file.exists()) {
				clean(result, monitor);
				return;
			}
			
			CIMBuilder.removeMarkers(file);

			ProfileValidator checker = new ProfileValidator(getProfileModel(file), getBackgroundModel(file));
			try {
				checker.run();
			} catch (IOException e) {
				throw error("Failed to validate profile", e);
			} catch (ParserException e) {
				throw error("Failed to validate profile", e);
			}
			
			if( checker.hasErrors()) {
				write(checker.getLog(), null, false, result, Format.RDF_XML_WITH_NODEIDS.toFormat(), monitor);
				result.setDerived(true);				
				CIMBuilder.addMarker(file, "Profile " + file.getName() + " has consistency errors with respect to its schema");
			}
			else {
				if( result.exists())
					result.delete(false, monitor);
			}
		}

		@Override
		protected Collection getOutputs(IResource file) throws CoreException {
			if(isProfile(file))
				return Collections.singletonList(getRelated(file, "repair"));
			else
				return Collections.EMPTY_LIST;
		}
		
	}
}
