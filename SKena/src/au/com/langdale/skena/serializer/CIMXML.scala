package au.com.langdale
package skena
package serializer

import com.hp.hpl.jena.graph.{Node, Graph}
import sax.AbstractReader
import java.io.OutputStream
import scala.collection.mutable.Set
import vocabulary._

object CIMXML {
  def serialize(graph: Graph, output: OutputStream) {
    val writer = new Writer(graph)
    writer.write(output)
  }
  
  class Writer(graph: Graph) extends AbstractReader {
    val base = graph.prefixes.get("")
    val seen = Set[Node]()
    
    def elem(tag: String)( build: => Unit ) {
      val e = new Element(tag)
      build
      e.close
    }
    
    def set(name: String, value: String) = getTop.set(name, value)
    def text(value: String) = getTop.append(value)
    
    def relative(node: Node) = {
      if( base == Some(node.getNameSpace))
        "#" + node.getLocalName
      else
        node.getURI
    }
    
    def objectProperty( p: Node, o: Node ) {
      for( tag <- graph.qname(p)) {
        elem(tag) {
          set("rdf:resource", relative(o))
        }
      }
    }
    
    def dataProperty( p: Node, o:Node) {
      for( tag <- graph.qname(p)) {
        elem(tag) {
          text(o.getLiteralLexicalForm)
        }
      }
    }
    
    def anonProperty( p: Node, o: Node ) {
      if( ! (seen contains o)) {
        for( tag <- graph.qname(p)) {
          seen += o
          elem(tag) {
            frame(o)
          }
        }
      }
    }
    
    def frame(subject: Node) {

      val types = RDF.a(subject)(graph)
      val nominal = types.headOption flatMap graph.qname
      val extras = if( nominal.isDefined ) types.tail else types

      elem( nominal getOrElse "rdf:Description" ) {
        
    	if(subject.isURI) {
          if( base == Some(subject.getNameSpace) ) 
            set("rdf:ID", subject.getLocalName )
          else
            set("rdf:about", subject.getURI)
    	}
    	
        for( extra <- extras ) {
          objectProperty(RDF.a.asNode, extra)
        }
        
        for( (p, o) <- graph.objects(subject) if p != RDF.a.asNode) {
          if( o.isURI )
            objectProperty( p, o )
          else if( o.isLiteral ) 
            dataProperty( p, o )
          else
        	anonProperty( p, o )
        }
      }
	
    }
    
    def emit {
      elem("rdf:RDF") {
        for((prefix, uri) <- graph.prefixes; sep = if(prefix == "") "" else ":" ) {
          set("xmlns" + sep + prefix, uri)
        }
          
        for(subject <- graph.subjects if subject.isURI) {
          frame(subject)
        }
      }
    }
  }
}
