/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.splitmodel;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.xml.sax.SAXException;

import au.com.langdale.util.Logger;

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
 * Bridge a Jena ARP RDF parser to a SplitWriter.  The run() method causes statements 
 * harvested from a a potentially very large RDF/XML document to be inserted into 
 * a series of files (a split model) using constant memory.
 */
public class ModelSplitter implements Runnable {

	private String source;
	private SplitWriter destin;
	List stack = new LinkedList();

	private ARP arp;
	
	/**
	 * 
	 * @param source: the filename of the RDF/XML input
	 * @param destin: the directory name of the split output
	 * @param namespace: default namespace for abreviating URI's in the output
	 * @param logger: destination for error messages
	 * @param allowQuotes: true causes parseType="Statements" to be recognised, which is
	 * used in Incremental CIM/XML
	 */
	public ModelSplitter(String source, String destin, String namespace, Logger logger, boolean allowQuotes) {
		this.source = source;
		this.destin = new SplitWriter(destin, namespace);
		arp = new ARP();
		
		ARPOptions options = arp.getOptions();
		options.setStrictErrorMode();
		options.setErrorMode(ARPErrorNumbers.WARN_REDEFINITION_OF_ID, ARPErrorNumbers.EM_IGNORE);

		ARPHandlers handlers = arp.getHandlers();
		handlers.setErrorHandler(logger.getSAXErrorHandler());
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
			arp.load(new BufferedInputStream( new FileInputStream(source)), destin.getBase());
			destin.close();
		} catch (SAXException e) {
			throw new TerminateParseException("fatal parse error", e);
		} catch (IOException e) {
			throw new TerminateParseException("error accessing source model", e);
		}
	}
	
	private void push(SplitWriter quoteWriter) {
		stack.add(destin);
		destin = quoteWriter;
	}
	
	private void pop() {
		destin = (SplitWriter) stack.remove(stack.size()-1);
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
				destin.add(getURI(subj), pred.getURI(), getURI(obj));
			} catch (IOException e) {
				throw new TerminateParseException("error writing to split model", e);
			}
		}

		public void statement(AResource subj, AResource pred, ALiteral lit) {
			try {
				destin.add(getURI(subj), pred.getURI(), lit.toString(), lit.getDatatypeURI());
			} catch (IOException e) {
				throw new TerminateParseException("error writing to split model", e);
			}
		}

		private String getURI(AResource node) {
			if( node.isAnonymous())
				return destin.createAnon(node.getAnonymousID());
			else
				return node.getURI();
		}
	}
	
	private class QuotedStatements extends Statements implements QuotedStatementHandler {
		
		public void startQuote(AResource quote) {
			push(destin.createQuote(quote.getAnonymousID()));
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
