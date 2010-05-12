/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.splitmodel;

/**
 * Write a split model.  A split model represents a very large RDF graph with
 * a set of Turtle files.
 */
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import au.com.langdale.kena.ConversionException;
import au.com.langdale.kena.Injector;

import com.hp.hpl.jena.rdf.model.impl.Util;

public class SplitWriter extends SplitBase implements Injector {
	
	public static final int DEFAULT_MODULUS = 128;
	public static final int DEFAULT_QUOTA = 128;
	
	private static final String QUOTE = "\"\"\"";
	private static final CharSequence ESCAPE = "\\";
	private static final CharSequence QUOTE_MARK = "\"";
	private static final CharSequence ESCAPE_ESCAPE = "\\\\";
	private static final CharSequence ESCAPE_QUOTE_MARK = "\\\"";
	private static final Pattern NCNAME_REGEX = Pattern.compile("[A-Za-z_][A-Za-z0-9-_.]*");
	private static final Random random = new Random();
	
	private final String local = LOCAL + Integer.toHexString(random.nextInt()) + "#";
	private final Map spaces;
	private final Map prefixes;
	private final Writer[] cache;
	private final int quota;
	
	private int sweep, active, sequ = 0x1000;
	private String base;
	private boolean freeze, imprinted;
	/**
	 * Intitialise with all parameters
	 * @param destin: the pathname of the directory containing the split model
	 * @param base: the base namespace
	 * @param modulus: the number of files to use.  this parameter should be proportional to model size.
	 * @param quota: the number of files to keep memory resident.
	 */
	public SplitWriter(String destin, String base, int modulus, int quota) {
		this.destin = new File(destin);
		this.quota = quota;
		setBase(base);
		setModulus(modulus);
		cache = new Writer[modulus];
		spaces = new LinkedHashMap();
		prefixes = new HashMap();
		setPrefix("local", local);
		setPrefix("split", SPLITMODEL);
		setPrefix("xsd", XSD_URI);
		clear();
	}
	/**
	 * Initialise with base namespace (recommended).  URI's that 
	 * start with the base namespace will be abreviated in storage.
	 * @param destin: the pathname of the directory containing the split model
	 * @param base: the base namespace.  
	 */
	public SplitWriter(String destin, String base) {
		this(destin, base, DEFAULT_MODULUS, DEFAULT_QUOTA);
	}
	/**
	 * Initialise using the base namespace.
	 * @param destin: the pathname of the directory containing the split model
	 */
	public SplitWriter(String destin) {
		this(destin, new File(destin).toURI().toString());
	}
	
	private SplitWriter(SplitWriter parent, String name) {
		destin = new File(parent.destin, name);
		quota = parent.quota;
		setBase(parent.base);
		setModulus(parent.modulus);
		cache = new Writer[modulus];
		prefixes = new HashMap(parent.prefixes);
		spaces = new HashMap(parent.spaces);
		removePrefix("local");
		setPrefix("local", local);
		clear();
	}
	
	private void clear() {
		for( int ix = 0; ix < modulus; ix++) {
			File f = getFile(ix);
			if( f.exists())
				f.delete();
		}
	}
	
	/* 
	 * @see au.com.langdale.splitmodel.Injector#createQuote(java.lang.String)
	 */
	public Injector createQuote(Object node) {
		String uri = (String) node;
		return new SplitWriter(this, uri.substring( Util.splitNamespace( uri )));
	}
	/* (non-Javadoc)
	 * @see au.com.langdale.splitmodel.Injector#getBase()
	 */
	public String getBase() {
		return base;
	}
	
	/**
	 * Set the base namespace, provided no statements have been added.
	 */
	public void setBase(String base) {
		if(freeze)
			return;
		if( ! base.endsWith("#"))
			base += "#";
		this.base = base;
	}
	/**
	 * Set the modulus, or number of files, provided no statements have been added.
	 * @param modulus
	 */
	public void setModulus(int modulus) {
		if(freeze)
			return;
		if( modulus < 2)
			throw new IllegalArgumentException("split model modulus must be 2 or greater");
		this.modulus = modulus;
	}
	/* (non-Javadoc)
	 * @see au.com.langdale.splitmodel.Injector#setPrefix(java.lang.String, java.lang.String)
	 */
	public void setPrefix(String prefix, String namespace) {
		if( freeze )
			return;
		if( ! namespace.startsWith("http:") || ! namespace.endsWith("#"))
			return;
		if( prefixes.containsKey(prefix) || spaces.containsKey(namespace))
			return;
		spaces.put(namespace, prefix);
		prefixes.put(prefix, namespace);
	}
	
	private void removePrefix(String prefix) {
		if( freeze )
			return;
		
		Object ns = prefixes.remove(prefix);
		if( ns != null)
			spaces.remove(ns);
	}
	
	private Writer getWriter(int key) throws IOException {
		Writer result = cache[key];
		
		if( result == null) {
			evict();
			result = open(key);
		}
		return result;
	}

	private Writer open(int key) throws IOException {
		Writer result;
		File target = getFile(key);
		destin.mkdirs();			
		boolean isnew = target.createNewFile();
		result = new OutputStreamWriter( new BufferedOutputStream (new FileOutputStream(target, true)), "UTF-8");
		if( isnew )
			init(result, key);
		else
			result.write("\n");
		cache[key] = result;
		active++;
		freeze = true;
		imprinted = imprinted || key == 0;
		return result;
	}

	private void init(Writer result, int key) throws IOException {
		// result.write("@base <" + base + "> .\n");
		result.write("@prefix : <" + base + "> .\n"); // @base not supported in Jena 2.5.3
	
		Iterator it = spaces.keySet().iterator();
		while( it.hasNext()) {
			String namespace = (String) it.next();
			String prefix = (String) spaces.get(namespace);
			result.write( "@prefix " + prefix + ": <" + namespace + "> .\n");
		}
		result.write("\n");

		try {
			result.write(createStatement(DOCUMENT, HASH, createSymbol(Integer.toString(key), XSD_INTEGER_URI)));
			result.write(createStatement(DOCUMENT, MODULUS, createSymbol(Integer.toString(modulus), XSD_INTEGER_URI)));
		} catch (ConversionException e) {
			throw new Error(e);
		}
		result.write("\n");
	}
	/* 
	 * @see au.com.langdale.splitmodel.Injector#close()
	 */
	public void close() throws IOException {
		for( int ix = 0; active > 0; ix++ ) {
			Writer target = cache[ix];
			if( target != null) {
				target.close();
				active--;
			}
		}
		
		if( ! imprinted )
			open(0).close(); // ensure file 0 is always present
	}
	
	private String createSymbol(String uri) throws ConversionException {
		if( uri.startsWith(base)) {
			String name = uri.substring(base.length());
			if( NCNAME_REGEX.matcher(name).matches())
				return ":" + name;
		}
		else {
			int ix = uri.lastIndexOf('#') + 1;
			if( ix > 0 && ix < uri.length()) {
				String namespace = uri.substring(0, ix);
				String prefix = (String) spaces.get(namespace);
				if( prefix != null) {
					String name = uri.substring(ix);
					if( NCNAME_REGEX.matcher(name).matches())
						return prefix + ":" + name;
				}
			}
		}

		if( uri.contains("{"))
			throw new ConversionException();
		return "<" + uri + ">";
	}
	
	private String createSymbol(String lex, String type) throws ConversionException {
		String suffix;
		
		if( type != null )
			suffix = "^^" + createSymbol(type);
		else
			suffix = "";
		
		String escaped = lex
							.replace(ESCAPE, ESCAPE_ESCAPE)
							.replace(QUOTE_MARK, ESCAPE_QUOTE_MARK);
		
		return QUOTE + escaped + QUOTE + suffix;
	}

	private String createStatement(String subj, String pred, String obj) throws ConversionException {
		return createSymbol(subj) + " " + createSymbol(pred) + " " + obj + " .\n";
	}

	private void evict() throws IOException {
		while( active > quota ) {
			sweep = ( sweep + 1) % modulus;
			Writer target = cache[sweep];
			if( target != null) {
				target.close();
				cache[sweep] = null;
				active--;
			}
		}
	}
	
	/* 
	 * @see au.com.langdale.splitmodel.Injector#createAnon(java.lang.String)
	 */
	public Object createAnon(String id) throws ConversionException {
		if( id != null )
			return local + id;
		else 
			return local + "_" + Integer.toHexString(sequ++);
	}
	
	/* 
	 * @see au.com.langdale.splitmodel.Injector#createNamed(java.lang.String)
	 */
	public Object createNamed(String uri) throws ConversionException {
		return uri;
	}
	/* 
	 * @see au.com.langdale.splitmodel.Injector#createLiteral
	 */
	public Object createLiteral(String value, String lang, String type, boolean isXML) throws ConversionException {
		return createSymbol(value, type);
	}
	
	/* 
	 * @see au.com.langdale.splitmodel.Injector#addObjectProperty(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void addObjectProperty(Object subj, String pred, Object obj) throws IOException, ConversionException {
		String s = (String)subj;
		String o = (String)obj;
		String stmnt = createStatement(s, pred, createSymbol(o));
		int subjKey = hashURI(s);
		getWriter(subjKey).write(stmnt);
		if( ! pred.equals(RDF_TYPE_URI)) {
			int objKey = hashURI(o);
			if( subjKey != objKey )
				getWriter(objKey).write(stmnt);
		}
	}
	/* 
	 * @see au.com.langdale.splitmodel.Injector#addDatatypeProperty(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	public void addDatatypeProperty(Object subj, String pred, Object obj) throws IOException, ConversionException {
		String s = (String)subj;
		String o = (String)obj;
		String stmnt = createStatement(s, pred, o);
		int subjKey = hashURI(s);
		getWriter(subjKey).write(stmnt);
	}
}