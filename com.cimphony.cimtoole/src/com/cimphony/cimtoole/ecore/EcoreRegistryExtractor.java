package com.cimphony.cimtoole.ecore;

import java.io.IOException;
import java.util.Scanner;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import com.cimphony.cimtoole.CimphonyCIMToolPlugin;

public class EcoreRegistryExtractor extends EcoreExtractor{

	public void run() throws IOException, CoreException{
		if (file == null) throw new CoreException(new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, "No input file set"));
		if (!file.getFileExtension().equals("ecore-registry"))
			return;
		Scanner scan = new Scanner(file.getContents());
		String ePackageUri = scan.nextLine();
		if (ePackageUri == null) throw new CoreException(new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, "No EPackage URI set"));
		EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(ePackageUri);
		if (ePackage == null)  throw new CoreException(new Status(IStatus.ERROR, CimphonyCIMToolPlugin.PLUGIN_ID, "The schema "+ePackageUri+" does not existing in the EPackage Registry"));
		try{
			String ns = ePackage.getNsURI();
			if (!ns.endsWith("#")) ns += "#";
			model.setNsPrefix(ePackage.getNsPrefix(), ns);
			processEPackage(ePackage);
			for (EClassifier c: classMap.keySet())
				postProcessEClassifiers(c);
		}catch (NullPointerException npe){
			npe.printStackTrace();
		}

	}
}
