package au.com.langdale.skena

import com.hp.hpl.jena.graph.{FrontsNode, Node, Graph, GraphAdd}

trait Property[T] extends FrontsNode with Step[Node,T] {
	
  val label: String
  
  def update( subj: Node, obj: T )( implicit graph: Graph ): Unit
  
  def update( subj: Node, obj: Option[T] )( implicit graph: Graph ): Unit
}
