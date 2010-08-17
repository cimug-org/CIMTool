package au.com.langdale
package actors
import scala.actors.OutputChannel
import Operation._

/**
 * A client is not necessarily an actor, but will communicate with an actor.
 */
trait Client {
  val broadcast: OutputChannel[Any]
  
  def request(op: Operation) = {
    Log("Initiating: " + op)
    (broadcast.receiver !? op) match {
      case Success(`op`) => Log("Completed: " + op); None
      case Failure(`op`, mesg) => Log("Failed: " + op); Some(mesg)
      case _ => Some("Unexpected result for: " + op)
    }
  }

  def requestReply(op: Operation)(handler: Any => Unit): Option[String] = {
    Log("Initiating: " + op)
    (broadcast.receiver !? op) match {
      case Result(`op`, r)  => handler(r); None
      case Failure(`op`, mesg: String) => Log("Failed: " + op); Some(mesg)
      case _ => Some("Unexpected result for: " + op)
    } 
  }
}
