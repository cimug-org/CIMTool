package au.com.langdale.skena
package vocabulary

object RDF extends Vocabulary {
  val NS = Namespace("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
  object a extends ObjectProperty(NS("type"))
  object first extends FunctionalObjectProperty(NS("first"))
  object rest extends FunctionalObjectProperty(NS("rest"))
  object nil extends Individual(NS("nil"))
}
