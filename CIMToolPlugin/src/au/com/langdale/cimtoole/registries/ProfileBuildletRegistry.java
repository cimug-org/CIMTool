package au.com.langdale.cimtoole.registries;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;

public interface ProfileBuildletRegistry {

	public static final String BUILDLET_REGISTRY_ID = "au.com.langdale.cimtoole.profile_buildlet";
	
	public static ProfileBuildletRegistry INSTANCE = new ProfileBuildletRegistryImpl();
	
	public ProfileBuildlet[] getBuildlets();
}
