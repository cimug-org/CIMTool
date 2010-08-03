package com.cimphony.cimtoole.buildlet;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import com.cimphony.cimtoole.ecore.EcoreGenerator;

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
        EcoreGenerator generator = getGenerator(Task.getProfileModel(file), Task.getBackgroundModel(file), namespace, Task.getProperty(file, Task.PROFILE_NAMESPACE), preserveNS);
        generator.run();
        // Use file name for top level package name.
        generator.getResult().setName(result.getName().split("\\.")[0]);
        System.out.println("Generated ECore model: " + result.getName());
        ResourceSet metaResourceSet = new ResourceSetImpl();
        // Register XML Factory implementation to handle .ecore files
        metaResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
            "ecore", new  XMLResourceFactoryImpl());
        // Create empty resource with the given URI
        org.eclipse.emf.ecore.resource.Resource metaResource =
            metaResourceSet.createResource(URI.createURI(result.getFullPath().toString()));
        metaResource.getContents().add(generator.getResult());
        try {
            metaResource.save(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected EcoreGenerator getGenerator(OntModel profileModel, OntModel backgroundModel, String namespace, String profileNamespace, boolean preserveNS) throws CoreException {
        return new EcoreGenerator(profileModel, backgroundModel, namespace, profileNamespace, preserveNS, true, true);
    }
}