package au.com.langdale
package db

import scala.collection.mutable.ListBuffer
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.OutputChannel

import SettingsAgent._
import actors.Publisher.Subscribe
import DBA._
import SQLQuoter._

import java.sql.{SQLException, Connection}

object ConnectionHolder {
  trait ConnectionOp
  case class Take( key: String, workspace: String ) extends ConnectionOp
  case class Give(key: String, workspace: String, connection: Connection) extends ConnectionOp
  case class Release(key: String, connection: Connection) extends ConnectionOp
  case class ConnectionChange( key: String ) extends ConnectionOp
  case class ConnectionProblem( key: String, descr: String) extends ConnectionOp
  case object GlobalRefresh extends ConnectionOp
}

import ConnectionHolder._

/**
 * An actor that manages a pool of database connections and lends them to other actors.
 * 
 * The key parameter identifies the type of connection handled by a particular ConnectionHolder. 
 * All messages recognised by the actor are tagged by this key value.
 * 
 * The actor begins by waiting for a Setting message that provides a connection specification.   
 * 
 * The main loop accepts Take messages and responds to each with a Give message containing a connection.
 * This connection should eventually be returned to the ConnectionHolder with a Release message. 
 *
 * The state of the connection in a Give message is clean. It is set to a specific workspace and 
 * contains no open transaction. It is not in auto commit mode. 
 * 
 * If the creation or initialisation of connection fails while responding to a Take message,
 * a ConnectionProblem message is broadcast and no Give message is sent.
 * 
 * If the number of connections on loan reaches the quota, given as a parameter, the  response to a Take
 * message is delayed until a Release message is received.
 * 
 * If a Settings message is received that contains a connection specification different to that in use,
 * any connections not on loan are closed and a SettingsChange message is broadcast. Those connections
 * currently on loan are closed as they are returned.
 * 
 * Broadcasts are to the notifier channel given as a parameter.
 * 
 */
class ConnectionHolder(val key: String, val quota: Int, notifier: OutputChannel[Any]) extends Actor {
  
  var spec: Option[Spec] = None
  
  private var count = 0
  private val pool= new ListBuffer[Connection] 
  private val ready = new ListBuffer[Connection]
  
  def act {
    // Log("ConnectionHolder ")
    
    notifier ! NeedSetting(key)
    react {
      case Setting( `key`, s: Spec ) =>
	    spec = Some(s)
        main
    }
  }
  
  def main: Nothing = react { 
    case Setting( `key`, s: Spec ) => change(s); main
    case Take( `key`, ws ) => reply(allocate(ws)); main
	case Release(`key`, c) => deallocate(c); main  
    case _ => main
  }
 
  private def allocate(ws: String): ConnectionOp = {
  
    def connect: Connection = {
      if(count >= quota) {
        forward(Take(key, ws))
        react {
          case Release(`key`, c) => deallocate(c); main
        }
      }
        
      val c = spec.get.connect
      c.setAutoCommit(false)
      count += 1
      pool += c
      c
    }
  
    try {
      val c = if( ready.isEmpty ) connect else ready.remove(0)
      c goto ws
      Give(key, ws, c)
    }
    catch {
      case e: SQLException =>
        Log.error("Connection problem", e)
        ConnectionProblem(key, e.getMessage)
    }
  }
  
  private def deallocate(c: Connection) {
    try {
      if( c.getAutoCommit )
        c.setAutoCommit(false)
      else
        c.rollback

      if( pool contains c ) {
        c goto LIVE
        ready += c
      }
      else 
        safeClose(c) 
    }
    catch {
      case e: SQLException => 
        Log.error("Connection deallocation problem",e)
        pool -= c
        safeClose(c)
    }
  }
  
  private def change(s: Spec) {
    val given = Some(s)
    if(spec != given) {
      closeAll
      spec = given
      notifier ! ConnectionChange( key )
    }
  }
  
  private def safeClose(c: Connection) {
    try {
      c.close
    }
    catch {
      case e: SQLException => Log.error("Problem closing connection", e)
    }
    count -= 1
  }
  
  private def closeAll { 
    for( c <- ready ) safeClose(c) 
    pool.clear
    ready.clear
  }
}
