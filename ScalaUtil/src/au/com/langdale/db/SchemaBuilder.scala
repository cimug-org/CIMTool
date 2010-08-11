package au.com.langdale
package db

import scala.collection.mutable.{HashMap, ListBuffer}
import java.sql.Connection
import SQLQuoter._
import DBA._
import graphic.Geometry.ISO216
import Oracle.OracleSpec

object SchemaBuilder {
  
  def createEmptySchema( sysSpec: Spec, targetSpec: Spec ) {
    val db = sysSpec.connect
    val OracleSpec(_, _, name, password) = targetSpec
    
    db maybe DROP ~ USER ~ ID(name) ~ CASCADE
    
    db exec CREATE ~ USER ~ ID(name) ~
             IDENTIFIED ~ BY ~ SQL(password) ~ 
             DEFAULT ~ TABLESPACE ~ ID("USERS") ~
             QUOTA ~ UNLIMITED ~ ON ~ ID("USERS")
    
    grant( db, name,
//        'DBA   
      CREATE ~ SESSION,
      CREATE ~ TABLE,
      CREATE ~ VIEW,
      CREATE ~ SEQUENCE,
      'WM_ADMIN_ROLE
      //EXECUTE ~ ON ~ ID("MDSYS", "SPATIAL_INDEX")
//      CREATE ~ ANY ~ INDEX,
//      EXECUTE ~ ANY ~ INDEXTYPE
    )
    
    db close
  }
  
  def grant( db: Connection, name: String, privs: SQL* ) {
    for( p <- privs ) db exec GRANT ~ p ~ TO ~ ID( name ) 
  }
}

trait SchemaBuilder {
  
  val all = new ListBuffer[SQL]
  val views = new ListBuffer[Symbol]
  val tableByName = new HashMap[Symbol, Table]
  def tables = tableByName.values
    
  var prevSerial = 0 
  def nextSerial = { prevSerial += 1; prevSerial }

  def table(name: Symbol, columns: (Symbol, SQL)*): Table = {
    val t = Table(name, columns.toList)
    all += t
    tableByName(t.name) = t
    t
  }
  
  def declareExistingTable(name: Symbol, columns: (Symbol, SQL)*): Table = {
    val t = Table(name, columns.toList)
    tableByName(t.name) = t
    t
  }
  
  def insert(name: Symbol, fields: (Symbol, SQL)*) { all += TableRow(name, fields.toList) }
  def call(func: SQL) { all += BEGIN ~ func ~ SQL(";") ~ END ~ SQL(";") }
  def exec(stmnt: SQL) { all += stmnt }

  private val LONG_LAT = SDO_DIM_ARRAY( SDO_DIM_ELEMENT("Longitude", -180, 180, 10), SDO_DIM_ELEMENT("Latitude", -90, 90, 10))
  private val A0 = SDO_DIM_ARRAY( SDO_DIM_ELEMENT("X", 0, ISO216.A0.x, 1), SDO_DIM_ELEMENT("Y", 0, ISO216.A0.y, 1))
 
  def geomMetadataA0(table: Table, field: Symbol) { insertGeomMetadata(table.name, field, A0, NULL) }
  def geomMetadataA0(name: Symbol, field: Symbol) { insertGeomMetadata(name, field, A0, NULL) }
 
  def geomMetadataLonLat(name: Symbol, field: Symbol) { insertGeomMetadata(name, field, LONG_LAT, SRID_WGS_84) }
  
  private def insertGeomMetadata(table: Symbol, field: Symbol, dims: SQL, srid: SQL) {
    this insert( 'USER_SDO_GEOM_METADATA, 
          'TABLE_NAME -> table.name, 
          'COLUMN_NAME -> field.name,
          'DIMINFO -> dims,
          'SRID -> srid)
  }
  
  def key(table: Table, fields: Symbol*) { 
    this exec ALTER ~ TABLE ~ table.name ~ ADD ~ PRIMARY ~ KEY( fields.map(f => ID(f)): _*)
  }

  def spatialIndex( table: Table, field: Symbol ) { 
    this exec CREATE ~ INDEX ~ ID("IDX_" + nextSerial) ~ ON ~ table.name(field) ~  
                     INDEXTYPE ~ IS ~ ID('MDSYS, 'SPATIAL_INDEX) 
  }

  def index( table: Table, field: Symbol ) { 
    this exec CREATE ~ INDEX ~ ID("IDX_" + nextSerial) ~ ON ~ table.name(field) 
  }
  
  def version(table: Table) {
    this call ID('DBMS_WM, 'EnableVersioning)( table.name.name )    
  }
  
  def createTables( spec: Spec) = withConnection(spec) { db =>
    for( stmnt <- all)
      db exec stmnt
  }
  
  def dropTables( spec: Spec ) { withConnection(spec) { dropTables _ }}
  
  def dropTables( db: Connection ) {
    
    for( t <- tables )
      db maybe DROP ~ TABLE ~ t.name
    
    for( s <- tables.map(_.name) ++ views.elements )
      db maybe DELETE ~ FROM ~ 'USER_SDO_GEOM_METADATA ~ WHERE ~ 'TABLE_NAME ~ EQ ~ s.name
     
    for( v <- views )
      db maybe DROP ~ VIEW ~ v
    
  }

  def cleanup( spec: Spec ) = withConnection(spec) { db =>
    for( t <- tables ) {
      db callMaybe ID('DBMS_WM, 'DisableVersioning)( t.name.name, TRUE)
    }
    
    db.commit

    db callMaybe ID('SDO_NET, 'DROP_NETWORK)("NET")
    
    dropTables(db)
   
    db.commit
    
    db maybe SQL("""
      BEGIN 
        FOR w IN (SELECT * FROM all_workspaces WHERE parent_workspace = 'LIVE') 
        LOOP 
          DBMS_WM.RemoveWorkspaceTree (w.workspace); 
        END LOOP; 
        FOR s IN (SELECT * FROM all_workspace_savepoints WHERE removable = 'YES') 
        LOOP 
          DBMS_WM.DeleteSavePoint (s.workspace, s.savepoint); 
        END LOOP; 
      END;""")
    
  }
  
  def dePopulate( spec: Spec ) = withConnection(spec) { db =>
    for( t <- tables)
      db exec DELETE ~ FROM ~ t.name
  }
}
