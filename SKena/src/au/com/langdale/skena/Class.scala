package au.com.langdale.skena

import com.hp.hpl.jena.graph.{FrontsNode, Node, Graph, GraphAdd}
import vocabulary._
import scala.collection.Set

class Class( val label: String, val asNode: Node ) extends FrontsNode with Terminal[Node] {
	
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )

  def update( subj: Node, value: Boolean )( implicit graph: Graph ): Unit = 
    if(value) RDF.a( subj ) = asNode

  def update( subj: Option[Node], value: Boolean )( implicit graph: Graph ): Unit = 
      subj.foreach( update(_, value))

  def apply( subj: Node )( implicit graph: Graph ) = RDF.a( subj, asNode )
  
  def toSet( implicit graph: Graph ) = RDF.a.subjects(asNode)
}
