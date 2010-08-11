package au.com.langdale
package db
import java.sql.ResultSet
import oracle.spatial.geometry.JGeometry
import oracle.sql.STRUCT
import graphic.Geometry._

object ResultExtractor {
  
  implicit def getInt(column: Symbol)(implicit results: ResultSet): Int = results.getInt(column.name)
  implicit def getLong(column: Symbol)(implicit results: ResultSet): Long = results.getLong(column.name)
  implicit def getString(column: Symbol)(implicit results: ResultSet) = results.getString(column.name) 
  implicit def getDouble(column: Symbol)(implicit results: ResultSet) = results.getDouble(column.name) 
  implicit def getStringOption(column: Symbol)(implicit results: ResultSet) = {
    val v = results.getString(column.name) 
    if( v != null ) Some(v) else None
  }
    
  implicit def getShape(column: Symbol)(implicit rs: ResultSet, transform: (Double, Double) => Point) = 
    Spatial.structToShape(rs.getObject(column.name)) 
}
