/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
/**
 * A task to import an RDF/XML document into the project 
 * as an HSQL (or other) RDB using Jena utilities and conventions. 
 */
public class RDBImporter implements IWorkspaceRunnable {
	private String source;
	private IProject project;
	private String name;
	private Model model;
	private IProgressMonitor monitor;
	
	private static final String DB_DRIVER = "org.hsqldb.jdbcDriver";       // path of driver class
	private static final String DB_URL =    "jdbc:hsqldb:file:";   // URL of database 
	private static final String DB_USER =   "sa";                          // database user id
	private static final String DB_PASSWD = "";                            // database password
	private static final String DB =        "HSQL";                        // database type

	public RDBImporter(String source, IProject project, String name) {
		super();
		this.source = source;
		this.project = project;
		this.name = name;
	}
	public void run(IProgressMonitor monitor) throws CoreException {
		this.monitor = monitor;
		
		try {
			Class.forName (DB_DRIVER);
		} catch (ClassNotFoundException e) {
			throw Info.error("the database driver is not available: " + DB_DRIVER, e);
		}

		IFolder instances = Info.getInstanceFolder(project);
		String dburl = DB_URL + instances.getFile(name).getLocation().toOSString();

		// Create database connection
		IDBConnection conn = new DBConnection ( dburl, DB_USER, DB_PASSWD, DB );
		try {
			try {
				conn.getConnection().setAutoCommit(false);
			} catch (SQLException e) {
				throw Info.error("error setting up database connection " + name, e);
			}
			
			// create or open the default model
			ModelMaker maker = ModelFactory.createModelRDBMaker(conn) ;
			model = maker.createDefaultModel();

			// load it up with the source document
			load();
		}
		finally {
			
			// Close the database connection
			try {
				conn.close();
			} catch (SQLException e) {
				throw Info.error("error closing database " + name, e);
			}		

			// tell eclipse we made some files
			instances.refreshLocal(1, monitor);
		}
	}
	
	// TODO: use ARP and collect parse errors
	private void load() throws CoreException {
		InputStream input;
		try {
			input = new BufferedInputStream ( new FileInputStream(source));
		} catch (FileNotFoundException e) {
			throw Info.error("error opeing source file " + source, e);
		}
		
		model.begin();
		model.read(input, new File(source).toURI().toASCIIString());
		model.commit();
		model.close();
	}
}
