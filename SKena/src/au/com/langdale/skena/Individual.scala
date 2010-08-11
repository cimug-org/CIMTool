package au.com.langdale.skena

import com.hp.hpl.jena.graph.{FrontsNode, Node, Graph}

class OneOf( label: String, asNode: Node ) extends Class( label, asNode ) {
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )
}

class Individual( val label: String, val asNode: Node ) extends FrontsNode with Terminal[Node] {
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )
  def apply( cand: Node )(implicit graph: Graph) = asNode == cand
  def toSet(implicit graph: Graph) = Set(asNode)
}
