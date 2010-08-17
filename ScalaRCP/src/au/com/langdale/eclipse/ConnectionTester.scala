package au.com.langdale
package eclipse
import scala.actors.Actor._
import java.sql.SQLException
import db.Spec

trait ConnectionTester {
  case class TestResult(result: String)
    
  def runTest(spec: Spec, localcast: UIActor) = actor {
    try {
      spec.connect.close
      localcast ! TestResult("OK.")
    }
    catch {
      case ex: SQLException => 
        localcast ! TestResult("failure:\n" + ex.toString)
    }
  }
}
