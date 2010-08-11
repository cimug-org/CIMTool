package au.com.langdale
package actors
import scala.actors.Actor._
import Operation._
import scala.actors.OutputChannel

trait Server {
  val broadcast: OutputChannel[Any]
 
  def perform( op: Operation )( imp: => Unit ) {
    val client = sender

    try {
      imp
      client ! Success(op)
      broadcast ! Notify(op)
    }
    catch {
      case ex: Exception =>
        client ! Failure(op, ex.getMessage)
        
      case er: OutOfMemoryError =>
        er.printStackTrace(Console.out)
        client ! Failure(op, "Memory exhausted, please restart the application")

      case er: StackOverflowError =>
        er.printStackTrace(Console.out)
        client ! Failure(op, "Memory exhausted, please restart the application")
    }
  }

}
