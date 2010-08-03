package au.com.langdale.cimtoole.registries;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;

public class ProfileBuildletRegistryImpl implements ProfileBuildletRegistry{

	private Map<String, ProfileBuildlet> buildlets;
	
	ProfileBuildletRegistryImpl() {
		buildlets = new TreeMap<String, ProfileBuildlet>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = registry.getExtensionPoint(ProfileBuildletRegistry.BUILDLET_REGISTRY_ID);

		IExtension[] pExts = extPoint.getExtensions();
		for (IExtension p : pExts){
			for (IConfigurationElement el: p.getConfigurationElements()){
				try {
					Object obj = el.createExecutableExtension("class");
					if (obj instanceof ProfileBuildlet){
						ProfileBuildlet buildlet = (ProfileBuildlet)obj;
						String id = el.getAttribute("id");
						if (buildlet!=null && id!=null){
							buildlets.put(id, buildlet);
						}
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	public ProfileBuildlet[] getBuildlets(){
		
		return buildlets.values().toArray(new ProfileBuildlet[buildlets.values().size()]);
		/* Not sure we need to make copies... try it as singletons for now
		
		ProfileBuildlet[] copies = new ProfileBuildlet[buildlets.size()];
		int i=0;
		for (ProfileBuildlet p : buildlets.values()){
			try {
				copies[i++] = p.getClass().newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return copies;
		*/
	}

}
