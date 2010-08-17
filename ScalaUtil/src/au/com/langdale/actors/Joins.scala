package au.com.langdale
package actors
import scala.actors.Actor.{self, sender}
import scala.actors.OutputChannel
import scala.collection.immutable.Set

/**
 * Augments the actors library with a form of join pattern similar in function
 * to C omega chords or Haller and Van Cutsem scala join patterns but with a 
 * different notation. 
 *
 * This is a brute force approach but it has the virtue of simplicity and it 
 * handles arbitrary message patterns with guards.
 * .  
 * The module defines three operators: pattern, join and action.  These are used
 * to set out join patterns as nested partial functions forming a tree of cases. 
 * 
 * The patter operator:
 * 
 * pattern { case pat1 => .... case pat2 => ... } 
 * 
 * introduces a tree of join patterns and causes the actor 
 * to dequeue messages until one branch of the join is matched completely.  All
 * events not participating in the successful join are requeued and the action
 * clause is executed.
 * 
 * The join operator:
 * 
 * pattern { case pat1 => join { case pat2 => ... }} 
 * 
 * joins patterns pat1 and pat2. Each must be matched by a distinct message before
 * the join is considered to match but the messages may arrive in any order. 
 * if pat1 and pat2 do not overlap, this simple join has the same behaviour as: 
 * 
 * react { case pat1 => react { case pat2 => ... }} 
 * 
 * If the patterns do overlap, or if the same pattern appears more than once in the
 * tree of cases, then the behaviour of the nested react construct depends on the order
 * of arrival of messages but the join construct does not. 
 * 
 * Here is the simplest branching join pattern:
 *
 * pattern { case pat1 => join { case pat2 => ... case pat3 => ...}}
 * 
 * This defines two joins.  pat1 is joined with pat2 and pat1 is joined with pat3.
 * 
 * Once a join pattern is matched, an action can be taken:
 * 
 * pattern { case pat1 => join { case pat2 => action { act1 }}}
 * 
 * This defines an action, act1, to be executed when the join of pat1 and pat2 is matched.
 * The action can trigger further pattern matching by invoking pattern {...} or
 * (recursively) invoking the enclosing pattern. The loop operator can be used:
 * 
 * loop { pattern { case pat1 => join { case pat2 => action { act1 }}}}
 * 
 * This continuously matches pat1 joined with pat2.
 * 
 * An action may also call react or receive directly and can respond to any messages other 
 * than those that matched the successfule join pattern. 
 * 
 * Here is the standard example for joins.  It implements a queue:
 * 
 * import scala.actors.Actor._
 * import Joins._
 * 
 * case class Put(x: Any)
 * case object Get
 * 
 * actor {
 *   loop {
 *     pattern {
 *       case Put(x) => join { case Get => action { lastSender ! x }}
 *     }
 *   }
 * }
 */
object Joins {

  type Action = Function0[Unit] /// an action performed on a successful join
  type Pattern = PartialFunction[Any,Either[SubPattern,Action]] /// a set of join patterns 
  case class SubPattern(pattern: Pattern) // a set of sub-patterns
  
  /**
   * The sender for actions.
   */
  private val lastSenderBuf = new ThreadLocal[OutputChannel[Any]]  
  def lastSender = lastSenderBuf.get
  
  /**
   * A message received by the actor
   */
  case class Event(index: Int, mesg: Any, sender: OutputChannel[Any])
  
  /**
   * A partially matched join pattern and the sequence of events that lead to it.
   */
  case class PartialMatch(pattern: Pattern, accepted: Set[Int])

  /**
   * Indicates that a match to a complete join pattern has been dispatched
   */
  case object Matched extends Exception
  
  private def diff(a: List[Event], b: Set[Int]) = a filter { x => ! b.contains(x.index) }
  
  /**
   *  The combined partial function for a set of patially matched join patterns.
   */
  class State( matches: List[PartialMatch], history: List[Event], serial: Int) extends PartialFunction[Any,Unit] {

    Log( "state: " + matches.mkString(", "))
    Log( "history: " + history.mkString(", "))
    
    /**
     * Applies one message to each partial match resulting in 
     * additional partial matches or at most one complete match.  
     * 
     * On a complete match, all of the events not used 
     * in that match are resubmitted to the actor.
     */
    def apply(mesg: Any) {
      Log( "received: " + mesg )
      val event = Event(serial, mesg, sender)
      try {
        val expanded = matches flatMap { expand(event, _)  }
        self.react(new State(expanded:::matches, event::history, serial+1))
      }
      catch {
        case Matched =>
      }
    }
    
    private def expand( event: Event, partial: PartialMatch): List[PartialMatch] = {

      Log("considering: " + partial + " with " + event)

      val PartialMatch(pattern, accepted) = partial
      
      if( pattern isDefinedAt event.mesg ) {
        
        val used = accepted + event.index
        val unused = history filter { e => ! used.contains(e.index) }
        
        pattern(event.mesg) match {
              
          case Left(SubPattern(sub)) => 
            val p = PartialMatch(sub, used )
            p :: (unused flatMap {expand(_, p)})
              
          case Right(action) => 
            Log("fire!")
            unused foreach { e => self.send(e.mesg, e.sender) }
            lastSenderBuf.set(event.sender)
            try {
              action()
            }
            finally {
              lastSenderBuf.remove
            }
            throw Matched
        }
      }
      else Nil 
    }
    
    /**
     * Indicates that the event will match at least one join pattern.
     * Alternative would be to return true unconditionally and let the
     * State.history queue up unmatched messages.
     */
    def isDefinedAt(mesg: Any) = true // matches exists { _.pattern isDefinedAt mesg }
  }
  
  def action(action: =>Unit) = Right(()=>action)
  
  def join(pattern: Pattern) = Left(SubPattern(pattern))
  
  def pattern(pattern: Pattern) = self.react(new State(List(PartialMatch(pattern, Set.empty)), Nil, 0))
}
