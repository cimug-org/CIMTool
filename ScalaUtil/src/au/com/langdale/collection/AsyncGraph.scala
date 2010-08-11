package au.com.langdale
package collection

import scala.actors.Actor._
import util.Graph._

class AsyncGraph {
  
  class Edges extends Responder[Edge] {
    def respond(consume: Edge => Unit) {
      var more = true
      loopWhile(more) {
        react {
          case Some(e: Edge) => consume(e)
          case None => more = false
        }
      }
    }
  }
  
  class Tags extends Responder[Tag] {
    def respond(consume: Tag => Unit) {
      var more = true
      loopWhile(more) {
        react {
          case Some(e: Tag) => consume(e)
          case None => more = false
        }
      }
    }
  }
  
  def edges(n: Node) = {
    new Tags
  }
}

object Example {
  case class Conductor( id: Long )
  
  val ag = new AsyncGraph  
  val n = 42l
  for( e <- ag.edges(n)) {
    e match {
      case c: Conductor => 
        actor {
          
        }
    }
  }
}
