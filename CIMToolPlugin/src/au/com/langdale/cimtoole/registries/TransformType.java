package au.com.langdale.cimtoole.registries;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.JSONSchemaBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.ProfileBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;

/**
 * Internal enumeration defining the set of supported types of XSLT
 * TransformBuildlets. This enum is used to "map" these type against what is
 * configured in the .builders configuration file.
 */
public enum TransformType {
	JSON, TEXT, XSD, TRANSFORM;

	public static TransformType toTransformType(ProfileBuildlet buildlet) {
		if (buildlet instanceof JSONSchemaBuildlet) {
			return JSON;
		} else if (buildlet instanceof TextBuildlet) {
			return TEXT;
		} else if (buildlet instanceof XSDBuildlet) {
			return XSD;
		} else if (buildlet instanceof TransformBuildlet) {
			return TRANSFORM;
		}
		return null;
	}
}