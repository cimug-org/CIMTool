package au.com.langdale.cimcheck
import util.CommandLine.parse
import Console.err
import java.io._

object CIMCheck {
  
  def main( args: Array[String]): Unit = parse(args) match {

    case (args, opts) if args.length >= 2 =>
      
      val checker = new Checker( args head, opts get "rules" )
      val output = opts get "output" map createOutputStream getOrElse System.out
      val work = opts get "work" getOrElse "cimcheck-temp-work"
      var errors = 0

      for( modelName <- args.tail ) 
        errors += checker.check(modelName, work, output)
      
      output.close
      
      if(errors > 0) {
        err.println("Errors found: " + errors)
        exit(1)
      }
      else
        exit(0)
      
    case _ => 

      err.println(usage)
      exit(2)
  }
  
  private def createOutputStream(name: String) = new BufferedOutputStream(new FileOutputStream(name))
  private def opt[A](a: A) = if(a != null) Some(a) else None
  
  def createChecker(profileName: String, ruleName: String) =
    new Checker(profileName, opt(ruleName))
  
  val usage = 
    """|CIMCheck version 20100512 - utility for validating CIM/XML documents. 
       |
       |usage: java -jar cimcheck.jar [option...] profile_file model_file...
       |
       |where:
       |
       |  profile_file    is the name of a profile in standalone OWL (.simple-owl) form
       |  model_file      is the name of a CIM/XML file to be validated
       |
       |each option may be:
       |
       |  --rules=rules_file      (default is to use standard validation rules)
       |  --work=temp_working_dir (default is "cimcheck-temp-work" in the current directory)
       |  --output=results_file   (default is standard output)
       |""".stripMargin

}
