package au.com.langdale
package util

import Graph._

class Islands(val g:Graph) {
  val grouper = new Grouper[Node]
  def nodes = grouper.nodes
  def size = grouper.size

  for(e <- g.edges) grouper.add(e.s, e.o)
  
  def reanalyse {
    grouper.clear
    for(e <- g.edges) grouper.add(e.s, e.o)
  }
}

class Summary(islands:Islands, threshhold: Int) extends Frequency[Int, Label] {
  override def title = "Islands"
  
  for( (i, ns) <- islands.nodes; if ns.size > threshhold) {
    add(i, "node", ns.size)
    for( n <- ns; e <- islands.g.edgesFrom(n) ) add(i, e.p)
  }
}
