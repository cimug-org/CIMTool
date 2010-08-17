package au.com.langdale.skena

import com.hp.hpl.jena.graph.{Node, Graph}
import Step._

trait Step[S,T] { left =>
  
  def unary_~ : Step[T,S] = new InverseStep(left)

  def /[U]( right: Step[T,U]): Step[S, U] = new MappedStep(left, right)
  
  def &( right: Predicate[T]): Step[S,T] = new FilteredStep(left, right)
   
  def &( right: T => Boolean): Step[S,T] = new FilteredStep(left, new SimplePredicate(right))
   
  def apply(subj: S, obj: T)( implicit graph: Graph): Boolean

  def apply(subj: S)( implicit graph: Graph): Set[T]

  def pick(subj: S)( implicit graph: Graph): Option[T] 

  def subjects(obj: T)( implicit graph: Graph): Set[S]
  
  def toSet( implicit graph: Graph): Set[(S,T)]

}

trait Predicate[T] { left=>

  def unary_~ : Predicate[T] = new InversePredicate(left)	

  def /[U]( right: Step[T,U]): Terminal[U] = new MappedPredicate(left, right)

  def &( right: Terminal[T]): Terminal[T] = new Conjunction(right, left)

  def apply(subj: T)( implicit graph: Graph): Boolean
}

trait Terminal[T] extends Predicate[T] { left =>

  override def /[U]( right: Step[T,U]): Terminal[U] = new MappedTerminal(left, right)

  def &( right: Predicate[T]): Terminal[T] = new Conjunction(left, right)
   
  def &( right: T => Boolean): Terminal[T] = new Conjunction(left, new SimplePredicate(right))
  
  def |( right: Terminal[T]): Terminal[T] = new Disjunction(left, right)
  
  def toSet( implicit graph: Graph): Set[T]
}

case class All[T] extends Predicate[T] {
  def apply(subj: T)( implicit graph: Graph) = true
}

object Step {
  
  class SimplePredicate[T](pred: T => Boolean) extends Predicate[T] {
    def apply( subj: T)( implicit graph: Graph) = pred(subj)
  }
  
  class InversePredicate[T](left: Predicate[T]) extends Predicate[T] {
    
    override def unary_~ : Predicate[T] = left	
    
    def apply(subj: T)( implicit graph: Graph) = ! left(subj)
  }
  
  class InverseStep[S,T]( left: Step[T,S]) extends Step[S,T] {
  
    override def unary_~ = left
  
    def apply(subj: S, obj: T)( implicit graph: Graph) = left(obj, subj)
  
    def apply(subj: S)( implicit graph: Graph) = left.subjects(subj)
  
    def pick(subj: S)( implicit graph: Graph) = apply(subj).headOption
  
    def subjects(obj: T)( implicit graph: Graph) = left(obj)
    
    def toSet( implicit graph: Graph) = left.toSet map { case (s, o) => (o, s) }
  }
  
  class MappedPredicate[S,T]( left: Predicate[S], right: Step[S,T]) extends Terminal[T] {
  	
    def apply(subj: T)( implicit graph: Graph) = right.subjects(subj) exists { left(_) }
    
    def toSet( implicit graph: Graph): Set[T] = right.toSet collect { case (s, o) if left(s) => o }
  }
  
  class MappedTerminal[S,T]( left: Terminal[S], right: Step[S,T]) extends Terminal[T] {
  	
    def apply(subj: T)( implicit graph: Graph) = right.subjects(subj) exists { left(_) }
    
    def toSet( implicit graph: Graph): Set[T] = left.toSet flatMap { right(_) }
  }
  
  class MappedStep[S,T,U](left: Step[S,T], right: Step[T,U]) extends Step[S,U] {
  
    def apply(subj: S, obj: U)( implicit graph: Graph) = left(subj) exists { right(_, obj) }
  
    def apply(subj: S)( implicit graph: Graph) = left(subj) flatMap { right(_) }
  
    def pick(subj: S)( implicit graph: Graph) = apply(subj).headOption
  
    def subjects(obj: U)( implicit graph: Graph) = right subjects obj flatMap { left subjects _ }
  
    def toSet( implicit graph: Graph) = left.toSet flatMap { case (s, o) => right(o) map { (s, _) }}
  }
  
  class FilteredStep[S,T,U](left: Step[S,T], right: Predicate[T]) extends Step[S,T] {
  
    def apply(subj: S, obj: T)( implicit graph: Graph) = right(obj) && left(subj, obj)
  
    def apply(subj: S)( implicit graph: Graph) = left(subj) filter { right(_) }
  
    def pick(subj: S)( implicit graph: Graph) = apply(subj).headOption
  
    def subjects(obj: T)( implicit graph: Graph) = if(right(obj)) left subjects obj else Set.empty
  
    def toSet( implicit graph: Graph) = left.toSet filter { l => right(l._2)}
  }
  
  class Conjunction[T]( left: Terminal[T], right: Predicate[T])extends Terminal[T] {
  	
    def apply(subj: T)( implicit graph: Graph) = left(subj) && right(subj) 
    
    def toSet( implicit graph: Graph) = left.toSet filter { right(_) }
  }
  
  class Disjunction[T]( left: Terminal[T], right: Terminal[T])extends Terminal[T] {
  	
    def apply(subj: T)( implicit graph: Graph) = left(subj) || right(subj) 
    
    def toSet( implicit graph: Graph) = left.toSet | right.toSet
  }
}
