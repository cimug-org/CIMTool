/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.reasoner.ValidityReport;
/**
 * A central point for issuing messages.  This class does not assume any eclipse 
 * conventions but provides enough hooks that a monitor instance could be adapted.
 * 
 * For now, the messages are simply written to a stream.  
 */
public class Logger {
	protected PrintWriter out;
	protected int count;
	
	public Logger(OutputStream stream) {
		out = new PrintWriter(stream);
	}

	public void log(ValidityReport report) {
		if (! report.isValid()) {
			for (Iterator it = report.getReports(); it.hasNext(); ) {
				log(it.next().toString());
			}
		}
	}

	public void log(Exception e) {
		log(e.getMessage());
	}

	public void log(String message) {
		out.println(message);
		count += 1;
	}

	public void estimate(int amount) {
		// override to declare the total amount of work 
	}

	public void worked(int amount) {
		// override to indicate progress
	}

	public int getErrorCount() {
		return count;
	}

	public RDFErrorHandler getRDFErrorHandler() {
		return new RDFErrorHandler() {
			public void error(Exception e) {
				log(e);			
			}

			public void fatalError(Exception e) {
				log(e);			
			}

			public void warning(Exception e) {
				log(e);			
			}
		};
	}
	
	public ErrorHandler getSAXErrorHandler() {
		return new ErrorHandler() {

			public void error(SAXParseException error) throws SAXException {
				log(error);
			}

			public void fatalError(SAXParseException error) throws SAXException {
				log(error);
			}

			public void warning(SAXParseException error) throws SAXException {
				log(error);
			}
			
		};
	}

	public void flush() {
		out.flush();
	}

	public void close() throws IOException {
		out.close();
		if( out.checkError())
			throw new IOException("error writing log file");
	}

}
