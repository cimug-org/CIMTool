package au.com.langdale
package db

import DBA._
import SQLQuoter.ID
import util.Descriptor
import scala.collection.mutable.ListBuffer
import java.sql.ResultSet
import java.util.Date

trait RecordType[T] extends Descriptor[T] {
  
  implicit def get(prop: Prop[String])(implicit rs: ResultSet) = rs.getString(prop.name)
  implicit def get(prop: Prop[Date])(implicit rs: ResultSet): Date = rs.getTimestamp(prop.name)
  implicit def get(prop: Prop[Int])(implicit rs: ResultSet) = rs.getInt(prop.name)
  implicit def get(prop: Prop[Long])(implicit rs: ResultSet) = rs.getLong(prop.name)
  implicit def getDateOption(prop: Prop[Option[Date]])(implicit rs: ResultSet) = {
    val raw = rs.getTimestamp(prop.name)
    if( raw == null ) None else Some(raw)
  }   
  implicit def getStringOption(prop: Prop[Option[String]])(implicit rs: ResultSet) = {
    val raw = rs.getString(prop.name)
    if( raw == null ) None else Some(raw)
  }   

  implicit def get(prop: Prop[_]): SQL = ID(prop.name)
    
  def apply(implicit rs: ResultSet): T
  
  def query(expr: SQL): Query[T] = new Query[T] {
    val sql = expr
    def fetch(implicit rs: ResultSet) = apply(rs)
  }
}
