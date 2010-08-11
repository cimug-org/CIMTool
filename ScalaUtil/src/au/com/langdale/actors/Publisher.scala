package au.com.langdale
package actors
import scala.actors.{Actor, OutputChannel}
import scala.actors.Actor._

object Publisher {
  case class Subscribe(c: OutputChannel[Any])
  case class Unsubscribe(c: OutputChannel[Any])
  object hub extends Publisher
}
import Publisher._

class Publisher extends Actor {
  private var subscribers: List[OutputChannel[Any]] = Nil
  
  def act {
    // Log("Publisher")
    loop {
      react {
        case Subscribe(c) => subscribe(c)
        case Unsubscribe(c) => subscribers = subscribers remove { _ == c }; /* Log( "Unsubscibe: " + c) */ 
        case m => subscribers filter { _ != sender } foreach { _ forward m } 
      }
    }
  }
  
  def listen(reaction: PartialFunction[Any,Unit]) = actor { 
    this ! Subscribe(self)
    loop(reaction) 
  }
  
  def subscribe(c: OutputChannel[Any]) { subscribers = c :: subscribers; /* Log( "Subscibe: " + c) */ }
}
