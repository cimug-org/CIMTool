/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class FEAPExtractor extends AbstractEAProjectDBExtractor {

	private static final String USERNAME = "SYSDBA";
	private static final String PASSWORD = "masterkey";

	public FEAPExtractor(File file) {
		super(file);
	}

	@Override
	protected void dbInit() throws SQLException {
		try {
			Class.forName("org.firebirdsql.jdbc.FBDriver");
		} catch (ClassNotFoundException e) {
			System.err.println("Firebird JDBC driver not found.");
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Connection getConnection() throws SQLException {
		/**
		 * The default Firebird DB administrator credentials are SYSDBA/masterkey. EA
		 * Firebird project files (.feap) can be accessed with these defaults.
		 */
		String connectionURL = "jdbc:firebirdsql:embedded:" + file.getAbsolutePath();
		return DriverManager.getConnection(connectionURL, USERNAME, PASSWORD);
	}

}
