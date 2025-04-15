/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class QEAParser extends AbstractEAProjectDBParsor {

	public QEAParser(File file, boolean selfHealOnImport, SchemaImportLogger logger) {
		super(file, selfHealOnImport, logger);
	}

	@Override
	protected void dbInit() throws EAProjectParserException {
		// Nothing to initialize for SQLite as the JDBC Driver is self-registering
	}

	@Override
	protected void dbShutdown() throws EAProjectParserException {
		// Do nothing...
	}

	@Override
	protected Connection getConnection() throws EAProjectParserException {
		try {
			String connectionURL = "jdbc:sqlite:" + file.getAbsolutePath();
			return DriverManager.getConnection(connectionURL);
		} catch (SQLException sqlException) {
			sqlException.printStackTrace(System.err);
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}
}
