package au.com.langdale
package db
import ConnectionHolder._
import java.sql.Connection
import scala.actors.OutputChannel
import db.DBA.LIVE

/*
 * A trait to allow a thread (not designed as an actor) to 
 * synchronously obtain a database connection.
 */
trait ConnectionBorrower {
  val key: String
  val broadcast: OutputChannel[Any]
  
  def withConnection[A](action: Connection => A) = {
    (broadcast.receiver !? Take(key, LIVE)) match {
      case Give(`key`, LIVE, c) =>
    
        try {
          Some(action(c))
        }
        finally {
          broadcast ! Release(key, c)
        }
        
      case x => 
        Log error "Connection not available: " + x
        None
    }
  }
}
