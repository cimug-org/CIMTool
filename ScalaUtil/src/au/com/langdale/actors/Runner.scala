package au.com.langdale
import scala.actors.{OutputChannel, Channel, Actor}
import scala.collection.mutable.ArrayBuffer
import scala.actors.!
import actors.Producer._
import Runner._

trait Runner[A,B] extends (A => B) with OutputChannel[A] { this: Actor => 
  val monitor: OutputChannel[Poll.type] 
  def apply(mesg: A): B = !?(mesg).asInstanceOf[B]
}

object Runner {
  
  case object Poll
  case object Busy
  case object Idle  
  
  def runner[A, B](transform: Transform[A,B]): Runner[Seq[A],Seq[B]] = new Actor with Runner[Seq[A],Seq[B]] {

    val preprocessor = flatMapper[Seq[A],A](x => x)
    val postprocessor = reducer[B, ArrayBuffer[B]](new ArrayBuffer[B]){(bs, b) => bs += b; bs}
    val processor = preprocessor compose transform compose postprocessor
    val internal = new Channel[Option[Seq[B]]](this)
    val monitor = new Channel[Poll.type](this)

    def act {
      
      react { 
        case a: Seq[_]  => 
          val client = sender
          val pipe = processor apply internal
          pipe ! Some(a.asInstanceOf[Seq[A]])
          pipe ! None

          react {        
            case internal ! Some(b) => 
              client ! b.asInstanceOf[Seq[B]]
              
              react { 
                case internal ! None => act
              }
              
            case monitor ! Poll => sender ! Busy 
          }
          
        case monitor ! Poll => sender ! Idle
      }
    }
    start
  }
}
