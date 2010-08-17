package au.com.langdale
package actors
import scala.actors.{OutputChannel, Channel, Actor}
import scala.collection.mutable.ArrayBuffer
import scala.actors.!
import scala.actors.Future
import Producer._

class Producer[A](process: Process[A], n: Int) extends Process[A] {
  
  def apply(c: Consumer[A]) = process(c)
  
  def |(c: Consumer[A]) = process(c)
  
  def |[B](t: Transform[A,B]) = new Producer[B](process compose t, n)
  
  val toFutureSeq: Future[Seq[A]] = {
    val (c, s) = futureConsumer[A]
    this | c
    s
  }

  def foreach( f: A => Unit) = this | asyncConsumer(f)
  
  def map[B]( f: A => B) = if(n==1) this | mapper(f) else this | parallelMapper(n)(f)
  
  def flatMap[B](f: A => Producer[B]) = this | parallelFlatMapper(n)(f)
  
  def filter(p: A => Boolean) = this | filterFlow(p)

  def reduce[B](b0: => B)(f: (B, A) => B) = this | reducer(b0)(f)
}

object Producer {
  
  type Consumer[A] = OutputChannel[Option[A]]
  type Transform[A,B] = Consumer[B] => Consumer[A]
  type Process[A] = Consumer[A] => Unit
  
  def apply[A](ai: Iterable[A], n: Int) = new Producer[A](process(ai), n)

  def process[A](ai: => Iterator[A]): Process[A] = {
    output => new Actor {
      def act {
        for(a <- ai) output ! Some(a)
        output ! None
      }
      start
    }
  }
  
  def process[A](ai: Iterable[A]): Process[A] = process(ai.elements)
  
  def mapper[A,B](f: A => B): Transform[A,B] = {
    output => new Actor {
      def act = react {
        case Some(a) => output ! Some(f(a.asInstanceOf[A])); act  
        case None => output ! None
      }
      start
    }
  }
  
  def reducer[A,B](b0: => B)(f: (B, A) => B): Transform[A,B] = {
    output => new Actor {
      private var b = b0
      def act = react {
        case Some(a) => b = f(b, a.asInstanceOf[A]); act  
        case None => output ! Some(b); output ! None
      }
      start
    }
  }
  
  def flatMapper[A,B](f: A => Iterable[B]): Transform[A,B] = {
    output => new Actor {
      def act = react {
        case Some(a) => f(a.asInstanceOf[A]) foreach {output ! Some(_)}; act  
        case None => output ! None
      }
      start
    }
  }
  
  def filterFlow[A](p: A => Boolean): Transform[A,A] = {
    output => new Actor {
      def act = react {
        case Some(a) => val a1 = a.asInstanceOf[A]; if(p(a1)) {output ! Some(a1)}; act  
        case None => output ! None
      }
      start
    }
  }

  def parallelMapper[A,B](n: Int)(f: A => B): Transform[A,B] = {
    output => new Actor {
      
      val internal = new Channel[Option[B]](this)
      var pool = (for( i <- 0 until n ) yield mapper(f)(internal)) toList
      
      def act = react {
        case internal ! Some(b) => 
          output ! Some(b.asInstanceOf[B])
          pool = sender :: pool
          act
          
        case Some(a) if ! pool.isEmpty =>
          val w = pool.head
          pool = pool.tail
          w ! Some(a.asInstanceOf[A])
          act
          
        case None if pool.length == n => 
          for(w <- pool) w ! None
          output ! None
      }
      start
    }
  }
  
  def parallelFlatMapper[A,B](n: Int)(f: A => Process[B]): Transform[A,B] = {
    output => new Actor {
      
      val internal = new Channel[Option[B]](this)
      var pool = n
      
      def act = react {
        case internal ! Some(b) => 
          output ! Some(b.asInstanceOf[B])
          act
          
        case internal ! None =>
          pool += 1
          act
          
        case Some(a) if pool > 0 =>
          pool -= 1
          f(a.asInstanceOf[A]) apply internal
          act
          
        case None if pool == n => 
          output ! None
      }
      start
    }
  }

  def asyncConsumer[A]( f: A => Unit): Consumer[A] = new Actor {
    def act = react {
      case Some(a) => f(a.asInstanceOf[A]); act
      case None =>
    }
    start
  }
  
  def futureConsumer[A]: (Consumer[A], Future[Seq[A]]) = {
    case object Get
    
    val a = new Actor {
      val buf = new ArrayBuffer[A]

      def act = react {
        case Some(a) => buf += a.asInstanceOf[A]; act
        case None => 
          react {
            case Get => sender ! buf
          }
        
      }
      start
    }
    
    (a, (a !! Get).asInstanceOf[Future[Seq[A]]])
  }
  
  def mapReduce[A,B,C](a: Seq[A], c0: => C, n: Int, f: A => B, g: (C, B) => C) =
    Producer(a, n).map(f).reduce(c0)(g).toFutureSeq()
}
