package com.cimphony.cimtoole.buildlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import com.cimphony.cimtoole.CimphonyCIMToolPlugin;
import com.cimphony.cimtoole.ecore.EcoreGenerator;
import com.cimphony.cimtoole.ecore.EcoreGenerator.Index;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.project.Task;
import au.com.langdale.kena.OntModel;

public class EcoreBuildlet extends ProfileBuildlet {

    public EcoreBuildlet() {
        super("ecore");
    }

    @Override
    protected void build(IFile result, IProgressMonitor monitor) throws CoreException {
        IFile file = Task.getRelated(result, "owl");
        boolean preserveNS = Task.getPreferenceOption(Task.PRESERVE_NAMESPACES);
        String namespace = preserveNS? Task.getSchemaNamespace(file): Task.getProperty(file, Task.PROFILE_NAMESPACE);
        EcoreGenerator generator = getGenerator(Task.getProfileModel(file), Task.getBackgroundModel(file), namespace, Task.getProperty(file, Task.PROFILE_NAMESPACE), preserveNS, result.getProject());
        generator.run();
        // Use file name for top level package name.
        EPackage schema = generator.getResult();
        if (schema.getName() == null)
        	schema.setName(result.getName().split("\\.")[0]);
        ResourceSet metaResourceSet = new ResourceSetImpl();
        // Register XML Factory implementation to handle .ecore files
        /*metaResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "ecore", new  XMLResourceFactoryImpl())
            */;
        // Create empty resource with the given URI
        org.eclipse.emf.ecore.resource.Resource metaResource =
            metaResourceSet.createResource(URI.createURI(result.getFullPath().toString()));
        metaResource.getContents().add(schema);
        
        ByteArrayOutputStream cache = new ByteArrayOutputStream();
        try {
            metaResource.save(cache, Collections.EMPTY_MAP);
        } catch (IOException ex) {
        	throw new CoreException(new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, "Error writing Cimphony Profile XML file", ex));
        }
    	if (!result.exists())
    		result.create(new ByteArrayInputStream(cache.toByteArray()), false, monitor);
    	else
    		result.setContents(new ByteArrayInputStream(cache.toByteArray()), false,true, monitor);
        
    }

    public static EcoreGenerator getGenerator(OntModel profileModel, OntModel backgroundModel, String namespace, String profileNamespace, boolean preserveNS, IProject project) throws CoreException {
        return new EcoreGenerator(profileModel, backgroundModel, namespace, profileNamespace, preserveNS, true, true, project);
    }
}