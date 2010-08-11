package au.com.langdale.skena
package vocabulary

import com.hp.hpl.jena.vocabulary.OWL2

object OWL extends Vocabulary {
  val NS = Namespace(OWL2.NS)
  
  object withRestrictions extends ListProperty(OWL2.withRestrictions)
  object equivalentClass extends ObjectProperty(OWL2.equivalentClass)
  object intersectionOf extends ListProperty(OWL2.intersectionOf)
  object onDatatype extends FunctionalObjectProperty(OWL2.onDatatype)
}
