/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;

/**
 * Static factory class for creating CIM interpreters specialized for specific
 * schema file types.
 *
 */
public class CIMInterpreterFactory {

	public static CIMInterpreter create(File file) {
		String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
		switch (ext) {
		case "xmi":
			return new LegacyCIMInterpreterImpl();
		default:
			return new CIMInterpreterImpl();
		}
	}
	
	public static CIMInterpreter create(StereotypedNamespaces stereotypedNamespaces, File file) {
		String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
		switch (ext) {
		case "xmi":
			return new LegacyCIMInterpreterImpl(stereotypedNamespaces);
		default:
			return new CIMInterpreterImpl(stereotypedNamespaces);
		}
	}
	
}
