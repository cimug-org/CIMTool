package au.com.langdale
package util
import org.slf4j.LoggerFactory

class PackageLogger(val name: String) {
  def this() = this { val c = getClass.getName; c.substring(0, c.lastIndexOf('.')) }
  val inner = LoggerFactory.getLogger(name)
  
  def apply( mesg: => Any ) = inner.info(mesg.toString)
  
  def info( mesg: String ) = inner.info(mesg)
  def info(msg: String, arg0: AnyRef, arg1: AnyRef) = inner.info(msg, arg0, arg1)
  
  def debug(msg: String, ex: Throwable) = inner.debug(msg, ex)
  def debug(msg: String, arg0: AnyRef, arg1: AnyRef) = inner.debug(msg, arg0, arg1)

  def warn( mesg: String ) = inner.warn(mesg)
  def warn(msg: String, ex: Throwable) = inner.warn(msg, ex)
  def warn(msg: String, arg0: AnyRef, arg1: AnyRef) = inner.warn(msg, arg0, arg1)

  def error( mesg: String ) = inner.error(mesg)
  def error(msg: String, ex: Throwable) = inner.error(msg, ex)
  def error(msg: String, arg0: AnyRef, arg1: AnyRef) = inner.error(msg, arg0, arg1)
  
  def isDebugEnabled = inner.isDebugEnabled
}
