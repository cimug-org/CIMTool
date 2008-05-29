/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.inference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
/**
 * Lexical analyser for the rule language.  Splits a stream of characters into tokens.
 */
public class RuleLexer {

	// this character class is defined in XML
	public static final String NCNAME_CHARS = "_-."; // plus the alphanumerics 

	// these character classes are defined in rfc3986 for URI's
	public static final String UNRESERVED_CHARS = NCNAME_CHARS + "~";
	public static final String SUB_DELIM_CHARS = "!$&'()*+,;=";
	public static final String PATH_CHARS = UNRESERVED_CHARS + SUB_DELIM_CHARS + ":@"; 
	public static final String FRAGMENT_CHARS = PATH_CHARS + "/?";

	// these are delimiters we can use in a rule following to a qname without whitespace
	public static final String RULE_DELIM_CHARS = "(,)";

	/**
	 * Determine if the character may occour in a URI fragment field
	 * as defined by  rfc3986.
	 */
	public static boolean isFragmentChar(int code) {
		return (Character.isLetterOrDigit(code) || FRAGMENT_CHARS.indexOf(code) != -1) && RULE_DELIM_CHARS.indexOf(code) == -1;
	}

	// End of line and end of input special characters
	private static final int EOL = '\n';
	private static final int EOI = -1;
	
	private Reader source;
	StringBuffer lookahead = new StringBuffer();
	int cursor = 0;

	private int nextLineNumber = 1;
	private int lineNumber = 1;
	/**
	 * Apply lexical analysis to a character stream.
	 * @param source: the character stream.
	 * @throws IOException
	 */
	public RuleLexer( Reader source) throws IOException {
		this.source = source;
	}

	/**
	 * Return the current character or -1 for end of input.  
	 * 
	 * The current character is initially the first character 
	 * in the input stream.  This is changed by next() and revert().
	 * 
	 * This method fills a lookahead buffer by reading the stream
	 * if necessary.
	 */
	private int get() throws IOException {
		while( cursor >= lookahead.length()) {
			int code = source.read();
			if( code == EOI )
				break;
			lookahead.appendCodePoint(code);
		}
		
		if( cursor >= lookahead.length()) 
			return EOI;
		else
			return lookahead.charAt(cursor);
	}
	
	/**
	 * Queue the current character and make the following character
	 * in the input stream current.  
	 * 
	 * Return this character or -1 for end of input.
	 */
	private int next() throws IOException {
		int code = get();
		if( code == EOI) 
			return EOI;
		if( code == EOL )
			nextLineNumber ++;
		
		cursor++;
		return get();
	}
	
	/**
	 * Revert the lexer to its state following the last take(). 
	 * The current character becomes the character after the 
	 * string returned by take(). 
	 */
	private void revert() {
		cursor = 0;
		nextLineNumber = lineNumber;
	}
	
	/**
	 * Return the string of characters that follow the last take() result
	 * in the input stream up to but excluding the current character.
	 * 
	 * That is, the characters queued by next() since the last 
	 * take() or revert().  
	 */
	private String take() {
		String result = lookahead.substring(0, cursor);
		lookahead.delete(0, cursor);
		cursor = 0;
		lineNumber = nextLineNumber;
		return result;
	}
	/**
	 * @return: the line number at which the last reported token was found.
	 */
	public int getLineNumber() {
		return lineNumber;
	}
	/**
	 * Extract a token.  This may advance the input stream beyond the
	 * token in order to find a match.
	 * 
	 * @return: the next token or an empty string on end of input.
	 *  
	 * @throws IOException
	 */
	public String nextToken() throws IOException {
		for(;;) {
			if( Character.isWhitespace(get())) {
				next();
				take();
			}
			else if( get() == '#') {
				next();
				while(get() != EOI && get() != EOL)
					next();
				take();
			}
			else {
				break;
			}
		}
		
		if( get() == '<' && next() == '-') {
			next();
			return take();
		}
		else
			revert();
		
		if(get() == '-' && next() == '>') {
			next();
			return take();
		}
		else
			revert();
		
		if(get() == '^' && next() == '^') {
			next();
			return take();
		}
		else
			revert();

		if( get() == '<' )
			return quoted('>');
		
		if( get() == '"')
			return quoted('"');
		
		if( get() == '\'')
			return quoted('\'');
		
		if(isWord()) { 
			next();
			while( isWord())
				next();
			return take();
		}
		
		next();
		return take();
	}
	
	private String quoted(char delim) throws IOException {
		next();
		for(;;) {
			if(get() == EOI) {
				break;
			}
			else if(get() ==  '\\') {
				next();
				next();
			}
			else if( get() == delim ) {
				next();
				break;
			}
			else {
				next();
			}
		}
		String result = take();
		result.replace("\\\\", "\\");
		result.replace("\\" + delim, "" + delim);
		return result;
	}

	private boolean isWord() throws IOException {
		int code = get();
		return isFragmentChar(code);
	}
	
	public static void main(String[] args) {
		RuleLexer  l;
		
		try {
			l = new RuleLexer( new BufferedReader(new InputStreamReader( RuleLexer.class.getResourceAsStream("/au/com/langdale/cim/cimtool-simple.rules"))));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		for(;;) {
			String token;
			try {
				token = l.nextToken();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			if( token.length() == 0)
				return;
			System.out.println(token + "\t" + l.getLineNumber());
		}
	}
}
