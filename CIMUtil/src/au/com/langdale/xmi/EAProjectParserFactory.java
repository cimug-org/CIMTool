/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.logging.SchemaImportLogger;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Static factory class for creating parsers specialized for specific EA Project
 * files.
 *
 */
public class EAProjectParserFactory {

	/**
	 * The extensions of the Enterprise Architect project files CIMTool can parse,
	 * in lower case and without the dot. {@link #createParser} has a parser for
	 * each of them, and the two must be kept in step. This list is the one
	 * definition of an Enterprise Architect project file, used by the CIMTool
	 * plug-in and the command line alike (issue #391).
	 */
	public static final List<String> EXTENSIONS = Collections
			.unmodifiableList(Arrays.asList("eap", "eapx", "qea", "qeax"));

	/**
	 * Returns true if the file name has one of the {@link #EXTENSIONS}, ignoring
	 * case.
	 */
	public static boolean isEAProject(String fileName) {
		if (fileName == null)
			return false;
		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1)
			return false;
		return EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

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
