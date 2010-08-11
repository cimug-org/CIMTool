package au.com.langdale.skena

import com.hp.hpl.jena.graph.{Node}

case class Namespace( uri: String ) extends (String => Node) {
  def apply( name: String) = Node.createURI( uri + name )
  def +( name: String ) = uri + name
}
