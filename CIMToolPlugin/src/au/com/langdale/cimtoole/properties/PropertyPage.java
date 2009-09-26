/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import au.com.langdale.cimtoole.builder.SchemaBuildlet;
import au.com.langdale.cimtoole.project.Info;
import au.com.langdale.ui.builder.FurnishedPropertyPage;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.util.Jobs;
import au.com.langdale.validation.Validation;
import static au.com.langdale.ui.builder.Templates.*;

public class PropertyPage extends FurnishedPropertyPage {

	@Override
	protected Content createContent() {
		return new Content() {

			@Override
			protected Template define() {
				IResource resource = getResource();
				if(Info.isSchema(resource ))
					return defineSchemaPage();
				else if(Info.isProfile(resource))
					return defineProfilePage();
				else if(Info.isInstance(resource) || Info.isSplitInstance(resource))
					return defineInstancePage();
				else if(Info.isIncremental(resource))
					return defineIncrementalPage();
				else if(resource instanceof IProject)
					return defineProjectPage();
				else
					return Label("No properties available for this resource.");
			}


			private Template defineInstancePage() {
				return Grid(
						Group(  Label("Namespace URI:"), 
								new Property(Info.INSTANCE_NAMESPACE, Validation.NAMESPACE)),
						Group(	Label("Profile Name:"), 
								new Property(Info.PROFILE_PATH, Validation.OptionalFileWithExt("owl")))
				);
			}


			private Template defineIncrementalPage() {
				return Grid(
						Group(  Label("Namespace URI:"), 
								new Property(Info.INSTANCE_NAMESPACE, Validation.NAMESPACE)),
						Group(	Label("Base Model Name:"), 
								new Property(Info.BASE_MODEL_PATH, Validation.OptionalFileAnyExt()))
				);
			}

			private Template defineProfilePage() {
				return Grid(
						Group(	Label("Namespace URI:"), 
								new Property(Info.PROFILE_NAMESPACE, Validation.NAMESPACE))
				);
			}

			private Template defineProjectPage() {
				return Grid(
						Group(Label("Merged Schema Output")),
						Group(	Label("Namespace URI:"), 
								new Property(Info.SCHEMA_NAMESPACE, Validation.NAMESPACE)),
						Group(	Label("File Name:"), 
								new Property(Info.MERGED_SCHEMA_PATH, Validation.OptionalFileWithExt("merged-owl")))
				);
			}


			private Template defineSchemaPage() {
				return Grid(
						Group(	Label("Namespace URI:"), 
								new Property(Info.SCHEMA_NAMESPACE, Validation.NAMESPACE)),
						Group(	Label("Warning: changing this namespace will affect existing profiles."))
				);
			}
			
			@Override
			public void update() {
				try {
					IResource resource = getResource();
					if( resource instanceof IProject) {
						
						String merged = Info.getProperty(resource, Info.MERGED_SCHEMA_PATH);
						if( merged != null && merged.length() != 0) {
							IFile file = resource.getProject().getFile(merged);
							Jobs.runJob(new SchemaBuildlet().asRunnable(file, false), resource, "Generating merged OWL");
						}

					}
					else
						resource.touch(null);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
				
			}
		};
	}
}
