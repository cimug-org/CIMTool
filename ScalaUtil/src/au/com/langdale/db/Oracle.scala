package au.com.langdale
package db

import scala.collection.mutable.{HashMap}
import java.sql.{Connection, DriverManager, SQLException }
import java.util.Properties
import util.{Descriptor, Settings}
import java.io.File

import oracle.jdbc.OracleDriver;

object Oracle {
  
  trait OracleCommonSpec extends Spec {
    def dbName: String
    def schema: String
    def password: String
    
    def uri = "jdbc:oracle:thin:" + schema + "/" + password + "@" + dbName 
      
    override def connect = {
      Log("Connecting to " + info)
      if( schema == "SYS" ) {
        val props = new Properties
        props.put("user", schema)
        props.put("password", password)
        props.put("internal_logon", "SYSDBA")
        props.put("database", dbName)
        DriverManager.getConnection("jdbc:oracle:thin:@", props)    
      }
      else {
        super.connect
      }
    }
    
    def changeSchema(schema: String, password: String): OracleCommonSpec
  }
  
  case class OracleSpec( host: String, sid: String, schema: String, password: String ) extends OracleCommonSpec {
    def dbName = "//" + host + "/" + sid
    def info = "host: " + host + " sid: " + sid + " schema: " + schema
    def saveTo( key: String, settings: Settings) {
      settings.set(key, "connection", "direct")
      for( prop <- OracleSpec.toList) settings.set(key, "direct", prop.name, prop.get(this).toString)
    }
    def changeSchema(schema: String, password: String) = OracleSpec(host, sid, schema, password)
  }
  
  object OracleSpec extends Descriptor[OracleSpec] {
    val host     = Prop("Host name", "host", _.host)
    val sid      = Prop("Database Service", "sid", _.sid)
    val schema   = Prop("Schema or User", "schema", _.schema)
    val password = Prop("Password", "password", _.password)
  }
  
  case class OracleTNSSpec( dbName: String, schema: String, password: String ) extends OracleCommonSpec {
    def info = "tns name: " + dbName + " schema: " + schema
    def saveTo( key: String, settings: Settings) {
      settings.set(key, "connection", "tns")
      for( prop <- OracleTNSSpec.toList) settings.set(key, "tns", prop.name, prop.get(this).toString)
    }
    def changeSchema(schema: String, password: String) = OracleTNSSpec(dbName, schema, password)
  }
  
  object OracleTNSSpec extends Descriptor[OracleTNSSpec] {
    val dbName  = Prop("Database TNS Name", "dbName", _.dbName)
    val schema   = Prop("Schema or User", "schema", _.schema)
    val password = Prop("Password", "password", _.password)
  }
  
  def setupTNS = {
    
    def valid(admin: String) = {
      (new File(admin, "tnsnames.ora")).isFile
    }
    
    val admin = System.getProperty("oracle.net.tns_admin")
    
    if( admin != null) {
      valid(admin)
    }
    else {
      
      val home = System.getenv("NVCS_ORACLE_HOME")
      
      if( home != null ) {
        
        val admin = List( home, "network", "admin" ).mkString(File.separator)
        val found = valid(admin)

        if( found )
          System.setProperty("oracle.net.tns_admin", admin)
        
        found
      }
      else {
        false
      }
    }
  }
  
  def setupDriver {}
  
  def extractSpec( key: String, settings: Settings ): Option[OracleCommonSpec] = {
    try {
      val method = settings.get(key, "connection")
      
      if(method.equalsIgnoreCase("tns")) {
        import OracleTNSSpec._
        implicit def getter(prop: Prop[_]) = settings.get( key, "tns", prop.name )
        Some(OracleTNSSpec(dbName, schema, password))
      }
      else if(method.equalsIgnoreCase("direct")) {
        import OracleSpec._
        implicit def getter(prop: Prop[_]) = settings.get( key, "direct", prop.name )
        Some(OracleSpec( host, sid, schema, password ))
      }
      else 
        None
    }
    catch {
      case _:NoSuchElementException => None
    }
  } 
  
  def add(key: String, host: String, sid: String) { add( key, host, sid, "memory") }
  def add(key: String, host: String, sid: String, password: String) { Spec(key) = OracleSpec( host, sid, key, password) }
  
  DriverManager.registerDriver(new OracleDriver);
}
