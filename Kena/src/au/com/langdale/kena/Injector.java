package au.com.langdale.kena;

import java.io.IOException;


public interface Injector {

	/**
	 * Create a submodel labelled by a given node and with the same parameters as the current model.
	 * 
	 * @param node: a node that designates this submodel 
	 * @return a new SplitWriter instance
	 */
	public Injector createQuote(Object node);

	/**
	 * Assign a namespace prefix.  Assigning prefixes to common namespaces
	 * will reduce the space requirement of the model.  Prefix assignments
	 * made after the first statement has been added to the model are 
	 * ignored.
	 * @param prefix: a short string conforming to NCNAME
	 * @param namespace: a http URI terminated by '#' 
	 */
	public void setPrefix(String prefix, String namespace);

	/**
	 * Flush pending  output and release resources.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException;

	/**
	 * Create a blank node with the given identity.
	 * @throws ConversionException 
	 * 
	 */
	public Object createAnon(String id) throws ConversionException;
	
	/**
	 * Create a node representing the given URI reference.
	 * @throws ConversionException 
	 * 
	 */
	public Object createNamed(String uri) throws ConversionException;
	
	/**
	 * Create a literal node.
	 */
	public Object createLiteral(String value, String lang, String type, boolean isXML) throws ConversionException;

	/**
	 * Add a statement referring to a resource.
	 * 
	 * @param subj: a node returned by createAnon() or createNamed() representing the subject
	 * @param pred: the URI of the predicate or property
	 * @param obj: a node returned by createAnon() or createNamed() representing the object 
	 * @throws IOException
	 * @throws ConversionException 
	 */
	public void addObjectProperty(Object subj, String pred, Object obj) throws IOException, ConversionException;

	/**
	 * Add a statement referring to a literal value.
	 * @param subj: a node returned by createAnon() or createNamed() representing the subject
	 * @param pred: the URI of the predicate or property
	 * @param value: a node returned by createLiteral
	 * @throws IOException
	 * @throws ConversionException 
	 */
	public void addDatatypeProperty(Object subj, String pred, Object value) throws IOException, ConversionException;

}