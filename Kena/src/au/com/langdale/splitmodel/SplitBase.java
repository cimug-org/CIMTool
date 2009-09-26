/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.splitmodel;

import java.io.File;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * A vocabulary use in split models and the mapping function between URI's and files.
 */
public class SplitBase {

	public static final String LOCAL = "http://langdale.com.au/2007/SplitModel/local";
	public static final String DOCUMENT = "http://langdale.com.au/2007/SplitModel#document";
	public static final String HASH = "http://langdale.com.au/2007/SplitModel#hasHash";
	public static final String MODULUS = "http://langdale.com.au/2007/SplitModel#hasModulus";
	public static final String SPLITMODEL = "http://langdale.com.au/2007/SplitModel#";
	public static final String RDF_TYPE_URI = RDF.Nodes.type.getURI();
	public static final String XSD_URI = XSDDatatype.XSD + "#";
	public static final String XSD_INTEGER_URI = XSDDatatype.XSDinteger.getURI();

	public static int hashURI(String uri, int modulus) {
		return Math.abs(uri.hashCode()) % modulus;
	}

	protected File destin;
	protected int modulus;

	protected File getFile(int key) {
		String hex = Integer.toHexString(key);
		if( hex.length() == 1)
			hex = "0" + hex;
		File target = new File(destin, "Part" + hex + ".ttl");
		return target;
	}
	/**
	 * @return the number of files used by this split model.
	 */
	public int getModulus() {
		return modulus;
	}

	protected int hashURI(String uri) {
		return hashURI(uri, modulus);
	}

}
