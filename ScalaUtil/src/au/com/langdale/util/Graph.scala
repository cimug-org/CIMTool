package au.com.langdale
package util

import scala.collection.mutable.{HashMap, ListBuffer, Buffer}

object Graph {

  type Node = Long
  type Label = String
  
  trait Item {
    def s: Node
    def p: Label
  }
  
  trait Tag extends Item 

  trait Edge extends Item { 
    def o:Node
  }
  
  object Edge {
    def unapply(e:Edge) = Some(e.s, e.p, e.o)
  }
  
  object Tag {
    def unapply(t:Tag) = Some(t.s, t.p)
  }
  
  def opposite(n:Node, e: Edge) = if( e.s == n) e.o else if( e.o == n) e.s else throw new IllegalArgumentException
}

import Graph._


class Graph {
  val index = new HashMap[Node, List[Edge]] 
  val tagdex = new HashMap[Node, List[Tag]] 

  var size = 0
  def nodes = index.keySet
  
  def edges = for( (n, ls) <- index; e <- ls; if e.s == n ) yield e 
  def edgesFrom(n:Node) = for( e <- edges(n); if e.s == n) yield e
  def edgesTo(n:Node) = for( e <- edges(n); if e.o == n) yield e
  def edges(n:Node) = index.getOrElse(n, Nil)
  def edges(n:Node, m:Node): List[Edge] = for( e <- edges(n); if e.s == n && e.o == m || e.s == m && e.o == n) yield e
  def tags = for((_, ts) <- tagdex; t <-ts) yield t
  def tags(n:Node) = tagdex.getOrElse(n, Nil)
  def contains(n:Node) = index.contains(n) || tagdex.contains(n)
  def contains(e0: Edge) = edges(e0.s).exists(_ == e0)
  def contains(t0: Tag) = tags(t0.s).exists(_ == t0)
  
  def +=(e:Edge) {
    index(e.s) = e :: index.getOrElse(e.s, Nil)
    index(e.o) = e :: index.getOrElse(e.o, Nil)
    size += 1
  }

  def +=(t:Tag) {
    tagdex(t.s) = t :: tagdex.getOrElse(t.s, Nil)
    size += 1
  }
  
  def ++=(is: Iterable[Item]) { for( i <- is ) i match {
      case e: Edge => this += e
      case t: Tag => this += t
    }
  }
  
  def ++=(g: Graph) {
    for((n, es) <- g.index) 
      index(n) = es ::: index.getOrElse(n, Nil)
    for((n, ts) <- g.tagdex) 
      tagdex(n) = ts ::: tagdex.getOrElse(n, Nil)
    size += g.size
  }
}

class Coincident(g:Graph) extends Frequency[Label, Label] {
  override def title = "Coincident Edges"
  
  for( n <- g.nodes; val ls = g.edges(n); l1 <- ls; l2 <- ls; if ! (l1 eq l2)) add(l1.p, l2.p)
}

class Parallel(g:Graph) extends Frequency[Label, Label] {
  override def title = "Parallel Edges"
  
  for( n <- g.nodes; val es = g.edgesFrom(n); e1 <- es) {
    var none = true
    for( e2 <- es; if (! (e1 eq e2)) && e1.s == e2.s && e1.o == e2.o) {
      add(e1.p, e2.p)
      none = false
    }
    if(none) 
      add(e1.p, "unique")
  }
}
