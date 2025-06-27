/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import au.com.langdale.logging.SchemaImportLogger;

public class FEAPParser extends AbstractEAProjectDBParsor {

	private static final String USERNAME = "SYSDBA";
	private static final String PASSWORD = "masterkey";

	public FEAPParser(File file, boolean selfHealOnImport, SchemaImportLogger logger) {
		super(file, selfHealOnImport, logger);
	}

	@Override
	protected void dbInit() throws EAProjectParserException {
		try {
			Class.forName("org.firebirdsql.jdbc.FBDriver");
		} catch (ClassNotFoundException e) {
			System.err.println("Firebird JDBC driver not found.");
			e.printStackTrace(System.err);
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					e);
		}
	}

	@Override
	protected void dbShutdown() throws EAProjectParserException {
		// Do nothing...
	}

	@Override
	protected Connection getConnection() throws EAProjectParserException {
		/**
		 * The default Firebird DB administrator credentials are SYSDBA/masterkey. EA
		 * Firebird project files (.feap) can be accessed with these defaults.
		 */
		try {
			String connectionURL = "jdbc:firebirdsql:embedded:" + file.getAbsolutePath();
			return DriverManager.getConnection(connectionURL, USERNAME, PASSWORD);
		} catch (SQLException sqlException) {
			sqlException.printStackTrace(System.err);
			throw new EAProjectParserException("Unable to import the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}

}
