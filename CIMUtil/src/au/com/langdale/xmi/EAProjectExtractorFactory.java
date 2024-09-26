/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;

/**
 * Static factory class for creating extractors specialized for specific EA
 * Project files.
 *
 */
public class EAProjectExtractorFactory {
	
	public static EAProjectExtractor createExtractor(File file) {
		String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
		switch (ext) {
			case "eap":
			case "eapx":
				return new EAPExtractor(file);
			case "qea":
			case "qeax":
				return new QEAExtractor(file);
			case "feap":
				return new FEAPExtractor(file);
			default:
				throw new IllegalArgumentException("Unsupported EA project file type: " + ext);
			}
	}
	
}
