package au.com.langdale
package actors

trait Operation 

object Operation {
  // commands
  case class Create[E](entity: E) extends Operation
  case class Update[E](entity: E) extends Operation
  case class Delete[E](entity: E) extends Operation
  
  // results
  trait Effect
  case class Result[V](op: Operation, value: V) extends Effect
  case class Success(op: Operation) extends Effect
  case class Failure(op: Operation, reason: String) extends Effect
  case class Notify(op: Operation)
  case object GeneralNotify
}
