package au.com.langdale
package util
import java.io.{FileOutputStream, OutputStream, PrintStream, IOException, BufferedOutputStream, File}
import java.util.Date

trait Redirector {
  def apply[A](name: String)( block: => A ): A
}

object Redirector {
  
  object NullStream extends OutputStream {
    def write(c: Int) {}
  }
  
  object FileRedirector extends Redirector {
     def apply[A](name: String)( block: => A ): A = Console.withOut( new FileOutputStream(name) ) { block }
  }
  
  object NullRedirector extends Redirector {
    def apply[A](name: String)( block: => A ): A = Console.withOut( NullStream ) { block }
  }
}