package au.com.langdale
package eclipse
import actors.Publisher
import actors.Publisher._
import scala.actors.Actor._

trait Subscriber extends DisposeChain {
  val broadcast: Publisher
  case object Dispose
  
  def subscribe( body: PartialFunction[Any, Unit]) = actor {
    broadcast ! Subscribe(self)
    
    loop {
      react {
        case Dispose =>
          broadcast ! Unsubscribe(self)
          exit  
        
        case mesg if body.isDefinedAt(mesg) => 
          body(mesg)
        
        case _ =>
      }
    }
  }
  
  abstract override def disposeChain {
	super.disposeChain
    broadcast ! Dispose
  }
}
