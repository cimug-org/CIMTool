package au.com.langdale.skena
import com.hp.hpl.jena.graph.{FrontsNode, Node, Graph, GraphAdd, Triple}

class ListProperty( val label: String, val asNode: Node ) extends Property[List[Node]] {
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )

  def update( subj: Node, obj: List[Node] )( implicit graph: Graph ): Unit = 
      graph.updateList( subj, asNode, obj )

  def update( subj: Node, obj: Option[List[Node]] )( implicit graph: Graph ): Unit = 
      obj.map( update(subj, _))

  def apply(subj: Node)( implicit graph: Graph) = Set.empty ++ graph.list(subj, asNode)
  
  def apply(subj: Node, obj: Option[List[Node]] )( implicit graph: Graph) = graph.list(subj, asNode) == obj

  def apply(subj: Node, obj: List[Node] )( implicit graph: Graph) = graph.list(subj, asNode) == Some(obj)

  def apply(subj: Node, obj: Node.ANY.type)( implicit graph: Graph): Boolean = graph.contains(subj, asNode, obj)

  def pick(subj: Node)( implicit graph: Graph) = graph.list(subj, asNode)
  
  def subjects(obj: List[Node])( implicit graph: Graph) = 
	  graph.association(asNode) collect { case (s, _) if apply(s, obj) => s }
  
  def toSet( implicit graph: Graph) = 
	  graph.association(asNode) flatMap { case (s, _) => graph.list(s, asNode) map (l => (s, l)) }

}
