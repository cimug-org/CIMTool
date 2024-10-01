/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

public class EAProjectParserException extends Exception {

	private static final long serialVersionUID = 1L;

	public EAProjectParserException() {
	}

	public EAProjectParserException(String message) {
		super(message);
	}

	public EAProjectParserException(Throwable cause) {
		super(cause);
	}

	public EAProjectParserException(String message, Throwable cause) {
		super(message, cause);
	}

	public EAProjectParserException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
