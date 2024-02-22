/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.sax;

import org.xml.sax.XMLReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.DTDHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 *  A support class for SAX XMLReader, XMLFilter and our own XMLTransformer
 *  implementations. This class is only declared as an
 *  XMLReader, leaving it open to the derived class to ignore the more
 *  specialized support.
 */
public abstract class XMLReaderBase implements XMLReader {

    /**
     *  The child's ContentHandler.
     */
    protected ContentHandler output;
    /**
     * 	The child's entity resolver
     */
    protected EntityResolver resolver = new DummyResolver();
    /**
     *  The source specified in the parse() message.
     */
    protected InputSource input;
    /**
     *  The child's ErrorHandler.
     */
    protected ErrorHandler errors;
    /**
     *  The parent.
     */
    protected XMLReader parent;
    /**
     *  The extended properties.
     */
    protected Map properties = new HashMap();
    /**
     *  The extended features.
     */
    protected Map features = new HashMap();
    /**
     *  The name of the extended property for a lexical-handler object.
     */
    protected final String LEXICAL_HANDLER = "http://xml.org/sax/handlers/LexicalHandler";

    /**
     *  Construct with default features.
     */
    public XMLReaderBase() {
        // we assume these features are always on.
        features.put("http://xml.org/sax/features/namespaces", Boolean.TRUE);
        features.put("http://xml.org/sax/features/namespace-prefixes", Boolean.TRUE);
    }


    /**
     *  Switches on a feature of the reader.
     *
     */
    public void setFeature(String parm1, boolean parm2) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (!features.containsKey(parm1)) {
            throw new SAXNotRecognizedException(parm1);
        }
        features.put(parm1, Boolean.valueOf(parm2));
    }


    /**
     *  Sets a property of the reader (e.g. a special handler).
     */
    public void setProperty(String parm1, Object parm2) throws SAXNotRecognizedException, SAXNotSupportedException {
        properties.put(parm1, parm2);
    }


    /**
     *  Nominally sets the EntityResolver attribute of the XMLReaderBase object
     *  (but not implemented).
     */
    public void setEntityResolver(EntityResolver parm1) {
    	resolver = parm1;
    }


    /**
     *  Sets the Lexical handler for the reader. This is eqivalent to setting
     *  the lexical handler property and is not a SAX method.
     *
     */
    public void setLexicalHandler(LexicalHandler lexical) {
        properties.put(LEXICAL_HANDLER, lexical);
    }


    /**
     *  Nominally sets the DTDHandler attribute of the XMLReaderBase object
     *  (but is not implemented).
     */
    public void setDTDHandler(DTDHandler parm1) {
    }


    /**
     *  Sets the ContentHandler attribute of the XMLReaderBase object
     *
     */
    public void setContentHandler(ContentHandler handler) {
        output = handler;
    }


    /**
     *  Sets the parent reader (if this is a filter).
     */
    public void setParent(XMLReader reader) {
        parent = reader;
    }


    /**
     *  Sets the ErrorHandler attribute of the XMLReaderBase object
     */
    public void setErrorHandler(ErrorHandler handler) {
        errors = handler;
    }


    /**
     *  Tests whether a feature is switched on.
     */
    public boolean getFeature(String parm1) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (!features.containsKey(parm1)) {
            throw new SAXNotRecognizedException(parm1);
        }
        return features.get(parm1).equals(Boolean.TRUE);
    }


    /**
     *  Gets a property object.
     *
     */
    public Object getProperty(String parm1) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (!properties.containsKey(parm1)) {
            throw new SAXNotRecognizedException(parm1);
        }
        return properties.get(parm1);
    }


    /**
     *  Gets the EntityResolver attribute of the XMLReaderBase object
     */
    public EntityResolver getEntityResolver() {
        return resolver;
    }


    /**
     *  Gets the Lexical handler. (This is not a SAX method.)
     */
    public LexicalHandler getLexicalHandler() {
        return (LexicalHandler) properties.get(LEXICAL_HANDLER);
    }


    /**
     *  Gets the DTDHandler attribute of the XMLReaderBase object
     *
     */
    public DTDHandler getDTDHandler() {
        return null;
    }


    /**
     *  Gets the ContentHandler attribute of the XMLReaderBase object
     *
     */
    public ContentHandler getContentHandler() {
        return output;
    }


    /**
     *  Gets the parent reader.
     */
    public XMLReader getParent() {
        return parent;
    }


    /**
     *  Gets the ErrorHandler attribute of the XMLReaderBase object
     */
    public ErrorHandler getErrorHandler() {
        return errors;
    }

	static class DummyResolver implements EntityResolver
	{
	    static class EmptyStream extends InputStream
	    {
   			/**
			 * @see java.io.InputStream#read()
			 */
			@Override
			public int read() throws IOException {
				return -1;
			}
		};

		/**
		 * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
		 */
		public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
			return new InputSource( new InputStreamReader( new EmptyStream() ));
		}

	};


    /**
     *  Accepts a specification of what to parse, and initiates parsing.
     *
     */
    public void parse(InputSource source) throws java.io.IOException, SAXException {
        input = source;
        parse();
    }


    /**
     *  Convenience method that accepts the filename of a file to parse.
     *
     */
    public void parse(String name) throws java.io.IOException, SAXException {
        parse(new InputSource(new java.io.FileReader(name)));
    }
    
    /**
     *  A utility to configure a delegate (e.g. parent) reader.
     */
    protected void setup(XMLReader delegate) throws SAXException {
        delegate.setContentHandler(output);
        delegate.setEntityResolver( resolver );
        if( errors != null)
            delegate.setErrorHandler(errors);
        if(! delegate.getFeature("http://xml.org/sax/features/namespaces")) {
            delegate.setFeature("http://xml.org/sax/features/namespaces", true);
        }
        for (Iterator ix = properties.keySet().iterator(); ix.hasNext(); ) {
            String key = (String) ix.next();
            try {
                delegate.setProperty(key, properties.get(key));
            }
            catch (SAXException ex) {
                // ignore
            }
        }
    }


    /**
     *  Overide this method to solicit SAX events from the parent
     *  and/or generate SAX events into the currently set handlers.
     */
    protected abstract void parse() throws SAXException, java.io.IOException;
}
