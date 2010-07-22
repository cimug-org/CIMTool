package au.com.langdale.cimcheck

import au.com.langdale.kena.RDFParser
import au.com.langdale.kena.RDFParser.TerminateParseException
import au.com.langdale.splitmodel.SplitWriter
import au.com.langdale.util.Logger
import au.com.langdale.kena.{OntModel, ModelFactory, IO}
import au.com.langdale.validation.ValidatorUtil.openStandardRules
import au.com.langdale.validation.SplitValidator
import au.com.langdale.inference.StandardFunctorActions.PROBLEM_PER_SUBJECT
import java.io._
  
class Checker(profileName: String, ruleName: Option[String]) {
  
  private val profile = {
    val model = ModelFactory.createMem
    IO.read( model, createInputStream(profileName), getNamespaceURI(profileName), "RDF/XML")
    model
  }

  private val rules = ruleName map createInputStream getOrElse 
    openStandardRules( "cimtool-split")
  
  private val validator = new SplitValidator(profile, rules)
    
  validator.setOption(PROBLEM_PER_SUBJECT, true)
    
  def check( modelName: String, dataName: String, output: OutputStream) = {
    val logger = new Logger(output)
    val namespace = getNamespaceURI(modelName)
    val writer = new SplitWriter(dataName, namespace)
    val splitter = new RDFParser(null, modelName, namespace, writer, logger.getSAXErrorHandler, false)
    splitter.run
    validator.run(dataName, null, namespace, logger)
    logger.flush
    logger.getErrorCount
  }
  
  private def getNamespaceURI(name: String) = (new File(name)).toURI.toString + "#"
  private def createInputStream(name: String) = new BufferedInputStream(new FileInputStream(name))

}
