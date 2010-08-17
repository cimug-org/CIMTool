package au.com.langdale
package eclipse
import org.eclipse.core.runtime.jobs.{Job, ISchedulingRule}
import org.eclipse.core.runtime.{IProgressMonitor, IStatus, Status}
import scala.collection.mutable.{HashMap}

object JobCache {
  
  /**
   * Jobs dispatched via the same Sequential intance with be executed one at a time
   * but concurrently with the calling thread and other jobs.
   */
  class Sequential( val name: String ) extends ISchedulingRule {
    def contains(that: ISchedulingRule) = this == that
    def isConflicting(that: ISchedulingRule) = this == that

    /**
     * Execute the given action as a background job.
     */
    def job( action: => Unit) {
      val j = new Job(name) {
        override def run(mon: IProgressMonitor): IStatus = {
          action
          Status.OK_STATUS
        }
      }
      
      j.setRule(this)
      j.schedule
    }
  }
  
  /**
   * Cache the value of some lengthy calculation, defined in a subclass.
   */
  abstract class Cached[A](name: String) extends Sequential(name) {
    protected var value: Option[A] = None
    protected var serial = 0

    /**
     * Request the value, causing it to be calculated if required.
     * 
     * This method returns immediately.  The reply function is called
     * back when the value is ready.  The value is accompanied by a
     * version number that can be used as a proxy for comparison and
     * detecting changes.
     */
    def request( reply: (A, Int) => Unit)
    
    /**
     * Clear the cached value, if any, ensuring it will be 
     * freshly calculated and given a new version number 
     * on the next request.
     */
    def invalidate = job {
      value = None
      serial += 1
    }
  }
  
  /**
   * A cache of the value of the calculation represented by build. 
   * 
   * The name argument is assigned to background jobs for information only.
   */
  class Independent[A]( name: String)( build: => A ) extends Cached[A](name) {
    def request( reply: (A, Int) => Unit) = job {
      if( value.isEmpty )
        value = Some(build)
      reply(value.get, serial)
    }
  }
  
  /**
   * A cache of the value of the calculation represented by build which takes
   * another cached value represented by prereq as an input. 
   */
  class Dependent[A,B](name: String, prereq: Cached[B])( build: B => A)  extends Cached[A](name) {
    private var check = 0
    
    def request( reply: (A, Int) => Unit) = prereq request { (b, s) =>
      job {
        if( value.isEmpty || s != check)
          value = Some(build(b))
        check = s
        reply(value.get, serial)
      }
    }
  }
  
  /**
   * A confusing DSL for creating cached values.
   */
  def store(name: String) = new {
    def as[A](build: => A) = new Independent(name)(build)
    def using[B](prereq: Cached[B]) = new {
      def as[A](build: B => A) = new Dependent(name, prereq)(build)
    }
  }
  
  /**
   * Cache the value of some lengthy calculation, build, indexed by its input.
   */
  class CachedMap[A,B](name: String)( build: A => Cached[B] ) extends Sequential(name) {
    private val memo = new HashMap[A,Cached[B]]
    
    def request( a: A)( reply: (B, Int) => Unit) = job {
      val cb = 
        if( memo contains a ) {
          memo(a)
        }
        else {
          val fresh = build(a)
          memo(a) = fresh
          fresh
        }
      cb.request(reply)
    }
    
    def invalidate( a: A ) = job {
      if( memo contains a)
        memo(a).invalidate
    }
  }
    
  /**
   * Cache the value of some lengthy calculation, build, indexed by its input.
   */
  class SimpleCachedMap[A,B](name: String)( build: A => B ) extends Sequential(name) {
    private val memo = new HashMap[A,(B, Int)]
    private var serial = 0
    
    def request( a: A)( reply: (B, Int) => Unit) = job {
      val (b, s) = 
        if( memo contains a ) {
          memo(a)
        }
        else {
          serial += 1
          val fresh = (build(a), serial)
          memo(a) = fresh
          fresh
        }
      reply(b, s)
    }
    
    def invalidate( a: A ) = job {
      memo -= a
    }
  }
}
