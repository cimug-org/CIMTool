package au.com.langdale
package util

import java.util.Properties;
import java.io.{File, FileInputStream, IOException, FileOutputStream}

trait Settings {
  
  val name: String
  val dir: String
  val description: String
  
  private lazy val propsFile = new File(dir, name)
  private var dirty = false
  
  lazy val props = {
    val props = new Properties
    try {
      val stream = new FileInputStream(propsFile)
      try {
        props.load(stream)
      }
      finally {
        stream.close
      }
    }
    catch {
      case ex: IOException =>
        Log.error("Problem reading " + propsFile, ex)
    }
    props
  }

  def save = {
    if( dirty ) {
      val stream = new FileOutputStream( propsFile )
      try {
        props.store( stream, description)
        dirty = false
      }
      finally {
        stream.close
      }
    }
  }
      
  def get(names: String*) = {
    val value = props.getProperty( names.mkString("."))
    if( value == null ) throw new NoSuchElementException
    value
  }
  
  def set(args: String*) {
    val value = args.last
    val name = args.take(args.length-1).mkString(".")
    val was = props.setProperty( name, value )  
    dirty ||= was == null || value != was
  }
}
