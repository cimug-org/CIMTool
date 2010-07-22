package au.com.langdale.util
import scala.collection.Map
import scala.collection.mutable.{ListBuffer, HashMap}

object CommandLine {
  val OPTIONAL_FLAG = "--([^=]+)"r
  val OPTIONAL_VALUE = "--([^=]+)=(.*)"r
  
  def parse(args: Array[String], defaults: (String, String)*): (List[String], Map[String, String]) = {
    val params = new ListBuffer[String]
    val options = new HashMap[String, String]
    
    options ++= defaults
    
    for( arg <- args ) arg match {
      case OPTIONAL_VALUE(name, value) => options(name) = value
      case OPTIONAL_FLAG(name) => options(name) = ""
      case param => params += param
    }
    
    (params.toList, options)
  }
}
