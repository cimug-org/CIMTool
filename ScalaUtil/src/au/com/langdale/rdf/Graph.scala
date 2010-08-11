package au.com.langdale
package rdf

import scala.collection.mutable.{HashMap, ListBuffer, Set, Buffer}
import scala.collection.immutable.TreeSet

object Graph {
  
  case class Edge(s:Subject, p:Label, o:Node)
  case class Tag(s:Subject, t:Label)

  trait Node extends Proxy
  trait Subject extends Node
  trait Label extends Subject with Ordered[Label] {
    def self:String
    def compare( other:Label ) = self.compare( other.self)
    
    def apply( s:Subject ) = Tag(s, this)
    def apply( s:Subject, o:Node ) = Edge(s, this, o)
    def unapply( t:Tag ) = if( t.t == this ) Some( t.s ) else None
    def unapply( e:Edge ) = if( e.p == this ) Some((e.s, e.o)) else None
  }
  
  case class Value[T](val self:T) extends Node
  
  class Anon extends Subject { 
    lazy val self = Anon.nextLong 
  }
  object Anon extends scala.util.Random 
  
  abstract class URIRef extends Label  {
    def name: String
    def namespace: String
    def self: String = namespace + name
  }
    
  abstract class Vocabulary {
    val namespace: String
    
    abstract class Term extends URIRef {
      lazy val name = getClass.getSimpleName.split('$').last 
      def namespace = Vocabulary.this.namespace
    }
    
    def Term( name:String ) = new SpelledTerm(name)
    
    class SpelledTerm(val name:String) extends URIRef {
      def namespace = Vocabulary.this.namespace
    }
  }
  
  abstract class LocalVocabulary extends Vocabulary {
    lazy val namespace = getClass.getPackage.getName.split('.').reverse.mkString("http://", ".", 
                      getClass.getSimpleName.split('$').mkString("/", "/", "#"))
  } 
  
  def valid(n:Node) = n != null && n != "0"
}

import Graph._

class Graph {
  val index = new HashMap[Node, Buffer[Edge]] 
  val tags = new HashMap[Node, Buffer[Tag]] 

  var size = 0
  var joins = 0
  def nodes = index.keySet
  
  def edges = for( (n, ls) <- index; e <- ls; if e.s == n ) yield e 
  def edgesFrom(n:Node) = for( e <- index.getOrElse(n, Iterable.empty); if e.s == n) yield e
  def edgesTo(n:Node) = for( e <- index.getOrElse(n, Iterable.empty); if e.o == n) yield e
  def edges(n:Node) = for( e <- index.getOrElse(n, Iterable.empty) ) yield e
  
  private def add(e:Edge, s:Node, o:Node) {
    index.getOrElseUpdate(s, new ListBuffer[Edge]) += e
    index.getOrElseUpdate(o, new ListBuffer[Edge]) += e
    size += 1
  }
  
  def add(e:Edge) {
    val Edge(s, p, o) = e
    
    if( valid(s) && valid(o) && s != o ) e match { 
      case NetworkVocabulary.Merge(s, o) => 
	    if( index.contains(s) && index.contains(o) && 
           ! index(s).exists( l => l.s == s && l.o == o || l.o == s && l.s == o)) {
	        add(e, s, o)
	        joins += 1
        }
 
      case _ => add(e, s, o) 
    }
  }
  
  def add(t:Tag) {
    if( valid(t.s)) tags.getOrElseUpdate(t.s, new ListBuffer[Tag]) += t
  }
}

object NetworkVocabulary extends LocalVocabulary {
  object Conductor extends Term
  object Merge extends Term
}

object XSD extends Vocabulary {
  val namespace = "http://www.w3.org/2001/XMLSchema#"
  object string extends Term
  object float extends Term
}
