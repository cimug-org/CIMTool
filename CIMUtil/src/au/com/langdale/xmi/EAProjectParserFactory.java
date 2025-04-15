/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;

/**
 * Static factory class for creating parsers specialized for specific EA
 * Project files.
 *
 */
public class EAProjectParserFactory {
	
	public static EAProjectParser createParser(File file, boolean selfHealOnImport, SchemaImportLogger logger) {
		String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
		switch (ext) {
			case "eap":
			case "eapx":
				return new EAPParser(file, selfHealOnImport, logger);
			case "qea":
			case "qeax":
				return new QEAParser(file, selfHealOnImport, logger);
			case "feap":
				return new FEAPParser(file, selfHealOnImport, logger);
			default:
				throw new IllegalArgumentException("Unsupported EA project file type: " + ext);
			}
	}
	
}
