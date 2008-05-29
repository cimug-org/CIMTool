package com.hp.hpl.jena.rdf.arp;
/**
 * Extended statement handler to allow RDF quoting.
 */
public interface QuotedStatementHandler extends StatementHandler {
	/**
	 * Start a quote. Following statements are part of the quote 
	 * until the quote is terminated and unless they belong to a nested quote.
	 *  
	 * @param quote: the name of the quote
	 */
	public void startQuote(AResource quote);
	/**
	 * Terminate the most recently started but unterminated quote.
	 */
	public void endQuote();
}
