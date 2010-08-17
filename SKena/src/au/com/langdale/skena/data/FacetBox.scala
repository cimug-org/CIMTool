package au.com.langdale.skena
package data

import com.hp.hpl.jena.graph.{FrontsNode, Node, Node_URI, Node_Blank, Graph, GraphAdd, Triple}
import com.hp.hpl.jena.datatypes.TypeMapper
import scala.collection.mutable.{Buffer, Set}
import vocabulary.OWL._
import vocabulary.RDFS._
import vocabulary.XSD

class FacetBox {
  def define(facet: Node, value: String) {}
  def unDefine(facet: Node) {}
  def isDefined(facet: Node) = false
  def applicable(facet: Node) = false
  def value(facet: Node):Node = null
  def validate: String = null
  def commit {}
}

object FacetBox {
  def createOpt(graph: Graph, head: Node): Option[FacetBox] = {
    implicit val g = graph
    
    if( Datatype(head)) (
      for {
        xsdtype <- equivalentClass(head) find ( _.getNameSpace == XSD.NS.uri)
      }
      yield {
        Empty
      }
    ) orElse {

      for{ 
        parts <- intersectionOf(head).iterator
        part <- parts.iterator
        if Datatype(part)
        base <- onDatatype(part).iterator
        if base.getNameSpace == XSD.NS.uri
        facets <- withRestrictions(part).iterator
      } 
      yield Empty 

    
    
    }.take(1).toList.headOption
        
    
    else None
  }
  
  def create(graph: Graph, head: Node): FacetBox = createOpt(graph, head) getOrElse Empty
  
  def create(): FacetBox = Empty
  
  object Empty extends FacetBox
}
