package au.com.langdale.cimtoole.registries;

import java.util.Map;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;

public class ProfileBuildletRegistryImpl implements ProfileBuildletRegistry {

	private Map<String, ProfileBuildlet> extensionBuildlets;

	ProfileBuildletRegistryImpl() {
		initBuildlets();
	}

	private void initBuildlets() {
		extensionBuildlets = ProfileBuildletConfigUtils.getExtensionBuildlets();
	}

	public ProfileBuildlet[] getExtensionBuildlets() {
		return extensionBuildlets.values().toArray(new ProfileBuildlet[extensionBuildlets.values().size()]);
	}

	public ProfileBuildlet[] getCustomBuildlets() {
		/**
		 * Note that since custom buildets can be imported without a restart we always
		 * make a call to getCustomBuildlets() to get the latest set of those buildlets.
		 */
		Map<String, TransformBuildlet> customBuildlets = ProfileBuildletConfigUtils.getTransformBuildlets();
		return customBuildlets.values().toArray(new ProfileBuildlet[customBuildlets.values().size()]);
	}

	public ProfileBuildlet[] getBuildlets() {
		/**
		 * Note that since custom buildets can be imported without a restart we always
		 * make a call to getCustomBuildlets() to get the latest set of those buildlets.
		 */
		Map<String, TransformBuildlet> customBuildlets = ProfileBuildletConfigUtils.getTransformBuildlets();

		ProfileBuildlet[] buildlets = new ProfileBuildlet[extensionBuildlets.values().size()
				+ customBuildlets.values().size()];

		System.arraycopy(extensionBuildlets.values().toArray(new ProfileBuildlet[] {}), 0, buildlets, 0,
				extensionBuildlets.values().size());
		System.arraycopy(customBuildlets.values().toArray(new ProfileBuildlet[] {}), 0, buildlets,
				extensionBuildlets.values().size(), customBuildlets.values().size());

		return buildlets;
	}

}
