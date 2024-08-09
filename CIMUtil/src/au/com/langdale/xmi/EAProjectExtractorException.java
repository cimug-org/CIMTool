/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

public class EAProjectExtractorException extends Exception {

	private static final long serialVersionUID = 1L;

	public EAProjectExtractorException() {
	}

	public EAProjectExtractorException(String message) {
		super(message);
	}

	public EAProjectExtractorException(Throwable cause) {
		super(cause);
	}

	public EAProjectExtractorException(String message, Throwable cause) {
		super(message, cause);
	}

	public EAProjectExtractorException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
