package au.com.langdale.skena

import com.hp.hpl.jena.graph.{FrontsNode, Node, Graph, GraphAdd, Triple}

class ObjectProperty( val label: String, val asNode: Node) extends Property[Node] {
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )

  def update( subj: Node, obj: Node )( implicit graph: Graph ): Unit = 
      graph.add( new Triple( subj, asNode, obj ))

  def update( subj: Node, obj: Option[Node] )( implicit graph: Graph ): Unit = 
      obj.foreach( update(subj, _))
  
  def apply(subj: Node, obj: Node)( implicit graph: Graph): Boolean = graph.contains(subj, asNode, obj)

  def apply(subj: Node)( implicit graph: Graph) = graph.objects(subj, asNode)

  def pick(subj: Node)( implicit graph: Graph) = graph.pickObject(subj, asNode)
  
  def subjects(obj: Node)( implicit graph: Graph) = graph.subjects(asNode, obj)
  
  def toSet( implicit graph: Graph) = graph.association(asNode)
}

class FunctionalObjectProperty( label: String, asNode: Node) extends ObjectProperty(label, asNode) {
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )
}
