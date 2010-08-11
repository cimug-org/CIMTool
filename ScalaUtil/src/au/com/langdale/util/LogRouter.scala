package au.com.langdale
package util
import java.util.Date
import java.io.{OutputStreamWriter, FileOutputStream, OutputStream, PrintStream, IOException, BufferedOutputStream, File}
import Redirector.NullStream

object LogRouter {

  def openLogFile(name: String) {
  
    val dir = System.getProperty("user.home")
    val backupName = name + ".bak"
    
    val target = new File(dir, name)
    val backup = new File(dir, backupName)
    
    try {
      backup.delete
      target renameTo backup
    }
    catch {
      case _: IOException => 
    }

    try {
      val writer = new OutputStreamWriter(new BufferedOutputStream( new FileOutputStream(target)))
      hookException { (tag, mesg, ex) =>
        try {
          writer.write(format(tag, mesg, ex))
          writer.append('\n')
          if(ex.isDefined) {
            writer.write(ex.get.getStackTrace.mkString(Spaces, "\n" + Spaces, "\n"))
          }
          writer.flush
        }
        catch {
          case _: IOException => 
        }
      }
    }
    catch {
      case _: IOException => 
    }
  }
  
  private var disposers: List[(String, String, Option[Throwable]) => Unit] = Nil
  
  def hookException( disposer: (String, String, Option[Throwable]) => Unit ) {
    synchronized { disposers = disposer :: disposers }
  }
  
  def hook( disposer: String => Unit ) {
    hookException((tag, mesg, ex) => disposer(format(tag, mesg, ex)))  
  }

  val Indent = 29
  val Spaces = " "*Indent
  
  def format( tag: String, mesg: String, ex: Option[Throwable]) = 
    tag + " " + mesg + (if(ex.isDefined) "\n" + Spaces + ex.get.toString else "")
  
  def log( mesg: String, ex: Throwable) {
    disposers foreach { d => d((new Date).toString, mesg, Option(ex)) }
  }
}
