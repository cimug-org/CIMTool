/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.kena;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.arp.ALiteral;
import com.hp.hpl.jena.rdf.arp.ARP;
import com.hp.hpl.jena.rdf.arp.ARPErrorNumbers;
import com.hp.hpl.jena.rdf.arp.ARPHandlers;
import com.hp.hpl.jena.rdf.arp.ARPOptions;
import com.hp.hpl.jena.rdf.arp.AResource;
import com.hp.hpl.jena.rdf.arp.NamespaceHandler;
import com.hp.hpl.jena.rdf.arp.QuotedStatementHandler;
import com.hp.hpl.jena.rdf.arp.StatementHandler;
/**
 * Bridge a Jena ARP RDF parser to the Injector interface.  This interface
 * supports quoting, preservation of blank node id's, and lightweight rewriting
 * of the source by SplitWriter.
 */
public class RDFParser implements Runnable {

	private InputStream stream;
	private String source, base;
	private Injector destin;
	private List stack = new LinkedList();

	private ARP arp;
	
	/**
	 * 
	 * @param source: the filename of the RDF/XML input
	 * @param destin: the directory name of the split output
	 * @param namespace: default namespace for abbreviating URI's in the output
	 * @param logger: destination for error messages
	 * @param allowQuotes: true causes parseType="Statements" to be recognised, which is
	 * used in Incremental CIM/XML
	 */
	public RDFParser(InputStream stream, String source, String base, Injector destin, ErrorHandler errors, boolean allowQuotes) {
		this.source = source;
		this.destin = destin;
		this.base = base;
		this.stream = stream;
		arp = new ARP();
		
		ARPOptions options = arp.getOptions();
		options.setStrictErrorMode();
		options.setErrorMode(ARPErrorNumbers.WARN_REDEFINITION_OF_ID, ARPErrorNumbers.EM_IGNORE);

		ARPHandlers handlers = arp.getHandlers();
		if( errors != null )
			handlers.setErrorHandler(errors);
		handlers.setNamespaceHandler(new Namespaces() );
		if( allowQuotes )
			handlers.setStatementHandler(new QuotedStatements());
		else
			handlers.setStatementHandler(new Statements());
	}
	
	/**
	 * Execute the parse.
	 */
	public void run() {
		try {
			if(stream == null)
				stream = new FileInputStream(source);
			arp.load(new BufferedInputStream( stream ), base);
			destin.close();
		} catch (SAXException e) {
			throw new TerminateParseException("fatal parse error", e);
		} catch (IOException e) {
			throw new TerminateParseException("error accessing source model", e);
		}
	}
	
	private void push(Injector quoteWriter) {
		stack.add(destin);
		destin = quoteWriter;
	}
	
	private void pop() {
		destin = (Injector) stack.remove(stack.size()-1);
	}
	
	private class Namespaces implements NamespaceHandler {
		public void startPrefixMapping(String prefix, String uri) {
			// capture any namespace prefixes and relay them to the pool
			// they will only be used if they occur before anything is written
			destin.setPrefix(prefix, uri);
		}

		public void endPrefixMapping(String prefix) {
			// don't care
		}
	}

	private class Statements implements StatementHandler {
		
		public void statement(AResource subj, AResource pred, AResource obj) {
			try {
				destin.addObjectProperty(getNode(subj), pred.getURI(), getNode(obj));
			} catch (IOException e) {
				throw new TerminateParseException("error writing to split model", e);
			} catch (ConversionException e) {
				// skip this statement TODO: possibly terminate parsing here
			}
		}

		public void statement(AResource subj, AResource pred, ALiteral lit) {
			try {
				Object value = destin.createLiteral(lit.toString(), lit.getLang(), lit.getDatatypeURI(), lit.isWellFormedXML());
				destin.addDatatypeProperty(getNode(subj), pred.getURI(), value);
			} catch (IOException e) {
				throw new TerminateParseException("error writing to split model", e);
			} catch (ConversionException e) {
				// skip this statement TODO: possibly terminate parsing here
			}
		}
	}

	private Object getNode(AResource node) throws ConversionException {
		if( node.isAnonymous()) {
			Object symbol = node.getUserData();
			if( symbol == null) {
				if(node.hasNodeID())
					symbol = destin.createAnon(node.getAnonymousID());
				else 
					symbol = destin.createAnon(null);
				node.setUserData(symbol);
			}
			return symbol;
		}
		else
			return destin.createNamed(node.getURI());
	}
	
	private class QuotedStatements extends Statements implements QuotedStatementHandler {
		
		public void startQuote(AResource quote) {
			try {
				push(destin.createQuote(getNode(quote)));
			} catch (ConversionException e) {
				throw new TerminateParseException("error writing to split model", e);
			}
		}

		public void endQuote() {
			try {
				destin.close();
			} catch (IOException e) {
				throw new TerminateParseException("error writing to split model", e);
			}
			pop();
		}
	}

	public static class TerminateParseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public TerminateParseException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	
}
