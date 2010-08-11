package au.com.langdale
package db
import scala.collection.mutable.{HashMap}
import java.sql.{Connection, DriverManager, SQLException }
import java.util.Properties
import util.{Descriptor, Settings}

object Spec {
  
  private val specs = new HashMap[String, Spec]
  
  def apply( key: String ) = specs(key)
  
  def update(key: String, spec: Spec) { specs(key) = spec }
  
  def save( settings: Settings ) {
    for((key, spec)  <- specs)
      spec.saveTo(key, settings) 
    settings.save
  }
}

trait Spec {
  def uri: String
  def info: String
  def saveTo( key: String, settings: Settings): Unit
  def connect = DriverManager.getConnection(uri)
}