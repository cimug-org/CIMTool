package au.com.langdale

import com.hp.hpl.jena.graph.{Graph, Node, FrontsNode}

package object skena {
  implicit def toGraphOps(g: Graph) = new GraphOps(g)
  implicit def toNode(r: FrontsNode) = r.asNode
  val ? = Node.ANY
}
