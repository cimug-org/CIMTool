/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import au.com.langdale.logging.SchemaImportLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EAProjectParsor implementation to support Sparx EA 16.x (64-bit) project
 * files. Introduced in the 64-bit release of EA 16.x, the internal format of
 * these files is based on the SQLite open source database and stored in binary
 * form. Both file types support basic replication with the .qeax extension
 * indicating that file sharing is enabled. A .qeax file can simply be renamed
 * back to .qea to disable file sharing.
 */
public class QEAParser extends AbstractEAProjectParsor {

	private static final Logger log = LoggerFactory.getLogger(QEAParser.class);

	public QEAParser(String baseURI, File file, boolean selfHealOnImport, boolean validateModel,
			boolean usePackageNames, SchemaImportLogger logger, File namespacesFile) {
		super(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger, namespacesFile);
	}

	public QEAParser(String baseURI, File file, boolean selfHealOnImport, boolean validateModel,
			boolean usePackageNames, SchemaImportLogger logger, File namespacesFile, Set<String> stereotypeExtensions) {
		super(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger, namespacesFile,
				stereotypeExtensions);
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
			log.error("Failed to open SQLite JDBC connection to: {}", file.getAbsolutePath(), sqlException);
			throw new EAProjectParserException("Unable to read the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}
}
