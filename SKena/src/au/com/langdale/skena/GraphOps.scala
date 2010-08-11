package au.com.langdale.skena

import com.hp.hpl.jena.graph.{Node, Graph, Triple}
import scala.collection.mutable.{Set, Iterable}
import scala.collection.{Map}
import scala.collection.JavaConversions._
import vocabulary.RDF

class GraphOps(graph: Graph) extends Set[(Node, Node, Node)] {
  
  private final def triples(s: Node, p: Node, o: Node): Iterator[Triple] = graph.find(s, p, o)
        
  def iterator = triples(?, ?, ?) map (t => (t.getSubject, t.getPredicate, t.getObject)) 
  def contains(t: (Node, Node, Node)): Boolean = graph.contains(t._1, t._2, t._3)
  def +=(t: (Node, Node, Node)) = { graph.add( Triple.create(t._1 , t._2, t._3)); this }
  def -=(t: (Node, Node, Node)) = { graph.delete( Triple.create(t._1 , t._2, t._3)); this }
  override def size = graph.size

  def prefixes: Map[String, String] = graph.getPrefixMapping.getNsPrefixMap
  
  def qname(node: Node) = 
    if(node.isURI) Option(graph.getPrefixMapping.qnameFor(node.getURI)) else None

  def objects(s: Node ) = triples(s, ?, ?) map (t => (t.getPredicate, t.getObject)) toSet
  def objects(s: Node, p: Node) = triples(s, p, ?) map (_.getObject) toSet
  def pickObject(s: Node, p: Node) = { val t = triples(s, p, ?); if( t.hasNext ) Some(t.next.getObject) else None }
  def subjects(o: Node) = triples(?, ?, o) map (t => (t.getSubject, t.getPredicate)) toSet
  def subjects(p: Node, o: Node) = triples(?, p, o) map (_.getSubject) toSet
  def subjects = triples(?, ?, ?) map (_.getSubject) toSet
  def association(p: Node) = triples(?, p, ?) map (t => (t.getSubject, t.getObject)) toSet
  
  def list(s: Node, p: Node): Option[List[Node]] = {
    implicit val g = graph
    
    def build( result: List[Node], head: Node): Option[List[Node]] =
      if( head == RDF.nil.asNode) 
        Some(Nil)
      else 
        for {
          f <- RDF.first.pick(head)
          r <- RDF.rest.pick(f)
          l <- build(f :: result, r)
        }
        yield l

    pickObject(s, p) flatMap { o => build(Nil, o) } map { _.reverse }
  }
  
  def updateList( s: Node, p: Node, o: List[Node]) {
	implicit val g = graph
	
    for {
      l <- list(s, p)
      e <- l
    }
    remove(e)
      
    def build(l: List[Node]): Node = l match {
      case f :: r => 
        val head = Node.createAnon
        RDF.first(head) = f
        RDF.rest(head) = build(r)
        head
        
      case Nil => RDF.nil.asNode
    }
      
    add((s, p, build(o)))  
  }
  
  def remove( node: Node ) {
    if( ! node.isLiteral)
      graph.getBulkUpdateHandler.remove(node, ?, ?)
    graph.getBulkUpdateHandler.remove(?, ?, node)
  }
}
