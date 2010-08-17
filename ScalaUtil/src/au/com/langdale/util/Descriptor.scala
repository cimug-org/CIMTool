package au.com.langdale
package util
import scala.reflect.Manifest
import scala.collection.mutable.ListBuffer

trait Descriptor[A] {
  
  case class Prop[+B](desc: String, name: String, get: A=>B) {
    def toString(a: A) = desc + ": " + get(a).toString
    val ordinal = props.length
    props += this
  }
    
  val props = new ListBuffer[Prop[Any]]
  def toList = props.toList 
  def mkString(a: A) = props.map( _.toString(a)).mkString 
    
}
