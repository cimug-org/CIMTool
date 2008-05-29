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

import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;

public class SplitWriter extends SplitBase {
	
	public static class ConversionException extends Exception {
		private static final long serialVersionUID = 9060636904114094601L;
	}
	
	public static final int DEFAULT_MODULUS = 128;
	public static final int DEFAULT_QUOTA = 128;
	
	private static final String QUOTE = "\"\"\"";
	private static final CharSequence ESCAPE = "\\";
	private static final CharSequence QUOTE_MARK = "\"";
	private static final CharSequence ESCAPE_ESCAPE = "\\\\";
	private static final CharSequence ESCAPE_QUOTE_MARK = "\\\"";
	private static final Pattern NCNAME_REGEX = Pattern.compile("[A-Za-z_][A-Za-z0-9-_.]*");
	private static final String RDF_TYPE = RDF.type.getURI(); 

	private final String local;
	private final Map spaces;
	private final Map prefixes;
	private final Writer[] cache;
	private final int quota;
	
	private int sweep, active;
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
		local = LOCAL + Integer.toHexString(new Random().nextInt()) + "#";
		spaces = new LinkedHashMap();
		prefixes = new HashMap();
		setPrefix("local", local);
		setPrefix("split", SPLITMODEL);
		setPrefix("xsd", XSD.getURI());
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
		local = parent.local;
		prefixes = parent.prefixes;
		spaces = parent.spaces;
		freeze = true;
	}
	/**
	 * Create a submodel in a subdirectory with a given name. The resulting
	 * SplitWriter has the same parameters as its parent.
	 * 
	 * @param name: the name of the submodel 
	 * @return a new SplitWriter instance
	 */
	public SplitWriter createQuote(String name) {
		return new SplitWriter(this, name);
	}
	/**
	 * @return the base URI of the model
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
	/**
	 * Assign a namespace prefix.  Assigning prefixes to common namespaces
	 * will reduce the space requirement of the model.  Prefix assignments
	 * made after the first statement has been added to the model are 
	 * ignored.
	 * @param prefix: a short string conforming to NCNAME
	 * @param namespace: a http URI terminated by '#' 
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
			result.write(createStatement(createSymbol(DOCUMENT), createSymbol(HASH), createSymbol(Integer.toString(key), XSD.integer.getURI())));
			result.write(createStatement(createSymbol(DOCUMENT), createSymbol(MODULUS), createSymbol(Integer.toString(modulus), XSD.integer.getURI())));
		} catch (ConversionException e) {
			throw new Error(e);
		}
		result.write("\n");
	}
	/**
	 * Flush pending  output and release resources.
	 * 
	 * @throws IOException
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

	private String createStatement(String subj, String pred, String obj) {
		return subj + " " + pred + " " + obj + " .\n";
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
	
	/**
	 * Create a URI reference that can be used as a poor man's anonymous resource.  
	 * The URI is constructed so that it will not be found in any other model.
	 * 
	 * @param id
	 * @return
	 */
	public String createAnon(String id) {
		return local + id;
	}
	/**
	 * Add a statement referring to a resource.
	 * 
	 * @param subj: the URI of the subject
	 * @param pred: the URI of the predicate or property
	 * @param obj: the URI of the object 
	 * @throws IOException
	 */
	public void add(String subj, String pred, String obj) throws IOException {
		String stmnt;
		try {
			stmnt = createStatement(createSymbol(subj), createSymbol(pred), createSymbol(obj));
		} catch (ConversionException e) {
			return;
		}
		int subjKey = hashURI(subj);
		getWriter(subjKey).write(stmnt);
		if( ! pred.equals(RDF_TYPE)) {
			int objKey = hashURI(obj);
			if( subjKey != objKey )
				getWriter(objKey).write(stmnt);
		}
	}
	/**
	 * Add a statement referring to a literal value.
	 * @param subj: the URI of the subject
	 * @param pred: the URI of the object
	 * @param value: the object as a string (its lexical form)
	 * @param type: the URI of the object's datatype
	 * @throws IOException
	 */
	public void add(String subj, String pred, String value, String type) throws IOException {
		String stmnt;
		try {
			stmnt = createStatement(createSymbol(subj), createSymbol(pred), createSymbol(value, type));
		} catch (ConversionException e) {
			return;
		}
		int subjKey = hashURI(subj);
		getWriter(subjKey).write(stmnt);
	}
}