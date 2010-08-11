package au.com.langdale.skena

import com.hp.hpl.jena.graph.{FrontsNode, Node, Graph, GraphAdd, Triple}
import com.hp.hpl.jena.datatypes.{TypeMapper, RDFDatatype}
import vocabulary._

class DataProperty[T]( val label: String, val asNode: Node, val dataType: RDFDatatype) extends Property[T] {

  def this( label: String, uri: String, xsdtype: String ) = 
          this( label, Node.createURI(uri), TypeMapper.getInstance.getTypeByName( XSD.NS + xsdtype ))

  def this( asNode: Node )(implicit m: Manifest[T]) = 
          this( asNode.getLocalName, asNode, TypeMapper.getInstance.getTypeByClass(m.erasure))
	
  def update( subj: Node, obj: T )( implicit graph: Graph ): Unit = 
      graph.add( new Triple( subj, asNode, valueToNode(obj)))

  def update( subj: Node, obj: Option[T] )( implicit graph: Graph ): Unit = 
      obj.foreach( update(subj, _))
   
  def valueToNode( obj: T ) = Node.createUncachedLiteral(obj, "", dataType)
  
  def nodeToValue( obj: Node ): Option[T] = {        
    if( obj.isLiteral) {
        val lit = obj.getLiteral
        if( lit.getDatatype == dataType ) 
          Some(lit.getValue.asInstanceOf[T])
        else
          None
    }
    else
      None
  }

  def apply(subj: Node, obj: T)( implicit graph: Graph): Boolean = graph.contains(subj, asNode, valueToNode(obj))

  def apply(subj: Node, obj: Node.ANY.type)( implicit graph: Graph): Boolean = graph.contains(subj, asNode, obj)
  
  def apply(subj: Node)( implicit graph: Graph) = graph.objects(subj, asNode) flatMap nodeToValue

  def pick(subj: Node)( implicit graph: Graph) = graph.pickObject(subj, asNode) flatMap nodeToValue

  def subjects(obj: T)( implicit graph: Graph) = graph.subjects(asNode, valueToNode(obj))

  def toSet( implicit graph: Graph) = graph.association(asNode) flatMap { case (s, o) => nodeToValue(o) map (v => (s, v)) }
}

class FunctionalDataProperty[T]( label: String, asNode: Node, dataType: RDFDatatype) extends DataProperty[T](label, asNode, dataType) {
  
  def this( label: String, uri: String, xsdtype: String ) = 
          this( label, Node.createURI(uri), TypeMapper.getInstance.getTypeByName( XSD.NS + xsdtype ))

  def this( asNode: Node )(implicit m: Manifest[T]) = 
          this( asNode.getLocalName, asNode, TypeMapper.getInstance.getTypeByClass(m.erasure))

}

class PlainDataProperty( label: String, asNode: Node ) extends DataProperty[String]( label, asNode, null) {
  def this( label: String, uri: String ) = this( label, Node.createURI(uri))  
  def this( asNode: Node ) = this( asNode.getLocalName, asNode )
}

