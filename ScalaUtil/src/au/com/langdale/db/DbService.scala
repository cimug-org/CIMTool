package au.com.langdale
package db
import actors.Server
import scala.actors.Actor._
import actors.Operation
import actors.Operation._
import scala.actors.OutputChannel
import java.sql.{Connection,SQLException}
import db.ConnectionHolder._

trait DbService extends Server {
  val broadcast: OutputChannel[Any]
  val key: String
  
  private def withConnection(op: Operation, ws: String)( imp: Connection => Unit): Nothing = {
    val client = sender
    
    broadcast ! Take(key, ws)
    
    react {
      case Give(`key`, `ws`, c) =>
        try {
          imp(c)
        }
        catch {
          case ex: Exception =>
            client ! Failure(op, ex.getMessage)
        }
        finally {
          broadcast ! Release( key, c )
        }
        
      case ConnectionProblem(`key`, mesg) =>
        client ! Failure(op, mesg)
    }
  }
  
  def performWithConnection(op: Operation, ws: String)( imp: Connection => Unit) = {
    val client = sender
    
    withConnection(op, ws) { c =>
      imp(c)
      client ! Success(op)
      broadcast ! Notify(op)
    }
  }
  
  def queryWithConnection[R](op: Operation, ws: String)( imp: Connection => R) = {
    val client = sender
    
    withConnection(op, ws) { c =>
      client ! Result[R](op, imp(c))
    }
  }
}
