package au.com.langdale
package actors
import scala.actors.Actor
import scala.actors.Actor._
import Operation._
import Jobber._

object Jobber {
  trait Job extends Operation with Function0[Unit]
  case object QueryStatus
  case class InProgress(op: Operation) extends Effect
  case object Idle extends Effect
}

class Jobber extends Actor {
  
  private var status: Effect = Idle
  private var ready = true
 
  def act = loop {
    react {
      case op: Job if ready =>
        status = InProgress(op)
        ready = false
        perform(op)
        
      case result: Effect =>
        status = result
        ready = true
        
      case QueryStatus => sender ! status
      
      case _ =>
    
    }
  }

  private val coordinator = this
  
  private def perform(op: Job) = new Actor {
    def act = try {
      op()
      coordinator ! Success(op)
    }
    catch {
      case ex => coordinator ! Failure(op, ex.toString)
      Log error ex.toString
    }
    start
  }
}


