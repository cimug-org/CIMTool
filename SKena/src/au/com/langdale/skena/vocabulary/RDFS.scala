package au.com.langdale.skena
package vocabulary

import com.hp.hpl.jena.vocabulary.{RDFS => jRDFS}

object RDFS extends Vocabulary {
  val NS = Namespace(jRDFS.getURI)
  
  object Datatype extends Class(jRDFS.Datatype)
  object comment extends PlainDataProperty(jRDFS.comment)
}