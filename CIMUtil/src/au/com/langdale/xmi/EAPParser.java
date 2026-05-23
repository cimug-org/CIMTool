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
 * EAProjectParsor implementation to support Sparx EA 15.x (32-bit) project
 * files. Standard in EA 15.x releases and earlier the internal format is based
 * on MS Access. Specifically, .eap files are based on the Jet3.5 engine and
 * .eapx on Jet4.0 (see Access Database Engine History) with both stored as
 * binaries.
 * 
 * The Jet database engine is available only in 32 bit configurations. Which
 * means that the .eap and .eapx file formats are still supported in the 32 bit
 * version of EA 16.x, but not in the new 64 bit version of Enterprise Architect
 * 16.0.
 */
public class EAPParser extends AbstractEAProjectParsor {

	private static final Logger log = LoggerFactory.getLogger(EAPParser.class);

	public EAPParser(String baseURI, File file, boolean selfHealOnImport, boolean validateModel,
			boolean usePackageNames, SchemaImportLogger logger, File namespacesFile) {
		super(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger, namespacesFile);
	}

	public EAPParser(String baseURI, File file, boolean selfHealOnImport, boolean validateModel,
			boolean usePackageNames, SchemaImportLogger logger, File namespacesFile, Set<String> stereotypeExtensions) {
		super(baseURI, file, selfHealOnImport, validateModel, usePackageNames, logger, namespacesFile,
				stereotypeExtensions);
	}

	@Override
	protected void dbInit() throws EAProjectParserException {
		try {
			/* often not required for Java 6 and later (JDBC 4.x) */
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
		} catch (ClassNotFoundException e) {
			log.error("UCanAccess JDBC driver not found on the classpath.", e);
			throw new EAProjectParserException("Unable to read the EA project file:  " + file.getAbsolutePath(), e);
		}
	}

	@Override
	protected void dbShutdown() throws EAProjectParserException {
		// Do nothing...
	}

	/**
	 * <pre>
	 * The following is two connection example calls::
	 * 
	 *  DriverManager.getConnection("jdbc:ucanaccess://<mdb or accdb file path>", user, password);  
	 *  DriverManager.getConnection("jdbc:ucanaccess://c:/pippo.mdb;memory=true;immediatelyReleaseResources=true");
	 * </pre>
	 */
	@Override
	protected Connection getConnection() throws EAProjectParserException {

		try {
			String connectionURL = "jdbc:ucanaccess://" + file.getAbsolutePath()
					+ ";memory=true;immediatelyReleaseResources=true";
			return DriverManager.getConnection(connectionURL);
		} catch (SQLException sqlException) {
			log.error("Failed to open UCanAccess JDBC connection to: {}", file.getAbsolutePath(), sqlException);
			throw new EAProjectParserException("Unable to read the EA project file:  " + file.getAbsolutePath(),
					sqlException);
		}
	}
}
