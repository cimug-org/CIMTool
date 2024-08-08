/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class QEAExtractor extends AbstractEAProjectDBExtractor {

	public QEAExtractor(File file) {
		super(file);
	}

	@Override
	protected void dbInit() {
		// Nothing to initialize for SQLite as the JDBC Driver is self-registering
	}

	@Override
	protected Connection getConnection() throws SQLException {
		String connectionURL = "jdbc:sqlite:" + file.getAbsolutePath();
		return DriverManager.getConnection(connectionURL);
	}
}
