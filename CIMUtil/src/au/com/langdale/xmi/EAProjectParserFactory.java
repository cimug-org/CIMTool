/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.logging.SchemaImportLogger;

import java.io.File;
import java.util.Set;

/**
 * Static factory class for creating parsers specialized for specific EA Project
 * files.
 *
 */
public class EAProjectParserFactory {

	public static EAProjectParser createParser(String baseURI, File file, boolean selfHealOnImport,
			boolean validateModel, boolean usePackageNames, SchemaImportLogger logger,
			File namespacesFile, Set<String> stereotypeExtensions) {
		String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
		switch (ext) {
		case "eap":
		case "eapx":
			return new EAPParser(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger,
					namespacesFile, stereotypeExtensions);
		case "qea":
		case "qeax":
			return new QEAParser(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger,
					namespacesFile, stereotypeExtensions);
		default:
			throw new IllegalArgumentException("Unsupported EA project file type: " + ext);
		}
	}

}
